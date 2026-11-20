import argparse
import hashlib
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from urllib.parse import urlparse

import boto3
import numpy as np
import pandas as pd
import psycopg
from botocore.exceptions import ClientError
from psycopg.rows import dict_row

from .contract import Offer
from .features import LABELS

SEED = 42
CATEGORIES = ["bakery", "produce", "meals", "dairy"]


def synthetic(count):
    rng = np.random.default_rng(SEED)
    rows = []
    for index in range(count):
        category = rng.choice(CATEGORIES)
        business = int(rng.integers(30))
        hour = int(rng.integers(10, 22))
        start = pd.Timestamp("2024-01-01", tz="UTC") + pd.Timedelta(
            days=index // 12, hours=hour
        )
        created = start - pd.Timedelta(hours=int(rng.integers(2, 36)))
        end = start + pd.Timedelta(minutes=int(rng.choice([45, 90, 120, 180])))
        size = 1 + business % 3
        original = rng.uniform(6, 30)
        discount = rng.uniform(0.15, 0.75)
        stock = int(rng.integers(4, 8 + 3 * size))
        demand = (
            2
            + 11 * discount
            + 4 * (17 <= hour <= 19)
            + 2 * (start.dayofweek >= 5)
            + 2 * (category == "bakery")
            + size
            - original / 7
            + (end - start).total_seconds() / 7200
        )
        leftover = max(0, stock - rng.poisson(max(0.1, demand)))
        rows.append(
            {
                "offerId": f"synthetic-{index}",
                "businessKey": f"synthetic-{business}",
                "price": round(original * (1 - discount), 2),
                "originalPrice": round(original, 2),
                "category": category,
                "quantityTotal": stock,
                "quantityAvailable": stock,
                "pickupStart": start,
                "pickupEnd": end,
                "createdAt": created,
                "scoredAt": created,
                "businessSize": size,
                "leftover_units": int(leftover),
                "source": "synthetic",
            }
        )
    return rows


def from_database(url):
    with psycopg.connect(url, row_factory=dict_row) as connection:
        connection.read_only = True
        with connection.cursor() as cursor:
            cursor.execute("""
                SELECT offer_id::text AS "offerId", business_id::text AS "businessKey",
                       category, price, original_price AS "originalPrice",
                       quantity_total AS "quantityTotal", quantity_total AS "quantityAvailable",
                       created_at AS "createdAt", created_at AS "scoredAt",
                       pickup_start AS "pickupStart", pickup_end AS "pickupEnd",
                       leftover_units
                FROM ml.v_offer_outcomes
                WHERE pickup_end < CURRENT_TIMESTAMP AND leftover_units IS NOT NULL
            """)
            return [dict(row, source="real") for row in cursor]


def add_history(frame):
    rates = frame.assign(
        rate=1 - frame.leftover_units / frame.quantityTotal.replace(0, np.nan),
        pickupHour=frame.pickupStart.dt.hour,
    )
    for keys, target in [
        (["source", "businessKey"], "businessSellThrough"),
        (["source", "category", "pickupHour"], "categoryHourSellThrough"),
    ]:
        frame[target] = np.nan
        for _, group in rates.groupby(keys):
            history = (
                group.dropna(subset=["rate"])
                .groupby("pickupEnd")
                .rate.agg(["sum", "count"])
            )
            cumulative = history.cumsum()
            at = cumulative.index.searchsorted(group.scoredAt, side="left")
            since = cumulative.index.searchsorted(
                group.scoredAt - pd.Timedelta(days=90), side="left"
            )
            totals = np.vstack([np.zeros(2), cumulative.to_numpy()])
            window = totals[at] - totals[since]
            frame.loc[group.index, target] = np.divide(
                window[:, 0],
                window[:, 1],
                out=np.full(len(group), np.nan),
                where=window[:, 1] > 0,
            )


def sha256(path):
    with path.open("rb") as handle:
        return hashlib.file_digest(handle, "sha256").hexdigest()


