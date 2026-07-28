#!/usr/bin/env bash
# One gate, three enforcement points: local runs, the pre-commit hook, and CI
# all execute this exact script — they can never disagree.
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew spotlessCheck assembleDebug testDebugUnitTest test lint

# Every ADR must appear in the index. ADR-0000 predates the index by one
# commit, so the check warns instead of failing until the index exists.
if [ -f docs/adr/README.md ]; then
    for adr in docs/adr/[0-9]*.md; do
        grep -q "$(basename "$adr")" docs/adr/README.md \
            || { echo "verify: $(basename "$adr") missing from docs/adr/README.md"; exit 1; }
    done
else
    echo "verify: docs/adr/README.md not present yet — ADR index check skipped"
fi

# README lint: the §7 voice rules are enforced, not remembered.
banned='blazingly|seamlessly|revolutionize|robust|cutting-edge|elevate|empower|unleash|delve'
if grep -nEiw "($banned)" README.md; then
    echo "verify: README uses a forbidden word"
    exit 1
fi
lines=$(wc -l < README.md | tr -d ' ')
if [ "$lines" -gt 250 ]; then
    echo "verify: README is $lines lines (max 250)"
    exit 1
fi
# Emoji budget: ✅/🔜 inside the Progress section only, nothing anywhere else.
awk '/^## Progress/{inboard=1; next} /^## /{inboard=0} !inboard' README.md \
    | perl -CSD -ne 'if (/[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}\x{FE0F}]/) { print "README:$.: emoji outside progress board\n"; $bad = 1 } END { exit($bad // 0) }'
awk '/^## Progress/{inboard=1; next} /^## /{inboard=0} inboard' README.md \
    | perl -CSD -ne 's/[\x{2705}\x{1F51C}]//g; if (/[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}]/) { print "README:$.: non-budget emoji in progress board\n"; $bad = 1 } END { exit($bad // 0) }'

echo "verify: green"
