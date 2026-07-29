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
  now (the android-library conversion belongs to row 22, when `GeminiNanoEngine` needs a
  `Context`). A pure keyword categorizer: whole-word case-insensitive matching, rule order as
  deterministic precedence for ambiguous words, `UNCATEGORIZED` floor. No `AiEngine` interface
  yet — ADR-0001 fixed the boundary and explicitly deferred that design to ADR-0006 (row 22).
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
- [x] 20. `test(integration): prove the capture→confirm→ledger loop end-to-end against a real
  in-memory database` — Robolectric/JVM only, runs under verify.sh/CI. Seeds a `Receipt` with
  parse output (one HIGH field, one LOW field, line items with a totals mismatch), drives
  `ConfirmViewModel` through the journey (prefills verified, user edits the LOW-confidence
  amount, changes the suggested category, saves), then asserts through the ledger read path that
  exactly one expense exists with the edited amount, the user's chosen category, and the
  `receiptId` linkage. The executable version of the manual boot walkthrough — the loop is the
  product, so the loop gets a test. No production code changes expected from this row.
- [x] 21. `fix(money): shared locale-aware Money display formatting, replacing the display placeholders`
  — one correct shared formatter respecting ISO-4217 fraction digits (a 12000-minor-unit KRW
  amount renders as 12,000 KRW, never 120.00), replacing the duplicated two-decimal placeholders
  in `:feature:ledger` and `:feature:confirm`; unit tests pin 0-, 2-, and 3-fraction-digit
  currencies; `Money` itself stays formatting-free (ADR-0002 — no new ADR needed, the boundary
  is already drawn). Closes a known-wrong display path (triage law) before a third consumer
  (insights, row 23) arrives
- [x] 22. `GeminiNanoEngine` behind capability check — ADR-0006 on-device-first AI
- [x] 23. `:feature:insights` monthly summary + Espresso interop test —
  `AggregateMonthlySummariesUseCase`, the first feature-local use case per ADR-0004's named
  candidate: buckets by `YearMonth.from(expense.date)` (timezone-free by construction, no
  `Clock`/`Instant`), partitions by `CurrencyCode` before any `Money.plus` fold so a
  mixed-currency month yields one summary per currency instead of throwing (triage law), and
  groups by the joined `Category` so archived categories still appear under their resolved name
  (ADR-0003). `InsightsContentTest` (Robolectric, `createComposeRule`) is row 23's UI test,
  running under verify.sh/CI with no emulator — the ladder's Espresso-interop wording resolves
  to a Robolectric Compose-UI test because Hapsum has no View/Compose hybrid boundary to justify
  a device-only androidTest. Rides along: `MoneyFormatterTest` negative-amount and cross-locale
  cases, plus its three comma-vs-period test-name typo fixes.
- [x] 24. `Entitlements` seam — ADR-0007 monetization without lock-in — closed `Entitlement`
  enum in `:core:model` making ADR-0006's CloudEngine precondition checkable, `Flow<Boolean>`
  read surface + `FreeTierEntitlements` bound in `DataModule` under `:core:data`, zero
  consumers by design (no gate on any free feature), pure JVM ahead of row 25's Kover gate
- [x] 25. Kover gate ≥80% on ViewModels/domain + coverage badge — merged "gate" variant (Kover
  0.9.8) over ViewModels/domain across ten modules, LINE minBound 80 riding inside
  `scripts/verify.sh`; measured 98.29% line / 82.65% branch at introduction (branch not gated —
  compiler-generated coroutine/reducer branches would incentivize filler tests); named
  zero-coverage-by-design exclusions (`GeminiNanoEngine*` per ADR-0006,
  `DefaultDispatcherProvider*`); added the textual day-month-year date fixture
  (`ReceiptParser.kt:192-195`) closing the one substantial honest gap the measurement found.
  Coverage badge omitted — the human pre-work it depends on (gist +
  `KOVER_BADGE_GIST_TOKEN` repo secret) is not yet in place. Backlog, not this row: filler-free
  tests for `MviViewModel.requeue` and the `ExpenseMapping` branch gaps
