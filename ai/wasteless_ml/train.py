import argparse
import json
import platform
import re
from datetime import UTC, datetime
from importlib.metadata import version
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from lightgbm import LGBMClassifier, LGBMRegressor
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import mean_absolute_error, roc_auc_score

from . import baseline
from .contract import RISK_THRESHOLDS, bound_predictions
from .dataset import sha256
from .drift import monitored_frame, profile
from .evaluation import evaluate, markdown, metrics
from .features import FEATURES, feature_frame, transformer

LIBRARIES = ["lightgbm", "numpy", "pandas", "scikit-learn", "joblib"]
VERSION_PATTERN = r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"


def validate_version(model_version):
    if not isinstance(model_version, str) or not re.fullmatch(
        VERSION_PATTERN, model_version
    ):
        raise ValueError("Model version must be MAJOR.MINOR.PATCH")
    return model_version


def runtime():
    return {
        "python": platform.python_version(),
        **{name: version(name) for name in LIBRARIES},
    }


def score(bundle, rows):
    features = feature_frame(rows)
    matrix = bundle["transformer"].transform(features)
    raw = bundle["classifier"].predict(matrix, raw_score=True).reshape(-1, 1)
    return bound_predictions(
        bundle["regressor"].predict(matrix),
        bundle["calibrator"].predict_proba(raw)[:, 1],
        rows.quantityAvailable.to_numpy(),
    )


def read_snapshot(path):
    digest = sha256(path)
    if path.stem != digest:
        raise ValueError("Snapshot filename does not match its SHA-256")
    rows = pd.read_parquet(path)
    return digest, rows


def report_for(bundle, snapshot, compare_promoted=False, champion_version=None):
    rows = snapshot[snapshot.split.eq("test")]
    validation = snapshot[snapshot.split.eq("validation")]
    features = feature_frame(rows)
    forecasts = {
        "candidate": score(bundle, rows),
        "baseline": baseline.predict(bundle["baseline"], features),
    }
    promoted_version = None
    if compare_promoted:
        from .registry import load

        champion, metadata = load(champion_version)
        promoted_version = metadata["version"]
        forecasts["promoted"] = score(champion, rows)
    report = evaluate(rows, features, forecasts, promoted_version)
    val_stock = validation.quantityAvailable.to_numpy()
    baseline_val = metrics(
        validation, baseline.predict(bundle["baseline"], feature_frame(validation))
    )
    naive_val = {
        "zero": metrics(
            validation, (np.zeros(len(validation)), np.zeros(len(validation)))
        ),
        "stock": metrics(validation, (val_stock, (val_stock > 0).astype(float))),
    }
    report["validationBaseline"] = {"baseline": baseline_val, "naive": naive_val}
    report["checks"]["heuristic"] = all(
        baseline_val["MAE"] < naive["MAE"] for naive in naive_val.values()
    )
    report["passed"] = all(report["checks"].values())
    return dict(report, datasetHash=bundle["datasetHash"], version=bundle["version"])


def train(snapshot, output, model_version, compare_promoted):
    validate_version(model_version)
    digest, rows = read_snapshot(snapshot)
    training, validation, test = (
        rows[rows.split.eq(split)].copy() for split in ("train", "validation", "test")
    )
    if min(map(len, (training, validation, test))) == 0:
        raise ValueError("Train, validation and test splits must each contain offers")
    if training.unsold_flag.nunique() < 2 or validation.unsold_flag.nunique() < 2:
        raise ValueError(
            "Train and validation must both contain positive and negative labels"
        )
    features = feature_frame(training)
    validation_features = feature_frame(validation)
    encoder = transformer()
    x_train = encoder.fit_transform(features)
    x_val = encoder.transform(validation_features)
    sample_age = (
        training.scoredAt.max() - training.scoredAt
    ).dt.total_seconds() / 86400
    recency = np.maximum(0.05, np.exp2(-sample_age / 180))
    best_regressor, best_classifier = (None, float("inf")), (None, -float("inf"))
    search = []
    for leaves in (7, 15, 31):
        for estimators in (100, 250):
            options = dict(
                num_leaves=leaves,
                n_estimators=estimators,
                learning_rate=0.04,
                verbosity=-1,
                n_jobs=1,
                random_state=42,
                deterministic=True,
                force_col_wise=True,
            )
            reg = LGBMRegressor(**options).fit(
                x_train, training.leftover_units, sample_weight=recency
            )
            clf = LGBMClassifier(**options, class_weight="balanced").fit(
                x_train, training.unsold_flag, sample_weight=recency
            )
            units, probability = bound_predictions(
                reg.predict(x_val),
                clf.predict_proba(x_val)[:, 1],
                validation.quantityAvailable,
            )
            mae = mean_absolute_error(validation.leftover_units, units)
            auc = roc_auc_score(validation.unsold_flag, probability)
            search.append(
                {"numLeaves": leaves, "estimators": estimators, "MAE": mae, "AUC": auc}
            )
            if mae < best_regressor[1]:
                best_regressor = reg, mae
            if auc > best_classifier[1]:
                best_classifier = clf, auc
    reg, clf = best_regressor[0], best_classifier[0]
    calibrator = LogisticRegression(random_state=42).fit(
        clf.predict(x_val, raw_score=True).reshape(-1, 1), validation.unsold_flag
    )
    table = dict(baseline.fit(training, features), version=model_version)
    bundle = dict(
        transformer=encoder,
        regressor=reg,
        classifier=clf,
        calibrator=calibrator,
        baseline=table,
        datasetHash=digest,
        version=model_version,
    )
    report = report_for(bundle, rows, compare_promoted)
    feature_names = encoder.get_feature_names_out().tolist()
    importance = {
        "leftover": dict(
            zip(feature_names, reg.feature_importances_.tolist(), strict=True)
        ),
        "unsold": dict(
            zip(feature_names, clf.feature_importances_.tolist(), strict=True)
        ),
    }
    output.mkdir(parents=True, exist_ok=True)
    joblib.dump(bundle, output / "model.joblib")
    metadata = dict(
        version=model_version,
        trainedAt=datetime.now(UTC).isoformat(),
        datasetHash=digest,
        artifactHash=sha256(output / "model.joblib"),
        runtime=runtime(),
        features=FEATURES,
        riskThresholds=RISK_THRESHOLDS,
        metrics=report["comparison"]["candidate"],
        driftReference=profile(monitored_frame(training, score(bundle, training))),
    )
    for name, data in [
        ("metadata", metadata),
        ("evaluation", report),
        ("fallback", table),
        (
            "experiment",
            {
                "search": search,
                "featureImportance": importance,
                "datasetHash": digest,
                "features": FEATURES,
                "regressor": reg.get_params(),
                "classifier": clf.get_params(),
            },
        ),
    ]:
        (output / f"{name}.json").write_text(
            json.dumps(data, indent=2, allow_nan=False), encoding="utf-8"
        )
    (output / "evaluation.md").write_text(markdown(report), encoding="utf-8")
    card = Path(__file__).resolve().parents[1] / "docs/model-card.md"
    (output / "model-card.md").write_text(
        card.read_text(encoding="utf-8") + "\n" + markdown(report), encoding="utf-8"
    )
    print(f"{output} (promotion gates passed: {report['passed']})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--snapshot", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--compare-promoted", action="store_true")
    args = parser.parse_args()
    snapshot = args.snapshot or max(
        Path("var/datasets").glob("*.parquet"), key=lambda path: path.stat().st_mtime_ns
    )
    train(
        snapshot,
        args.output or Path("var/candidates") / validate_version(args.version),
        args.version,
        args.compare_promoted,
    )
