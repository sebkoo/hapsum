#!/usr/bin/env bash
# Points git at the repo's versioned hooks. Run once after clone.
set -euo pipefail
cd "$(dirname "$0")/.."
git config core.hooksPath scripts/git-hooks
echo "hooks: core.hooksPath -> scripts/git-hooks"
