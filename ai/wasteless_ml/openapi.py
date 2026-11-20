import argparse
import json
from pathlib import Path

from .serve import app


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    target = Path(__file__).resolve().parents[1] / "docs/openapi.json"
    schema = json.dumps(app.openapi(), indent=2, sort_keys=True) + "\n"
    if args.check:
        if target.read_text(encoding="utf-8") != schema:
            raise SystemExit("OpenAPI schema is outdated")
    else:
        target.write_text(schema, encoding="utf-8")


if __name__ == "__main__":
    main()
