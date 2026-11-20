import argparse
import json
from pathlib import Path

import joblib

from .dataset import sha256
from .evaluation import markdown
from .train import read_snapshot, report_for, runtime


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("snapshot", type=Path)
    args = parser.parse_args()
    metadata = json.loads((args.candidate / "metadata.json").read_bytes())
    prior_report = json.loads((args.candidate / "evaluation.json").read_bytes())
    digest, rows = read_snapshot(args.snapshot)
    artifact = args.candidate / "model.joblib"
    if (
        digest != metadata["datasetHash"]
        or sha256(artifact) != metadata["artifactHash"]
    ):
        raise ValueError("Snapshot or artifact hash does not match model metadata")
    if metadata["runtime"] != runtime():
        raise ValueError("Pinned dependencies are required to reproduce the report")
    champion = prior_report["promotedVersion"]
    report = report_for(joblib.load(artifact), rows, bool(champion), champion)
    if report != prior_report:
        raise ValueError("Regenerated evaluation differs from the stored evaluation")
    print(markdown(report), end="")


if __name__ == "__main__":
    main()
