# Feature and leakage audit

The shared feature transform is in wasteless_ml/features.py. It derives discount as 1 minus price divided by original price, pickup duration and hour, weekday, and offer age. It passes price, original price, quantity total and available, category, optional business size and two optional trailing sell-through rates. The sell-through window is 90 days and excludes outcomes at or after scoredAt. Category/hour uses the past offer's UTC pickup start hour. Business keys are used only for grouping and dropped before the snapshot is written. Location is excluded from the first version.

The transform selects an explicit allowlist and checks that the label columns do not appear in it. The fit step learns medians, scaling and one-hot categories from the training split. Unknown categories map to zeros in the category columns, and absent optional numbers map to training medians. Train and validation rows whose labels would be unavailable at the next split boundary are excluded. The test suite compares exact training and serving feature vectors and tests strict history cutoffs.

The per-version experiment.json contains split-count importances for both models using the encoded feature names. Importances describe how the fitted trees split and do not measure causality.

The P26 view must expose immutable publication-time price, category, initial stock and pickup schedule. Validate those semantics when the migration is available. Outcomes reconstructed from mutable current fields would violate this audit.
