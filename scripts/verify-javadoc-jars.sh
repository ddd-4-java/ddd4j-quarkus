#!/usr/bin/env bash

set -euo pipefail
shopt -s nullglob

project_root="${1:-.}"
expected_count=0
verified_count=0

while IFS= read -r source_directory; do
    module_directory="${source_directory%/src/main/java}"
    expected_count=$((expected_count + 1))

    javadoc_jars=("$module_directory"/target/*-javadoc.jar)
    if [[ ${#javadoc_jars[@]} -ne 1 ]]; then
        echo "Expected exactly one Javadoc jar for $module_directory, found ${#javadoc_jars[@]}" >&2
        exit 1
    fi

    javadoc_jar="${javadoc_jars[0]}"
    unzip -tq "$javadoc_jar" >/dev/null
    if ! unzip -Z1 "$javadoc_jar" | awk '/\.html$/ { found = 1 } END { exit !found }'; then
        echo "Javadoc jar contains no HTML: $javadoc_jar" >&2
        exit 1
    fi
    verified_count=$((verified_count + 1))
done < <(find "$project_root" -type d -path '*/src/main/java' \
    -not -path '*/target/*' -print | sort)

if [[ $expected_count -eq 0 ]]; then
    echo "No Java source modules found under $project_root" >&2
    exit 1
fi

echo "Verified $verified_count/$expected_count Javadoc jars: archives are readable and contain HTML."
