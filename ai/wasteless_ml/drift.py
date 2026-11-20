import argparse
import json
import os
from pathlib import Path

import numpy as np
import pandas as pd

from .features import FEATURES, feature_frame

THRESHOLDS = {"distribution": 0.20, "nullRateIncrease": 0.10, "minRows": 100}
NUMERIC = [
    "price",
    "quantityAvailable",
    "pickupMinutes",
    "pickupHour",
    "expectedLeftoverUnits",
    "unsoldProbability",
]


def distributions(frame, reference=None):
    result = {}
    for column in [*NUMERIC, "category"]:
        values = frame[column].dropna()
        if column == "category":
            frequencies = (
                values.astype(str).value_counts(normalize=True).sort_index().to_dict()
            )
            result[column] = {"frequencies": frequencies}
        else:
            cuts = (
                reference[column]["cuts"]
                if reference is not None
                else np.unique(values.quantile([0.2, 0.4, 0.6, 0.8])).tolist()
            )
            counts = np.bincount(
                np.searchsorted(cuts, values, side="right"), minlength=len(cuts) + 1
            )
            result[column] = {
                "cuts": cuts,
                "frequencies": (counts / max(1, len(values))).tolist(),
            }
    return result


def monitored_frame(rows, forecasts):
    return feature_frame(rows).assign(
        expectedLeftoverUnits=forecasts[0], unsoldProbability=forecasts[1]
    )


def profile(frame):
    return {
        "distributions": distributions(frame),
        "nullRates": frame[[*FEATURES, "expectedLeftoverUnits", "unsoldProbability"]]
        .isna()
        .mean()
        .to_dict(),
    }


def check(rows, frame, reference):
    if rows.empty:
        return {
            "rows": 0,
            "shifts": {},
            "nullRates": {},
            "thresholds": THRESHOLDS,
            "alerts": ["too few rows"],
        }
    current = distributions(frame, reference["distributions"])
    shifts = {}
    for name, result in current.items():
        before = reference["distributions"][name]["frequencies"]
        after = result["frequencies"]
        if isinstance(before, dict):
            distance = sum(
                abs(before.get(key, 0) - after.get(key, 0))
                for key in before.keys() | after.keys()
            )
        else:
            distance = float(np.abs(np.array(before) - np.array(after)).sum())
        shifts[name] = distance / 2
    alerts = [
        f"distribution shift: {key}"
        for key, value in shifts.items()
        if value >= THRESHOLDS["distribution"]
    ]
    null_rates = frame[list(reference["nullRates"])].isna().mean().to_dict()
    alerts += [
        f"null rate increased: {key}"
        for key, rate in null_rates.items()
        if rate - reference["nullRates"][key] >= THRESHOLDS["nullRateIncrease"]
    ]
    if len(rows) < THRESHOLDS["minRows"]:
        alerts.append("too few rows")
    if (
        "leftover_units" in rows
        and not rows.leftover_units.between(0, rows.quantityAvailable).all()
    ):
        alerts.append("leftover label outside expected range")
    return {
        "rows": len(rows),
        "shifts": shifts,
        "nullRates": null_rates,
        "thresholds": THRESHOLDS,
        "alerts": alerts,
    }


if __name__ == "__main__":
    from datetime import UTC, datetime

    from .dataset import from_database, snapshot_frame
    from .registry import load
    from .train import score

    parser = argparse.ArgumentParser()
    parser.add_argument("snapshot", type=Path, nargs="?")
    parser.add_argument("--database", action="store_true")
    parser.add_argument("--window-days", type=int)
    args = parser.parse_args()
    if args.database == bool(args.snapshot):
        parser.error("Provide a snapshot or --database")
    if args.database:
        extracted = from_database(os.environ["DB_URL_READONLY"])
        rows = (
            snapshot_frame(extracted, enforce_labels=False)
            if extracted
            else pd.DataFrame()
        )
    else:
        rows = pd.read_parquet(args.snapshot)
    if args.window_days and not rows.empty:
        rows = rows[
            rows.pickupEnd >= datetime.now(UTC) - pd.Timedelta(days=args.window_days)
        ]
    bundle, metadata = load()
    version = metadata["version"]
    observed = (
        monitored_frame(rows, score(bundle, rows)) if not rows.empty else pd.DataFrame()
    )
    report = check(rows, observed, metadata["driftReference"])
    report["modelVersion"] = version
    output = Path("var/drift") / version / f"{datetime.now(UTC):%Y%m%dT%H%M%S%f}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(output)
    for alert in report["alerts"]:
        print(alert)
    raise SystemExit(bool(report["alerts"]))