def snapshot_frame(rows, *, enforce_labels=True):
    if not rows:
        raise ValueError("No offers available for dataset")
    for row in rows:
        offer = Offer.model_validate(
            {k: v for k, v in row.items() if k in Offer.model_fields}
        )
        row.update(offer.model_dump())
    frame = pd.DataFrame(rows)
    for column in ("createdAt", "scoredAt", "pickupStart", "pickupEnd"):
        frame[column] = pd.to_datetime(frame[column], utc=True)
    frame = frame.sort_values(["scoredAt", "source", "offerId"]).reset_index(drop=True)
    if frame.duplicated(["source", "offerId"]).any():
        raise ValueError("Duplicate offer in one-row-per-offer dataset")
    valid_labels = frame.leftover_units.between(0, frame.quantityAvailable)
    if enforce_labels and not valid_labels.all():
        raise ValueError("Leftover labels outside stock range")
    frame[LABELS[1]] = frame.leftover_units.gt(0)
    frame[LABELS[2]] = frame.leftover_units.eq(0)
    frame[LABELS[3]] = (
        1 - frame.leftover_units / frame.quantityTotal.replace(0, np.nan)
    ).fillna(0)
    if not enforce_labels:
        # Invalid labels remain available for alerts, but never enter features.
        frame.loc[~valid_labels, "leftover_units"] = np.nan
    add_history(frame)
    return frame.drop(columns=["businessKey"])


def build(output, count, db_url=None):
    frame = snapshot_frame(synthetic(count) + (from_database(db_url) if db_url else []))
    cut1, cut2 = frame.scoredAt.quantile([0.60, 0.80])
    frame["split"] = np.select(
        [
            frame.pickupEnd < cut1,
            (frame.scoredAt >= cut1) & (frame.pickupEnd < cut2),
            frame.scoredAt >= cut2,
        ],
        ["train", "validation", "test"],
        default="gap",
    )
    frame = frame[frame.split.ne("gap")]
    output.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=output) as staging:
        temporary = Path(staging) / "snapshot.parquet"
        frame.to_parquet(temporary, index=False)
        digest = sha256(temporary)
        snapshot = output / f"{digest}.parquet"
        temporary.replace(snapshot)
    card = {
        "sha256": digest,
        "syntheticSeed": SEED,
        "splits": {
            name: {
                "rows": len(group),
                "from": group.scoredAt.min().isoformat(),
                "to": group.scoredAt.max().isoformat(),
                "unsoldShare": float(group.unsold_flag.mean()),
                "syntheticShare": float(group.source.eq("synthetic").mean()),
            }
            for name, group in frame.groupby("split")
        },
    }
    snapshot.with_suffix(".json").write_text(
        json.dumps(card, indent=2), encoding="utf-8"
    )
    print(snapshot)
    return snapshot


def upload_snapshot(snapshot, uri):
    address = urlparse(uri)
    if address.scheme != "s3" or not address.netloc:
        raise ValueError("Snapshot storage must be s3://bucket[/prefix]")
    client = boto3.client("s3", endpoint_url=os.getenv("S3_ENDPOINT_URL") or None)
    for file in (snapshot, snapshot.with_suffix(".json")):
        key = "/".join(filter(None, [address.path.strip("/"), "datasets", file.name]))
        try:
            with file.open("rb") as body:
                client.put_object(
                    Bucket=address.netloc, Key=key, Body=body, IfNoneMatch="*"
                )
        except ClientError as exc:
            if exc.response["ResponseMetadata"]["HTTPStatusCode"] != 412:
                raise


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("var/datasets"))
    parser.add_argument("--synthetic", type=int, default=6000)
    parser.add_argument("--database", action="store_true")
    parser.add_argument("--upload-uri")
    args = parser.parse_args()
    snapshot = build(
        args.output,
        args.synthetic,
        os.environ["DB_URL_READONLY"] if args.database else None,
    )
    if args.upload_uri:
        upload_snapshot(snapshot, args.upload_uri)
