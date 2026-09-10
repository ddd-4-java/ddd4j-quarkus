#!/usr/bin/env python3
"""Reject Quarkus build logs that contain ignored configuration keys."""

import argparse
import sys
from pathlib import Path


UNRECOGNIZED_CONFIGURATION = "Unrecognized configuration key"
DEPRECATED_CONFIGURATION = "config property is deprecated"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("logs", type=Path, nargs="+")
    args = parser.parse_args()

    failures = []
    for log in args.logs:
        if not log.is_file():
            failures.append(f"missing build log: {log}")
            continue
        unrecognized_matches = [
            f"{line_number}: {line.strip()}"
            for line_number, line in enumerate(log.read_text(encoding="utf-8", errors="replace").splitlines(), 1)
            if UNRECOGNIZED_CONFIGURATION in line
        ]
        if unrecognized_matches:
            failures.append(
                f"{log}: unrecognized Quarkus configuration detected\n  "
                + "\n  ".join(unrecognized_matches)
            )
        deprecated_matches = [
            f"{line_number}: {line.strip()}"
            for line_number, line in enumerate(log.read_text(encoding="utf-8", errors="replace").splitlines(), 1)
            if DEPRECATED_CONFIGURATION in line
        ]
        if deprecated_matches:
            failures.append(
                f"{log}: deprecated Quarkus configuration detected\n  "
                + "\n  ".join(deprecated_matches)
            )

    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(
        f"Quarkus log verified: files={len(args.logs)} "
        "unrecognized_configuration=0 deprecated_configuration=0"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
