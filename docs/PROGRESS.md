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
- [ ] 6. `docs: publish README v1` — then public repo, About + topics, dressing audit

**Phase gate — human review before Phase 1:** architecture docs and ADR-0001 · dependency pins ·
product scope · the next stretch of the ladder.

## Phase 1+ — planned, not started

- [ ] 7. `:core:model` + `:core:testing` carve-out
- [ ] 8. Room schema + repository (offline-first), MockK'd DAO tests
- [ ] 9. `:core:designsystem` tokens/theme
- [ ] 10. MVI runtime (`MviViewModel` base, reducer test harness) — ADR-0002 MVI×MVVM
- [ ] 11. `:feature:ledger` list screen + ViewModel tests (Turbine) + first Compose UI test
- [ ] 12. CameraX capture screen
- [ ] 13. ML Kit OCR + deterministic receipt parser (synthetic fixtures, golden tests)
- [ ] 14. `RuleBasedEngine` categorizer + tests
- [ ] 15. `GeminiNanoEngine` behind capability check — ADR-0003 on-device-first AI
- [ ] 16. `:feature:insights` monthly summary + Espresso interop test
- [ ] 17. `Entitlements` seam — ADR-0004 monetization without lock-in
- [ ] 18. Kover gate ≥80% on ViewModels/domain + coverage badge
- [ ] 19. Screenshots/GIF + README refresh
- [ ] 20. `v0.1.0` tagged release with changelog

## Queued badges (added only when truthful)

- Kover coverage badge (shields endpoint from a gist, no external service) — commit 18
- `github/v/release` + downloads badges + official Google Play badge — v0.1.0
- Star-history widget + Releases APK download section — once v0.1.0 exists

## Operational backlog (friction to revisit, not commits)

- Generalize the README lint into a docs lint
- Add `/review` and `/release` commands when their work exists
- Split verify.sh into `verify-*.sh` stages at ~50 lines
- Extract the `.claude/` + `scripts/` + `docs/` skeleton into a template repo after v0.1.0
