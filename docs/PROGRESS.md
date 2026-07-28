# Progress

The single authority on the plan. The loop ticks checkboxes and fixes typos here; adding,
removing, reordering, or changing the meaning of ladder rows is a human act at a phase gate.
The README progress board mirrors this file manually, in the same commit as each tick.

## Phase 0 — repository foundation

- [x] 1. `chore(repo): initialize with license and hygiene files`
- [x] 2. `chore(env): add Gradle wrapper and version catalog`
- [x] 3. `chore(env): scaffold app module with compose shell`
- [x] 4. `chore(ai-harness): add CLAUDE.md, slash commands, and progress board`
- [x] 5. `ci: build, test and lint on every push`
- [x] 6. `docs: publish README v1` — then public repo, About + topics, dressing audit

**Phase gate — human review before Phase 1:** architecture docs and ADR-0001 · dependency pins ·
product scope · the next stretch of the ladder.

## Phase 1+ — planned, not started

- [x] 7. `:core:model` + `:core:testing` carve-out
- [x] 8. Room schema + repository (offline-first), MockK'd DAO tests — ADR-0003 Room schema
- [x] 9. `test(core-data): exercise the Room schema against an in-memory database` — Robolectric,
  runs under verify.sh/CI (no emulator): Uncategorized seed exists in a fresh database; archive
  flag round-trips and no hard-delete path exists on the DAO; inserting an expense referencing a
  nonexistent category throws (FK RESTRICT enforced); `observeExpensesWithCategory()` returns the
  correctly mapped projection
- [x] 10. `:core:designsystem` tokens/theme
- [x] 11. `docs(harness): record the push-as-approval cadence in CLAUDE.md` — the loop commits
  locally and stops; a human review + push is the per-commit approval
- [x] 12. MVI runtime (`MviViewModel` base, reducer test harness) — ADR-0004 MVI×MVVM
- [x] 13. `:feature:ledger` list screen + ViewModel tests (Turbine) + first Compose UI test
- [x] 14. `feat(core-data): receipt persistence + schema v2` — `ReceiptEntity` mirroring the
  domain `Receipt`, receipts table, FK RESTRICT from `expenses.receiptId` to `receipts.id`
  (SQLite table-recreate migration), exportSchema v2 JSON, MigrationTestHelper test proving
  v1→v2 — the ADR-0003 migration discipline's first exercise
- [ ] 15. CameraX capture screen
- [ ] 16. ML Kit OCR + deterministic receipt parser (synthetic fixtures, golden tests)
- [ ] 17. `RuleBasedEngine` categorizer + tests
- [ ] 18. `GeminiNanoEngine` behind capability check — ADR-0005 on-device-first AI
- [ ] 19. `:feature:insights` monthly summary + Espresso interop test
- [ ] 20. `Entitlements` seam — ADR-0006 monetization without lock-in
- [ ] 21. Kover gate ≥80% on ViewModels/domain + coverage badge
- [ ] 22. Screenshots/GIF + README refresh
- [ ] 23. `v0.1.0` tagged release with changelog

## Queued badges (added only when truthful)

- Kover coverage badge (shields endpoint from a gist, no external service) — commit 21
- `github/v/release` + downloads badges + official Google Play badge — v0.1.0
- Star-history widget + Releases APK download section — once v0.1.0 exists

## Operational backlog (friction to revisit, not commits)

- Generalize the README lint into a docs lint
- Add `/review` and `/release` commands when their work exists
- Split verify.sh into `verify-*.sh` stages at ~50 lines
- Extract the `.claude/` + `scripts/` + `docs/` skeleton into a template repo after v0.1.0
- Manual expense entry without a receipt — needs `receiptId` to become nullable, a real
  migration, and its own ADR (ADR-0003 deliberately keeps it `NOT NULL` for MVP)
- Per-line-item categorization — today an `Expense` has one `CategoryId`; splitting a single
  receipt's line items across categories is deferred (ADR-0003)
