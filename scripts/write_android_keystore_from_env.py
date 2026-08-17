#!/usr/bin/env python3
"""Restore the Android release keystore from a CI environment secret."""

import base64
import binascii
import os
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parent.parent
KEYSTORE_PATH = ROOT / "release.keystore"


def main() -> None:
    encoded = "".join(os.environ.get("ANDROID_KEYSTORE_BASE64", "").split())
    if not encoded:
        print("ERROR: ANDROID_KEYSTORE_BASE64 is empty", file=sys.stderr)
        raise SystemExit(1)

    try:
        decoded = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as error:
        print(f"ERROR: invalid ANDROID_KEYSTORE_BASE64: {error}", file=sys.stderr)
        raise SystemExit(1) from error

    if not decoded:
        print("ERROR: decoded keystore is empty", file=sys.stderr)
        raise SystemExit(1)

    KEYSTORE_PATH.write_bytes(decoded)
    print(f"Wrote {KEYSTORE_PATH.name}")


if __name__ == "__main__":
    main()
