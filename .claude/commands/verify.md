---
description: Run the verify gate and summarize any failure.
model: sonnet
effort: low
---

Run `scripts/verify.sh`. If it passes, say "verify: green" in one line. If it fails, summarize
the failure in at most 5 lines and name the shortest fix path. Do not fix anything and do not
run any other command.
