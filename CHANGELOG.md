# Changelog

All notable changes to Hapsum are documented in this file. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.1.0] - 2026-07-28

First tagged release: camera capture through parsed, categorized, ledgered, and summarized
expenses, entirely on-device.

### Added

- **Capture** — CameraX preview with a permission flow, feeding a `ReceiptRepository`
- **OCR + deterministic parse** — ML Kit Text Recognition v2 behind a seam, golden-tested
  deterministic parser producing structured fields and line items
- **Categorization** — a rule-based deterministic floor, refined by Gemini Nano on supported
  hardware (ADR-0006); confirm screen shows a two-phase suggestion as the refinement lands
- **Confirm/edit** — review and correct parsed fields and category before an `Expense` is
  written
- **Ledger** — list screen over the confirmed expense history
- **Monthly insights** — timezone-free, currency-partitioned aggregation summarizing a month's
  spending
- **Offline-first storage** — Room as the single source of truth, no network layer in the MVP
- **Privacy** — nothing leaves the device; cloud AI exists only as an explicit, off-by-default
  opt-in seam
- **Quality gate** — `scripts/verify.sh` (Spotless, lint, unit tests, Kover ≥80% line coverage
  on ViewModels/domain) enforced identically local, pre-commit, and in CI; coverage gate
  introduced at 98.29% line / 82.65% branch (row 25, pre-fixture); 99.2% line / 83.3% branch
  measured at the v0.1.0 tag

[Unreleased]: https://github.com/sebkoo/hapsum/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sebkoo/hapsum/releases/tag/v0.1.0
