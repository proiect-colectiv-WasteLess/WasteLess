lint:
	uv run --locked ruff check .
	uv run --locked black --check .

test:
	uv run --locked pytest

train:
	uv run --locked python -m wasteless_ml.dataset
	uv run --locked python -m wasteless_ml.train --version 0.1.0

serve:
	uv run --locked python -m wasteless_ml.serve
