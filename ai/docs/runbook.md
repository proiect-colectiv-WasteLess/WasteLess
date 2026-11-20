# Training, promotion and rollback

Run commands from ai/. Install uv 0.13.9 and Python 3.12.16. The project lock pins the dependencies for training and inference. The Makefile exposes lint, test, synthetic training and serve. To install the pre-commit hook from the repository root, run uv run --project ai --locked pre-commit install -c ai/.pre-commit-config.yaml.

```sh
uv sync --locked --python 3.12.16
make lint test train
```

For a real run, set DB_URL_READONLY to the provisioned ml_reader connection and run:

```sh
uv run --locked python -m wasteless_ml.dataset --database --synthetic 0
uv run --locked python -m wasteless_ml.train --version 0.2.0 --snapshot var/datasets/HASH.parquet --compare-promoted
uv run --locked python -m wasteless_ml.report var/candidates/0.2.0 var/datasets/HASH.parquet
```

Skip --compare-promoted for the first candidate. The dataset command reads only ml.v_offer_outcomes in a read-only transaction. It expects offer_id, business_id, category, price, original_price, quantity_total, created_at, pickup_start, pickup_end and leftover_units. WAS-90 owns the view and credentials. The dataset builds a hash-named Parquet and JSON dataset card. Store both files under the same hash in private artifact storage for reproduction. Never publish datasets, credentials, or model binaries in Git. S3 or MinIO can receive these snapshots with --upload-uri s3://bucket/prefix.

The synthetic generator covers seeded variation in discount, category, stock, pickup duration, weekday/hour and business size. Labels follow stock at end of pickup, and source is preserved. Use synthetic results as a cold-start smoke check. Review real test results before first production promotion.

## Review and promote

- Use candidate evaluation.md for MAE/RMSE, AUC/PR-AUC, Brier, calibration and category, hour, size and price slices. Defaults require MAE ≤ 2, AUC ≥ 0.70 and ECE ≤ 0.05. Slices of at least 30 offers require MAE ≤ 4. Both headline metrics must beat the heuristic, and neither can regress versus the currently promoted model. The heuristic must beat zero and stock on validation MAE. Confirm the thresholds with Backend and Platform before production use.
- The report command regenerates metrics from the same snapshot and candidate. Evaluation against a champion pins the original champion version. The bundle includes the encoder, two models, probability calibrator, versioned fallback, metadata, report and model card.
- Set MODEL_DIR for the local registry or ARTIFACT_URI=s3://bucket/prefix for S3/MinIO. S3_ENDPOINT_URL can point to MinIO. Supply credentials through the runtime's standard AWS identity chain. Only load joblib bundles from the trusted model registry.

```sh
uv run --locked python -m wasteless_ml.registry promote var/candidates/0.2.0
uv run --locked python -m wasteless_ml.serve
uv run --locked python -m wasteless_ml.registry rollback
```

Each semantic version is immutable once registered. Promotion updates the registry pointer after verifying the evaluation and artifact. Rollback swaps it with the previous version. Restart the service to load the chosen artifact without rebuilding the image. If MODEL_VERSION is set, update it before restarting. Check /health and /model-info after restart.

The Java fallback uses fallback.json directly. Look up an exact (category, UTC pickup hour) pair in groups, or use default. Multiply current stock by leftoverShare and use unsold as the probability. Apply the same clipping, zero-stock rule and riskThresholds. The backend sets degraded=true when it falls back and owns counting complete network failures. The service exports only the in-process fallback-needed count and never logs offer identifiers in plaintext.

## Container and load smoke

```sh
docker build -t wasteless-ai:git-SHA .
docker run --rm -p 8000:8000 -v "$PWD/var/registry:/models:ro" wasteless-ai:git-SHA
k6 run tests/load.js
```

The CI image is tagged git-<full commit SHA>. Image size budget is 600 MiB. Model version is independent of the image tag. The container runs as UID 10001 and only needs the artifact mount or registry access. For reduced logging set LOG_LEVEL=WARNING. See [load-report.md](load-report.md) for the recorded run.

## Drift and retraining

Use the daily drift workflow after WAS-90 provides DB_URL_READONLY and the registry is configured. It scores the 14 most recent days of completed offers with the promoted model, writes aggregate reports under var/drift/<version>, and fails the check when alerts fire. GitHub Actions runs scheduled workflows from the default branch. Set AI_DRIFT_ENABLED=true, ARTIFACT_URI, AWS_DEFAULT_REGION and optional S3_ENDPOINT_URL as repository variables. Set DB_URL_READONLY as a secret and configure a runner role or the AI_AWS_ACCESS_KEY_ID and AI_AWS_SECRET_ACCESS_KEY secrets. A failed workflow can notify its owners through GitHub Actions. For downstream fallback cases that the inference service cannot observe, alert on WAS-87's backend metrics.

Drift thresholds are total variation distance ≥ 0.20, null-rate increase ≥ 10 percentage points, under 100 observations per window, or a leftover label outside [0, available stock]. Output distribution is based on recent offers replayed through the current model. Review drift, feature integrity and matured forecast quality before retraining. Re-evaluate after collecting at least 1,000 additional real outcomes, after a confirmed drift alert, or when observed MAE exceeds 2 on a representative recent sample. Promote only after the gate checks pass.
