#!/usr/bin/env python3
"""Reject Quarkus build logs that contain ignored configuration keys."""

import argparse
import sys
from pathlib import Path


UNRECOGNIZED_CONFIGURATION = "Unrecognized configuration key"
SETTINGS_READER_PROBLEM = "Settings problem encountered"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--reject-settings-problems",
        action="store_true",
        help="also reject Quarkus bootstrap Maven settings reader warnings",
    )
    parser.add_argument("logs", type=Path, nargs="+")
    args = parser.parse_args()

    failures = []
    for log in args.logs:
        if not log.is_file():
            failures.append(f"missing build log: {log}")
            continue
        matches = [
            f"{line_number}: {line.strip()}"
            for line_number, line in enumerate(log.read_text(encoding="utf-8", errors="replace").splitlines(), 1)
            if UNRECOGNIZED_CONFIGURATION in line
        ]
        if matches:
            failures.append(
                f"{log}: unrecognized Quarkus configuration detected\n  " + "\n  ".join(matches)
            )
        if args.reject_settings_problems:
            settings_matches = [
                f"{line_number}: {line.strip()}"
                for line_number, line in enumerate(
                    log.read_text(encoding="utf-8", errors="replace").splitlines(), 1
                )
                if SETTINGS_READER_PROBLEM in line
            ]
            if settings_matches:
                failures.append(
                    f"{log}: Maven settings reader problem detected\n  "
                    + "\n  ".join(settings_matches)
                )

    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(
        f"Quarkus log verified: files={len(args.logs)} "
        f"unrecognized_configuration=0 settings_reader_problems=0"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
