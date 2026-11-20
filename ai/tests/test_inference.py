import json
import time
from pathlib import Path

import numpy as np
import pandas as pd
import pytest
from fastapi.testclient import TestClient

from wasteless_ml import baseline
from wasteless_ml.contract import Offer, Risk, bound_predictions, risk_level
from wasteless_ml.dataset import add_history, build
from wasteless_ml.drift import check, monitored_frame, profile
from wasteless_ml.features import FEATURES, feature_frame
from wasteless_ml.registry import Registry, load, promote, rollback
from wasteless_ml.serve import create_app, forecasts
from wasteless_ml.train import read_snapshot, score, train

FIXTURES = Path(__file__).parent / "fixtures"


@pytest.fixture(scope="session")
def trained(tmp_path_factory):
    root = tmp_path_factory.mktemp("ml")
    snapshot = build(root / "datasets", 6000)
    candidate = root / "candidate"
    train(snapshot, candidate, "0.1.0", False)
    return snapshot, candidate


@pytest.fixture
def promoted(trained, tmp_path, monkeypatch):
    monkeypatch.delenv("ARTIFACT_URI", raising=False)
    monkeypatch.delenv("MODEL_VERSION", raising=False)
    monkeypatch.setenv("MODEL_DIR", str(tmp_path / "registry"))
    promote(Registry(), trained[1])
    return load()


@pytest.fixture
def client(promoted):
    with TestClient(create_app()) as client:
        yield client


def sample():
    return json.loads((FIXTURES / "offer.json").read_text(encoding="utf-8"))


@pytest.mark.parametrize("count", [0, 1, 50])
def test_contract(client, count):
    offers = [dict(sample(), offerId=f"offer-{index}") for index in range(count)]
    response = client.post(
        "/predict", json={"offers": offers}, headers={"X-Request-ID": "f" * 32}
    )
    assert response.status_code == 200
    assert response.headers["X-Request-ID"] == "f" * 32
    predictions = response.json()["predictions"]
    assert len(predictions) == count
    for offer, forecast in zip(offers, predictions, strict=True):
        assert set(forecast) == {
            "offerId",
            "expectedLeftoverUnits",
            "unsoldProbability",
            "riskLevel",
            "modelVersion",
            "generatedAt",
        }
        assert offer["offerId"] == forecast["offerId"]
        assert forecast["modelVersion"] == "0.1.0"
        assert 0 <= forecast["expectedLeftoverUnits"] <= offer["quantityAvailable"]
        assert 0 <= forecast["unsoldProbability"] <= 1


def test_invalid_and_oversized(client):
    assert client.post("/predict", json={"offers": [sample()] * 51}).status_code == 422
    response = client.post(
        "/predict", json={"offers": [dict(sample(), quantityAvailable=-1)]}
    )
    assert response.status_code == 422
    assert response.json()["detail"][0]["loc"] == [
        "body",
        "offers",
        0,
        "quantityAvailable",
    ]
    assert "input" not in response.json()["detail"][0]
    assert client.post("/predict", content=b" " * (128 * 1024 + 1)).status_code == 413
    assert client.post("/predict", content=b"{").status_code == 422


def test_health_metadata_and_metrics(client):
    assert client.get("/health").json() == {
        "status": "ok",
        "ready": True,
        "modelVersion": "0.1.0",
    }
    assert client.get("/model-info").json()["features"] == FEATURES
    assert "wasteless_prediction_fallback_requests_total" in client.get("/metrics").text


def test_clipping_risk_unknown_category_and_expired_windows(client):
    zero = dict(sample(), quantityAvailable=0)
    large = dict(
        sample(),
        quantityAvailable=2**31 - 1,
        quantityTotal=2**31 - 1,
        category="unseen",
        scoredAt="2026-01-02T23:00:00Z",
    )
    result = client.post("/predict", json={"offers": [zero, large]}).json()[
        "predictions"
    ]
    assert result[0]["expectedLeftoverUnits"] == result[0]["unsoldProbability"] == 0
    assert result[0]["riskLevel"] == "LOW"
    assert result[1]["expectedLeftoverUnits"] == large["quantityAvailable"]
    assert result[1]["unsoldProbability"] == 1
    np.testing.assert_array_equal(
        bound_predictions([-1, 9], [-0.1, 1.1], [3, 4])[0], [0, 4]
    )
    assert risk_level(0, 0.6, 10) == Risk.HIGH
    assert risk_level(5, 0, 10) == Risk.HIGH
    assert risk_level(2.5, 0, 10) == Risk.MEDIUM
    assert risk_level(0, 0.3, 10) == Risk.MEDIUM
    assert risk_level(0, 0.2999, 10) == Risk.LOW


