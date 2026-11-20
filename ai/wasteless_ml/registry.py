import argparse
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from urllib.parse import urlparse

import boto3
import joblib
from botocore.exceptions import ClientError
from filelock import FileLock

from .dataset import sha256
from .train import runtime, validate_version

POINTER = "promoted.json"
FILES = (
    "model.joblib",
    "metadata.json",
    "evaluation.json",
    "evaluation.md",
    "fallback.json",
    "experiment.json",
    "model-card.md",
)


class Registry:
    def __init__(self):
        uri = os.getenv("ARTIFACT_URI")
        self.path = Path(os.getenv("MODEL_DIR", "var/registry"))
        self.s3 = urlparse(uri) if uri else None
        if self.s3 and (self.s3.scheme != "s3" or not self.s3.netloc):
            raise ValueError("ARTIFACT_URI must be s3://bucket[/prefix]")
        self.client = (
            boto3.client("s3", endpoint_url=os.getenv("S3_ENDPOINT_URL") or None)
            if self.s3
            else None
        )

    def address(self, key):
        return {
            "Bucket": self.s3.netloc,
            "Key": "/".join(filter(None, [self.s3.path.strip("/"), key])),
        }

    def read(self, key):
        if not self.client:
            return (self.path / key).read_bytes()
        try:
            with self.client.get_object(**self.address(key))["Body"] as body:
                return body.read()
        except ClientError as exc:
            if exc.response["Error"]["Code"] in {"NoSuchKey", "404"}:
                raise FileNotFoundError(key) from exc
            raise

    def pointer_state(self):
        try:
            if not self.client:
                return json.loads(self.read(POINTER)), None
            response = self.client.get_object(**self.address(POINTER))
            with response["Body"] as body:
                return json.load(body), response["ETag"]
        except FileNotFoundError:
            pass
        except ClientError as exc:
            if exc.response["Error"]["Code"] not in {"NoSuchKey", "404"}:
                raise
        return {"current": None, "previous": None}, None

    def pointer(self):
        return self.pointer_state()[0]

    def set_pointer(self, current, previous, expected=None, etag=None):
        data = json.dumps({"current": current, "previous": previous}, indent=2).encode()
        if self.client:
            condition = {"IfMatch": etag} if etag else {"IfNoneMatch": "*"}
            self.client.put_object(
                **self.address(POINTER),
                Body=data,
                ContentType="application/json",
                **condition,
            )
        else:
            with FileLock(self.path / ".pointer.lock", timeout=10):
                if expected is not None and self.pointer() != expected:
                    raise ValueError("Promotion pointer changed during the operation")
                with TemporaryDirectory(dir=self.path) as staging:
                    pending = Path(staging) / POINTER
                    pending.write_bytes(data)
                    pending.replace(self.path / POINTER)

    def store(self, version, candidate):
        prefix = f"versions/{version}"
        if self.client:
            manifest = json.dumps(
                {name: sha256(candidate / name) for name in FILES}
            ).encode()
            for name, data in [
                ("manifest.json", manifest),
                *((name, (candidate / name).read_bytes()) for name in FILES),
            ]:
                key = f"{prefix}/{name}"
                try:
                    self.client.put_object(
                        **self.address(key), Body=data, IfNoneMatch="*"
                    )
                except ClientError as exc:
                    if (
                        exc.response["ResponseMetadata"]["HTTPStatusCode"] != 412
                        or self.read(key) != data
                    ):
                        raise
        else:
            versions = self.path / "versions"
            versions.mkdir(parents=True, exist_ok=True)
            destination = versions / version
            if destination.exists():
                raise FileExistsError(destination)
            with TemporaryDirectory(dir=versions) as staging:
                pending = Path(staging) / version
                pending.mkdir()
                for name in FILES:
                    (pending / name).write_bytes((candidate / name).read_bytes())
                pending.rename(destination)


def load(pinned_version=None):
    registry = Registry()
    model_version = (
        pinned_version or os.getenv("MODEL_VERSION") or registry.pointer()["current"]
    )
    if not model_version:
        raise FileNotFoundError(
            "No promoted model. Set MODEL_DIR or ARTIFACT_URI and promote a candidate."
        )
    prefix = f"versions/{validate_version(model_version)}"
    metadata = json.loads(registry.read(f"{prefix}/metadata.json"))
    if metadata["version"] != model_version or metadata["runtime"] != runtime():
        raise ValueError("Model version or dependency mismatch")
    with TemporaryDirectory() as directory:
        path = Path(directory) / "model.joblib"
        path.write_bytes(registry.read(f"{prefix}/model.joblib"))
        if sha256(path) != metadata["artifactHash"]:
            raise ValueError("Model artifact SHA-256 mismatch")
        bundle = joblib.load(path)
    if (
        bundle["version"] != model_version
        or bundle["datasetHash"] != metadata["datasetHash"]
    ):
        raise ValueError("Model bundle version or dataset hash mismatch")
    return bundle, metadata


def promote(registry, candidate):
    metadata = json.loads((candidate / "metadata.json").read_bytes())
    report = json.loads((candidate / "evaluation.json").read_bytes())
    model_version = validate_version(metadata["version"])
    pointer, etag = registry.pointer_state()
    current = pointer["current"]
    if not report["passed"] or not all(report["checks"].values()):
        raise ValueError("Candidate failed evaluation gates")
    if report["promotedVersion"] != current:
        raise ValueError(
            "Evaluate the candidate against the currently promoted version"
        )
    if (
        report["version"] != model_version
        or report["datasetHash"] != metadata["datasetHash"]
        or report["comparison"]["candidate"] != metadata["metrics"]
        or metadata["artifactHash"] != sha256(candidate / "model.joblib")
    ):
        raise ValueError("Candidate metadata does not match its evaluation or artifact")
    for name in FILES:
        if not (candidate / name).is_file():
            raise FileNotFoundError(candidate / name)
    registry.store(model_version, candidate)
    registry.set_pointer(model_version, current, expected=pointer, etag=etag)


def rollback(registry):
    pointer, etag = registry.pointer_state()
    if not pointer["previous"]:
        raise ValueError("No previous promoted version")
    registry.set_pointer(
        pointer["previous"], pointer["current"], expected=pointer, etag=etag
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    promotion = subparsers.add_parser("promote")
    promotion.add_argument("candidate", type=Path)
    subparsers.add_parser("rollback")
    args = parser.parse_args()
    registry = Registry()
    if args.command == "promote":
        promote(registry, args.candidate)
    else:
        rollback(registry)
    print(registry.pointer())
