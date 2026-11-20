import numpy as np
import pandas as pd
from sklearn.metrics import average_precision_score, brier_score_loss, roc_auc_score

GATES = {
    "maxMAE": 2.0,
    "minAUC": 0.7,
    "maxECE": 0.05,
    "maxSliceMAE": 4.0,
    "minSliceRows": 30,
}


def metrics(rows, predictions):
    units, probabilities = predictions
    error = units - rows.leftover_units.to_numpy()
    y = rows.unsold_flag.to_numpy(dtype=int)
    bins = np.minimum((probabilities * 10).astype(int), 9)
    curve = []
    for index in np.unique(bins):
        mask = bins == index
        curve.append(
            {
                "bin": int(index),
                "rows": int(mask.sum()),
                "predicted": float(probabilities[mask].mean()),
                "observed": float(y[mask].mean()),
            }
        )
    two_classes = len(np.unique(y)) == 2
    return {
        "rows": len(rows),
        "MAE": float(np.abs(error).mean()),
        "RMSE": float(np.sqrt(np.square(error).mean())),
        "AUC": float(roc_auc_score(y, probabilities)) if two_classes else None,
        "PR_AUC": (
            float(average_precision_score(y, probabilities)) if two_classes else None
        ),
        "Brier": float(brier_score_loss(y, probabilities)),
        "ECE": sum(
            item["rows"] * abs(item["predicted"] - item["observed"]) for item in curve
        )
        / len(rows),
        "calibration": curve,
    }


def evaluate(rows, features, forecasts, promoted_version=None):
    comparison = {
        name: metrics(rows, predictions) for name, predictions in forecasts.items()
    }
    slices = {}
    dimensions = {
        "category": features.category,
        "pickupHour": features.pickupHour,
        "businessSize": features.businessSize.fillna(-1),
        "priceBand": pd.cut(
            features.price,
            [-np.inf, 5, 10, 20, np.inf],
            labels=["<=5", "5–10", "10–20", ">20"],
        ),
    }
    for dimension, values in dimensions.items():
        slices[dimension] = {}
        for value in values.unique():
            mask = values.eq(value).to_numpy()
            slices[dimension][str(value)] = metrics(
                rows.loc[mask], tuple(part[mask] for part in forecasts["candidate"])
            )
    candidate, baseline = comparison["candidate"], comparison["baseline"]
    auc = candidate["AUC"]
    regressions = []
    for name, comparator in comparison.items():
        if name == "candidate":
            continue
        for key in ["MAE", "RMSE", "Brier", "ECE", "AUC", "PR_AUC"]:
            value, reference = candidate[key], comparator[key]
            if value is not None and reference is not None:
                worse = (
                    value < reference if key in ["AUC", "PR_AUC"] else value > reference
                )
                if worse:
                    regressions.append(f"{key} worse than {name}")
    checks = {
        "mae": candidate["MAE"] <= GATES["maxMAE"],
        "auc": auc is not None and auc >= GATES["minAUC"],
        "calibration": candidate["ECE"] <= GATES["maxECE"],
        "slices": all(
            item["MAE"] <= GATES["maxSliceMAE"]
            for group in slices.values()
            for item in group.values()
            if item["rows"] >= GATES["minSliceRows"]
        ),
        "baseline": candidate["MAE"] < baseline["MAE"]
        and auc is not None
        and baseline["AUC"] is not None
        and auc > baseline["AUC"],
        "promoted": not any(
            message.endswith("than promoted")
            for message in regressions
            if message.startswith(("MAE ", "AUC "))
        ),
    }
    return {
        "comparison": comparison,
        "slices": slices,
        "gates": GATES,
        "checks": checks,
        "passed": all(checks.values()),
        "regressions": regressions,
        "promotedVersion": promoted_version,
        "syntheticShare": float(rows.source.eq("synthetic").mean()),
    }


def markdown(report):
    lines = [
        f"# Evaluation {report['version']}",
        "",
        f"Dataset SHA-256: `{report['datasetHash']}`",
        "",
        f"Test synthetic share: {report['syntheticShare']:.1%}.",
        f"Compared promoted version: {report['promotedVersion'] or 'none'}.",
        "",
        "| Model | MAE | RMSE | AUC | PR-AUC | Brier | ECE |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for name, result in report["comparison"].items():
        values = [
            result[key] for key in ("MAE", "RMSE", "AUC", "PR_AUC", "Brier", "ECE")
        ]
        lines.append(
            "| "
            + " | ".join(
                [name] + [f"{v:.4f}" if v is not None else "n/a" for v in values]
            )
            + " |"
        )
    lines += ["", "## Promotion checks", ""]
    lines += [
        f"- {name}: {'pass' if ok else 'fail'}" for name, ok in report["checks"].items()
    ]
    lines += ["", "## Regressions", ""]
    lines += [f"- {item}" for item in report["regressions"]] or ["None observed."]
    lines += [
        "",
        "## Candidate slices",
        "",
        "| Dimension | Slice | Rows | MAE | AUC |",
        "| --- | --- | --- | --- | --- |",
    ]
    for dimension, slices in report["slices"].items():
        for name, values in slices.items():
            auc = "n/a" if values["AUC"] is None else f"{values['AUC']:.4f}"
            lines.append(
                f"| {dimension} | {name} | {values['rows']} | {values['MAE']:.4f} | {auc} |"
            )
    lines += [
        "",
        "## Calibration curve",
        "",
        "| Probability bin | Rows | Mean predicted | Observed |",
        "| --- | --- | --- | --- |",
    ]
    for item in report["comparison"]["candidate"]["calibration"]:
        lines.append(
            f"| {item['bin']} | {item['rows']} | {item['predicted']:.4f} | {item['observed']:.4f} |"
        )
    return "\n".join(lines) + "\n"
