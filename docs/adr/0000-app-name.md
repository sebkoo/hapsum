# ADR-0000: App name — Hapsum

- Status: Accepted
- Date: 2026-07-27
- Implemented by: commit 1 (repository initialization)

## Context

The app needs a name before the first commit: it appears in the license note, the README, the
application namespace, and the UI string from day one, which makes it the single most irreversible
decision in this repository. The name should say what the app does (sum up receipts), be short,
be pronounceable, and be free of same-category collisions in the stores and trademark sources we
can reach.

## Decision

**Hapsum** — Korean 합 (*hap*, "sum / to combine") + English *sum*. Both halves mean the same
thing, which is what the app does with receipts. Two syllables, pronounceable in English (HAP-sum)
and Korean (합숨) alike; the bilingual pun is the mnemonic.

## Collision search — evidence (2026-07-27)

Searched, not cleared: a store or database search is not a trademark clearance. Formal clearance
is a separate legal step before any commercial launch.

| Channel | Method | Result |
|---|---|---|
| GitHub | `gh search repos hapsum` | 0 repositories; `sebkoo/hapsum` unclaimed |
| Google Play | store search + web index | no same-name or near-identical app (nearest: Happ, Haps, Hapsmiths — all distinct) |
| Apple App Store | iTunes Search API, `term=hapsum&entity=software` | 0 software results |
| US trademarks | Justia-indexed pages + aggregator web index (USPTO search UI is not scriptable) | no HAPSUM or HAP SUM mark surfaced in any category; nearest phonetic neighbors HAPSUN (textiles) and HAOSUM (kitchen utensils) are different names in unrelated categories |
| KR trademarks | web index incl. `site:kipris.or.kr` (KIPRIS search UI is not scriptable) | no 합숨/hapsum mark, app, or brand; 합섬 is a generic textile-industry word with different spelling and category |
| Web at large | "Hapsum app" and variants | no app or software product; unrelated non-app entities only (an Indian medical-supplies distributor, a fictional martial art on tabletop forums) |

Access limitation, recorded honestly: the USPTO and KIPRIS search UIs could not be queried
directly, so those rows rest on search-engine-indexed database pages, not authoritative registry
queries.

## Consequences

- The name is used everywhere from commit 1: license note, README, namespace, UI string.
- Apache-2.0 covers the code; the Hapsum name and logo are not licensed.
- Before any commercial launch (Play listing, paid Pro build), commission a formal trademark
  clearance — USPTO and KIPRIS at minimum.

## Alternatives considered

- **Zipsum** — clean sound, previously checked, but search results conflate it with Zip/BNPL
  products; kept as first fallback.
- **Scanbu** — pronounceable but unverified; second fallback, would need the same search battery
  before use.
