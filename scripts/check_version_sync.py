#!/usr/bin/env python3
"""Verify that the canonical version matches the Android app version."""

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parent.parent
SEMVER_RE = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")
VERSION_NAME_RE = re.compile(r'^\s*versionName\s*=\s*"([^"]+)"\s*$', re.MULTILINE)
VERSION_CODE_RE = re.compile(r"^\s*versionCode\s*=\s*(\d+)\s*$", re.MULTILINE)


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def single_match(pattern: re.Pattern[str], text: str, field: str) -> str:
    matches = pattern.findall(text)
    if len(matches) != 1:
        fail(f"expected exactly one {field}, found {len(matches)}")
    return matches[0]


def main() -> None:
    version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    if SEMVER_RE.fullmatch(version) is None:
        fail(f"VERSION is not X.Y.Z semver: {version!r}")

    gradle = (ROOT / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    version_name = single_match(VERSION_NAME_RE, gradle, "versionName")
    version_code = int(single_match(VERSION_CODE_RE, gradle, "versionCode"))

    if version_name != version:
        fail(f"VERSION is {version!r}, but versionName is {version_name!r}")
    if version_code < 1:
        fail(f"versionCode must be a positive monotonic integer, got {version_code}")

    print("OK")


if __name__ == "__main__":
    main()