def test_train_serving_feature_parity(promoted, trained):
    bundle, _ = promoted
    _, rows = read_snapshot(trained[0])
    training_row = rows.iloc[[100]]
    raw = training_row.iloc[0].to_dict()
    offer = Offer.model_validate(
        {k: v for k, v in raw.items() if k in Offer.model_fields}
    )
    served_row = pd.DataFrame([offer.model_dump()])
    encoder = bundle["transformer"]
    np.testing.assert_allclose(
        encoder.transform(feature_frame(training_row)),
        encoder.transform(feature_frame(served_row)),
        rtol=0,
        atol=0,
    )
    candidate_units, candidate_probability = score(bundle, training_row)
    forecast = forecasts(bundle, [offer])[0]
    assert forecast.expectedLeftoverUnits == candidate_units[0]
    assert forecast.unsoldProbability == candidate_probability[0]


def test_rolling_history_uses_only_strictly_earlier_outcomes():
    frame = pd.DataFrame(
        {
            "source": ["real"] * 3,
            "businessKey": ["b"] * 3,
            "category": ["bakery"] * 3,
            "quantityTotal": [10] * 3,
            "leftover_units": [2, 4, 8],
            "pickupStart": pd.to_datetime(["2026-01-01T17:00:00Z"] * 3),
            "pickupEnd": pd.to_datetime(["2026-01-01T19:00:00Z"] * 3),
            "scoredAt": pd.to_datetime(
                ["2026-01-01T18:00:00Z", "2026-01-01T19:00:00Z", "2026-01-01T19:00:01Z"]
            ),
        }
    )
    add_history(frame)
    for column in ("businessSellThrough", "categoryHourSellThrough"):
        assert frame[column].iloc[:2].isna().all()
        assert frame[column].iloc[2] == pytest.approx((0.8 + 0.6 + 0.2) / 3)


def test_missing_or_incompatible_model(promoted, monkeypatch):
    metadata = Registry().path / "versions/0.1.0/metadata.json"
    original = metadata.read_bytes()
    changed = dict(json.loads(original), version="9.9.9")
    metadata.write_text(json.dumps(changed), encoding="utf-8")
    with pytest.raises(ValueError, match="dependency mismatch"):
        with TestClient(create_app()):
            pass
    metadata.write_bytes(original)
    monkeypatch.setenv("MODEL_DIR", str(metadata.parent / "nonexistent"))
    with pytest.raises(FileNotFoundError, match="No promoted model"):
        with TestClient(create_app()):
            pass


def test_timeout(client, monkeypatch):
    def slow(bundle, offers):
        time.sleep(0.65)
        return []

    monkeypatch.setattr("wasteless_ml.serve.forecasts", slow)
    assert client.post("/predict", json={"offers": [sample()]}).status_code == 504


def test_drift_detects_shift_and_fallback_works(promoted, trained):
    bundle, _ = promoted
    _, rows = read_snapshot(trained[0])
    rows = rows.iloc[:200].copy()
    normal = monitored_frame(rows, score(bundle, rows))
    shifted = rows.assign(price=1_000, category="new")
    result = check(
        shifted, monitored_frame(shifted, score(bundle, shifted)), profile(normal)
    )
    assert "distribution shift: price" in result["alerts"]
    assert "distribution shift: category" in result["alerts"]
    exported = json.loads((trained[1] / "fallback.json").read_bytes())
    features = feature_frame(shifted.assign(quantityAvailable=0))
    np.testing.assert_array_equal(
        baseline.predict(exported, features), np.zeros((2, len(features)))
    )


def test_rollback_and_duplicate_version(promoted, trained):
    registry = Registry()
    with pytest.raises(ValueError, match="currently promoted"):
        promote(registry, trained[1])
    with pytest.raises(FileExistsError):
        registry.store("0.1.0", trained[1])
    with pytest.raises(ValueError, match="No previous"):
        rollback(registry)
    registry.set_pointer("0.2.0", "0.1.0")
    rollback(registry)
    assert load()[1]["version"] == "0.1.0"
