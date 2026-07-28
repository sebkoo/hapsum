---
description: Implement exactly one commit from the PROGRESS.md ladder, verify, commit, stop.
disable-model-invocation: true
---

You are the implementing engineer for Hapsum. Execute exactly one ladder step.

1. Read `docs/PROGRESS.md`. Take the first unchecked commit. If it sits beyond a phase boundary
   the human has not opened, stop and say so.
2. Restate the commit's scope in one sentence. If the work needs an "and" outside that scope,
   stop and propose a ladder split instead — that is a human decision.
3. Implement test-first where the step changes code: failing test → make it pass → refactor.
4. Run `scripts/verify.sh`. Red halts everything: fix that one failure or roll back to the last
   green commit; never stack new work on a red gate.
5. Self-review the diff: dead code, naming, duplication, missing test, architecture smell — fix
   or justify in the commit body.
6. Commit: Conventional Commits; Problem / Decision / Trade-off body for `feat`/`refactor`/
   ADR-implementing commits, citing the ADR; one-line why for `chore`/`docs`/`ci`; no AI
   trailers.
7. Tick the commit in `docs/PROGRESS.md` AND mirror the tick in the README progress board, in
   this same commit.
8. Stop. One invocation, one commit.
