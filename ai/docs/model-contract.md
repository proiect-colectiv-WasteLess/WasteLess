# Unsold offer scoring contract

WAS-74 and WAS-82 define the scoring API for WAS-87 and WAS-88. The risk thresholds and API are proposed for approval by Backend and Platform. The machine-readable contract is [openapi.json](openapi.json).

## Target and data cutoff

Predict remaining sellable units at pickup end and the probability that the count is positive. The canonical training grain is one snapshot per offer at publication. At that time available quantity equals initial quantity. The API also accepts later snapshots. Predictions at later times are clipped to then-current stock, but their accuracy needs separate validation with production snapshots. Zero stock yields zero units and zero probability. At or after pickup end, current stock is the observed outcome and inference returns it with probability 0 or 1.

Use only offer data known at scoredAt. All times contain an offset. Calendar transformations use UTC. Historical aggregates are optional and, if supplied, must use only offers ending strictly before scoredAt. The request excludes customer data, titles and free-text notes.

## Endpoints

- POST /predict accepts an object with an offers array of 0–50 offers in the [request format](../tests/fixtures/offer.json). Response is an object with a predictions array in input order. Each prediction contains offerId, expectedLeftoverUnits, unsoldProbability, riskLevel, modelVersion and generatedAt (UTC).
- GET /health returns readiness and the pinned version after a successful load. GET /model-info returns version, trainedAt, features and evaluation metrics.
- All optional inputs support null. For validation failures the service returns 422 with field locations. A request over 128 KiB receives 413. Saturation receives 503 and a timed-out request receives 504. The service budgets 500 ms and echoes a supplied 16–64 character hex or UUID request ID, or generates one. The backend should fall back on any scoring failure.
- Risk is HIGH when probability reaches 0.60 or expected leftovers reach 50% of available stock. Otherwise it is MEDIUM when either threshold reaches 0.30 or 25%. All other cases, including zero stock, are LOW.

The service only forecasts unsold products. See the [model card](model-card.md), [feature audit](features.md) and [runbook](runbook.md).