- [x] 26. Screenshots/GIF + README refresh — captured on a Pixel_9 AVD (API 37,
  `google_apis_playstore`), `-camera-back virtualscene` fed a synthetic receipt PNG as a scene
  poster; drove capture→confirm (`Dining` floor suggestion visible, `RuleBasedEngine` via the
  "cafe" keyword)→ledger→insights plus empty states via adb, real ML Kit OCR end to end (no
  fabricated data). Human-approved gate decision (row 26 context): the Nano-specific demo is
  device-gated (ADR-0006) and stays deferred to real Pixel-9/10-class hardware — this row's
  screenshots show the deterministic floor only, captioned as such. GIF assembled from the same
  genuine app screenshots (ffmpeg palette-optimized) rather than a raw screen recording, since
  the recording's second take hit OCR's own honest nondeterminism (a blank date field, disabled
  Save) — truthful degradation per the triage law, just not the take chosen for the README.
  Coverage badge wired this row: gist `ac07feacc241fbdc26a0ea54b7138498` created,
  `KOVER_BADGE_GIST_TOKEN` fine-grained PAT set by the human, `scripts/publish-coverage-badge.sh`
  + a continue-on-error main-push CI step publish it — badge now live next to CI in the README
- [x] 27. `v0.1.0` tagged release with changelog — `assembleRelease` measured green first-try
  (unsigned, unminified, 55 MB) ahead of this row, human-approved as the closing gate context.
  Debug-signed sideload decision: `signingConfig = signingConfigs.getByName("debug")` on the
  release build type is the sideload path for this release, with a comment marking it as
  evaluation-only until a human-held release keystore replaces it before any Play submission.
  No ADR — the signing choice and versionName bump are reversible conventions, not hard-to-reverse
  architecture. `versionCode` stays 1: nothing was ever released under it, so there is no prior
  release to increment past. `CHANGELOG.md` added (Keep a Changelog form, distilled by
  capability). Queued badges split this row: `github/v/release` + downloads badges go live now;
  the official Google Play badge stays queued since the app isn't on Play.

## Queued badges (added only when truthful)

- ~~Kover coverage badge (shields endpoint from a gist, no external service) — commit 25~~ live
  as of commit 26
- ~~`github/v/release` + downloads badges — v0.1.0~~ live as of commit 27
- ~~Releases APK download section — v0.1.0~~ live as of commit 27
- Star-history widget — retired (README maintenance commit) pending an actual curve; the
  trigger is once stars form a visible curve, not merely once a release exists
- Official Google Play badge — queued until the app is actually on Play

## Operational backlog (friction to revisit, not commits)

- R8/resource shrinking — v0.1.0's `assembleRelease` measured 55 MB unminified; shrink before
  the next release
- Real release keystore before any Play submission — v0.1.0 ships debug-signed, sideload only
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
- Split transactions — multiple expenses per receipt (one per confirmed line item, each its own
  category); the v4 `expenses.lineItemId` FK already supports this, confirm just doesn't write
  it yet (row 19 writes one `Expense` per receipt)
- Suggestion-vs-correction history — recording when a user changes an engine's suggested
  category is an on-device learning signal and a candidate Pro-tier insight, not built yet
- Real-device Gemini Nano demo capture — row 26's screenshots/GIF used the emulator's
  deterministic-floor path only (ADR-0006's differentiator is device-gated to Pixel-9/10-class
  hardware, which the emulator cannot represent); capture the on-device refinement once
  supported hardware is available
- `MviViewModel.requeue` + `ExpenseMapping` branch coverage — filler-free tests for the gaps
  row 25's Kover gate introduction named but didn't close
- Localized month names in Insights — `MonthSection` renders `summary.month.toString()`
  (`YearMonth`'s ISO form, e.g. "2026-07") instead of a locale-formatted month name
