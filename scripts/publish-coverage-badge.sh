#!/usr/bin/env bash
# Publishes the Kover gate line-coverage % as a shields.io endpoint badge, via the
# hapsum-coverage.json gist. Requires KOVER_BADGE_GIST_TOKEN (a fine-grained PAT scoped to
# Gists:RW only) in the environment. Never fails the calling job — the CI step wrapping this
# runs continue-on-error, so a badge hiccup never blocks a merge (truthful-badge law: the badge
# reflects real coverage or doesn't update, it never blocks shipping).
set -euo pipefail
cd "$(dirname "$0")/.."

GIST_ID="ac07feacc241fbdc26a0ea54b7138498"
REPORT="build/reports/kover/reportGate.xml"

./gradlew koverXmlReportGate

line=$(grep -o '<counter type="LINE" missed="[0-9]*" covered="[0-9]*"/>' "$REPORT" | tail -1)
missed=$(echo "$line" | sed -E 's/.*missed="([0-9]+)".*/\1/')
covered=$(echo "$line" | sed -E 's/.*covered="([0-9]+)".*/\1/')
total=$((missed + covered))
pct=$(awk "BEGIN { printf \"%.1f\", (100.0 * $covered) / $total }")
pct_int=${pct%.*}

if [ "$pct_int" -ge 90 ]; then
    color="brightgreen"
elif [ "$pct_int" -ge 80 ]; then
    color="green"
elif [ "$pct_int" -ge 70 ]; then
    color="yellowgreen"
elif [ "$pct_int" -ge 50 ]; then
    color="yellow"
else
    color="red"
fi

badge=$(jq -n --arg msg "${pct}%" --arg color "$color" \
    '{schemaVersion: 1, label: "coverage", message: $msg, color: $color}')

jq -n --arg content "$badge" '{files: {"hapsum-coverage.json": {content: $content}}}' \
    | curl -sf -X PATCH \
        -H "Authorization: token ${KOVER_BADGE_GIST_TOKEN}" \
        -H "Accept: application/vnd.github+json" \
        "https://api.github.com/gists/${GIST_ID}" \
        -d @- >/dev/null

echo "coverage badge: ${pct}% (${color})"
