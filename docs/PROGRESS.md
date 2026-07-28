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

## Phase 1 — infrastructure and the first screen

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

**Phase gate — human review before Phase 2:** rows 13–14 reviewed, pushed, CI green.

## Phase 2 — capture and on-device categorization

- [x] 15. `feat(di): adopt Hilt, retire AppContainer` — ADR-0005 Hilt over Koin/manual, including
  the honest origin story (manual container to keep the graph visible, Hilt once the second
  ViewModel approached); migrates every current injection site (database, repositories,
  dispatchers, `LedgerViewModel` → `@HiltViewModel`); no Hilt test infrastructure until a test
  needs it
- [x] 16. `feat(capture): CameraX capture screen` — copies the ledger MVI template; permission
  flow; `ReceiptRepository` + its read surface arrive here (their first user); the first
  effect-emitting screen (the Compose effect-collection helper and possibly `MainDispatcherRule`
  find their first real users); navigation effect carries the new receipt id. Capture writes the
  JPEG to app-private storage (`filesDir/receipts/<receiptId>.jpg`) — `ReceiptEntity.imageRef` is
  the app-managed relative path, never a transient camera `content://` Uri or MediaStore. A
  Receipt without an Expense is a designed state (captured evidence awaiting confirm), not a
  failure to prevent — no transaction wraps capture into an Expense; confirm's Expense +
  line-item write is the `@Transaction`. Unconfirmed receipts stay invisible in MVP (roadmap
  note below, not this row's scope)
- [x] 17. `feat(ocr): ML Kit OCR + deterministic receipt parser` — against synthetic golden
  fixtures. The determinism boundary: ML Kit sits behind an `OcrEngine` seam (thin adapter, zero
  coverage by design — the `CameraCapture` trade), and `parseReceipt` is a pure JVM function from
  OCR text structure to `ParsedReceipt`, golden-tested on committed synthetic OCR-text fixtures —
  never image→parse end-to-end, which is device- and ML-Kit-model-version-dependent. Amounts
  parse into `Money` minor units via the single app-default currency (device locale at receipt
  creation), respecting that currency's ISO-4217 fraction digits — no hardcoded two decimals.
  Confidence is per-field (`ParsedField` HIGH/LOW, absent = null) so the future confirm screen
  can highlight uncertain fields; schema v3 adds the parsed header columns and the `line_items`
  table (v2→v3 migration, `MigrationTestHelper`-proven). Scope ends at capture storing ocrText +
  parse output on the `Receipt` — the confirm screen is a discovered missing ladder row, flagged
  below for the Phase 3 gate
- [x] 18. `feat(ai): RuleBasedEngine categorizer` + tests — `:core:ai` carved out, pure JVM for
  now (the android-library conversion belongs to row 20, when `GeminiNanoEngine` needs a
  `Context`). A pure keyword categorizer: whole-word case-insensitive matching, rule order as
  deterministic precedence for ambiguous words, `UNCATEGORIZED` floor. No `AiEngine` interface
  yet — ADR-0001 fixed the boundary and explicitly deferred that design to ADR-0006 (row 20).
  The engine's vocabulary and the startup seed share one source of truth (`DefaultCategories`
  in `:core:model`), so every id the engine can suggest exists in the database by construction
  — no FK ambush when confirm starts persisting suggestions

**Phase gate — human review before Phase 3.**

## Phase 3 — on-device AI, insights, monetization

- [x] 19. `feat(confirm): add the confirm/edit screen` — `:feature:confirm` carved out; reads the
  immutable `Receipt` and writes the `Expense` (schema v4 adds the `expenses.lineItemId` FK,
  table-recreate migration, `MigrationTestHelper` v3→v4); category prefills from
  `RuleBasedEngine`, always user-changeable; totals-mismatch hint is display-only; save clears
  capture/confirm from the back stack and returns to the ledger
- [ ] 20. `GeminiNanoEngine` behind capability check — ADR-0006 on-device-first AI
- [ ] 21. `:feature:insights` monthly summary + Espresso interop test
- [ ] 22. `Entitlements` seam — ADR-0007 monetization without lock-in
- [ ] 23. Kover gate ≥80% on ViewModels/domain + coverage badge
- [ ] 24. Screenshots/GIF + README refresh
- [ ] 25. `v0.1.0` tagged release with changelog

## Queued badges (added only when truthful)

- Kover coverage badge (shields endpoint from a gist, no external service) — commit 23
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
- Unconfirmed-receipts inbox and cleanup policy — capture (commit 16) can leave a `Receipt` with
  no `Expense` (a designed state, not a bug); a UI to surface or a policy to expire these is
  future scope, not commit 16's
- Multi-currency detection — commit 17 parses every amount in the single app-default currency
  (device locale at receipt creation); reading the currency off the receipt text itself is
  future scope
- OCR is Latin-script only (bundled ML Kit model) — other script packs (e.g. Korean) are a
  pinned-dependency decision for a later row
- Noise-realistic synthetic OCR fixtures — today's golden fixtures are clean text; hand-model
  fixtures on observed receipt structure (skew, dropout, misreads) to stress the parser, never
  pasted real OCR output, per the no-real-receipts law
- Currency-symbol override of locale — let a symbol detected in receipt text override the
  device-locale currency assumption commit 17 made, ahead of full multi-currency detection above
- Arithmetic cross-check beyond the confirm hint — row 19's totals-mismatch notice is
  display-only; a stronger reconciliation (e.g. flagging which line item is likely wrong) is
  future scope
- Quantity notation — line items don't yet parse a quantity/unit-price split out of a single
  amount; deferred until a receipt fixture actually needs it
- Mid-receipt dropout fixtures — OCR fixtures where a middle section (not just edges) is
  missing or garbled, to exercise the parser's partial-data handling
