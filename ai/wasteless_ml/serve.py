import asyncio
import hashlib
import json
import logging
import os
import re
import time
from concurrent.futures import ThreadPoolExecutor
from contextlib import asynccontextmanager
from datetime import UTC, datetime
from threading import BoundedSemaphore
from uuid import uuid4

import pandas as pd
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest
from starlette.requests import ClientDisconnect

from .contract import (
    BUDGET_SECONDS,
    MAX_BODY_BYTES,
    Batch,
    Forecast,
    Offer,
    Predictions,
    risk_level,
)
from .registry import load
from .train import score

LOG = logging.getLogger("wasteless.predictions")
REQUESTS = Counter("wasteless_prediction_requests", "Inference requests", ["outcome"])
LATENCY = Histogram("wasteless_prediction_latency_seconds", "Inference request latency")
FALLBACKS = Counter(
    "wasteless_prediction_fallback_requests",
    "Requests requiring caller-side fallback due to inference failures",
)


def forecasts(bundle, offers):
    if not offers:
        return []
    rows = pd.DataFrame([offer.model_dump() for offer in offers])
    leftovers, probabilities = score(bundle, rows)
    generated = datetime.now(UTC)
    for index, offer in enumerate(offers):
        if offer.scoredAt >= offer.pickupEnd:
            leftovers[index] = offer.quantityAvailable
            probabilities[index] = float(offer.quantityAvailable > 0)
    return [
        Forecast(
            offerId=offer.offerId,
            expectedLeftoverUnits=units,
            unsoldProbability=probability,
            riskLevel=risk_level(units, probability, offer.quantityAvailable),
            modelVersion=bundle["version"],
            generatedAt=generated,
        )
        for offer, units, probability in zip(
            offers, leftovers, probabilities, strict=True
        )
    ]


def create_app(loader=load):
    @asynccontextmanager
    async def lifespan(app):
        app.state.bundle, app.state.metadata = loader()
        app.state.slots = BoundedSemaphore(4)
        with ThreadPoolExecutor(max_workers=4) as executor:
            app.state.executor = executor
            yield

    app = FastAPI(
        title="WasteLess Unsold Offer Prediction", version="1.0.0", lifespan=lifespan
    )

    @app.exception_handler(RequestValidationError)
    async def validation_error(request, exc):
        # Errors omit raw input, arbitrary property names and validator context.
        detail = []
        for error in exc.errors():
            location = [
                (
                    part
                    if isinstance(part, int)
                    or part in Batch.model_fields
                    or part in ("body", *Offer.model_fields)
                    else "field"
                )
                for part in error["loc"]
            ]
            detail.append({"loc": location, "msg": error["msg"], "type": error["type"]})
        return JSONResponse(status_code=422, content={"detail": detail})

    @app.middleware("http")
    async def request_limits(request: Request, call_next):
        if request.method != "POST" or request.url.path != "/predict":
            return await call_next(request)
        started = time.perf_counter()
        request_id = request.headers.get("x-request-id", "")
        request_id = (
            request_id
            if re.fullmatch(r"[0-9a-fA-F-]{16,64}", request_id)
            else uuid4().hex
        )
        request.state.request_id = request_id
        request.state.started = started
        try:
            async with asyncio.timeout(BUDGET_SECONDS):
                body = bytearray()
                async for chunk in request.stream():
                    if len(body) + len(chunk) > MAX_BODY_BYTES:
                        response = JSONResponse(
                            {"detail": "Request body exceeds 128 KiB"}, status_code=413
                        )
                        break
                    body.extend(chunk)
                else:
                    request._body = bytes(body)
                    response = await call_next(request)
        except TimeoutError:
            response = JSONResponse(
                {"detail": "Prediction deadline exceeded"}, status_code=504
            )
        except ClientDisconnect:
            response = JSONResponse({"detail": "Client disconnected"}, status_code=400)
        latency = time.perf_counter() - started
        degraded = response.status_code >= 500
        LATENCY.observe(latency)
        REQUESTS.labels(str(response.status_code)).inc()
        if degraded:
            FALLBACKS.inc()
        LOG.info(
            json.dumps(
                {
                    "event": "prediction_request",
                    "requestId": request_id,
                    "latencyMs": round(latency * 1000, 2),
                    "status": response.status_code,
                    "degraded": degraded,
                    "modelVersion": app.state.metadata["version"],
                }
            )
        )
        response.headers["X-Request-ID"] = request_id
        return response

    @app.post("/predict", response_model=Predictions)
    async def predict(batch: Batch, request: Request):
        if not app.state.slots.acquire(blocking=False):
            raise HTTPException(503, "Prediction workers busy")
        try:
            future = app.state.executor.submit(
                forecasts, app.state.bundle, batch.offers
            )
        except RuntimeError:
            app.state.slots.release()
            raise HTTPException(503, "Prediction workers unavailable") from None
        future.add_done_callback(lambda _: app.state.slots.release())
        try:
            results = await asyncio.wrap_future(future)
        except Exception as exc:
            LOG.error(
                json.dumps(
                    {
                        "event": "prediction_failed",
                        "errorType": type(exc).__name__,
                        "requestId": request.state.request_id,
                    }
                )
            )
            raise HTTPException(503, "Prediction failed") from None
        latency = round((time.perf_counter() - request.state.started) * 1000, 2)
        for result in results:
            LOG.info(
                json.dumps(
                    {
                        "event": "prediction",
                        "requestId": request.state.request_id,
                        "offerHash": hashlib.sha256(
                            result.offerId.encode()
                        ).hexdigest(),
                        "modelVersion": result.modelVersion,
                        "expectedLeftoverUnits": result.expectedLeftoverUnits,
                        "unsoldProbability": result.unsoldProbability,
                        "latencyMs": latency,
                        "degraded": False,
                    }
                )
            )
        return Predictions(predictions=results)

    @app.get("/health")
    async def health():
        return {
            "status": "ok",
            "ready": True,
            "modelVersion": app.state.metadata["version"],
        }

    @app.get("/model-info")
    async def model_info():
        return {
            key: app.state.metadata[key]
            for key in ("version", "trainedAt", "metrics", "features")
        }

    @app.get("/metrics", include_in_schema=False)
    async def metrics():
        return Response(
            content=generate_latest(), headers={"content-type": CONTENT_TYPE_LATEST}
        )

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    logging.basicConfig(
        level=os.getenv("LOG_LEVEL", "INFO").upper(), format="%(message)s"
    )
    uvicorn.run(
        app, host="0.0.0.0", port=int(os.getenv("PORT", "8000")), access_log=False
    )
