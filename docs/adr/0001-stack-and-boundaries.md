# ADR-0001: Stack and boundaries

- Status: Accepted
- Date: 2026-07-27
- Implemented by: commits 2–4 (version catalog, app shell, harness); later commits reference
  their rows as they land

## Version pins and stability strategy

Every dependency is pinned in `gradle/libs.versions.toml`; nothing floats. Kotlin is 2.3.21 —
deliberately not the newest stable (2.4.10) — because Room 3 is KSP-only and KSP 2.3.9 has a
confirmed-open codegen failure on Kotlin 2.4 (google/ksp#2964, checked 2026-07-27); the first
build must be green. AGP 9's built-in Kotlin embeds KGP 2.2.10, so the root build script pins
the higher 2.3.21 through the documented buildscript-classpath override. When a pin and the
dependency resolver disagree, the resolver wins and the change is recorded in the commit body —
that is how core/activity/lifecycle came to sit at the last stables accepting compileSdk 36.
Pins are re-verified once per phase or before each tagged release, whichever comes first; the
Kotlin pin moves to 2.4.x when the KSP issue closes.

## Navigation: Navigation 3

`androidx.navigation3` 1.1.3 (stable since 2025-11) over classic `navigation-compose` 2.9.x:
it is the Compose-first stack Google recommends for new production apps. The migration fallback
is recorded here in advance: if Nav3 blocks a feature, classic `navigation-compose:2.9.8` is
the retreat and this section gains a Superseded note explaining what blocked it.

## Namespace: io.github.sebkoo.hapsum

Derived from a GitHub account already controlled, so no personal domain purchase is required
and the namespace stays predictable. Changing a shipped applicationId is a one-way door: if a
domain is owned by Play-launch time, this ADR must be revisited and superseded *before* any
store release. A unit test asserts the applicationId so the door stays visibly closed.

## SDK levels: min 26, compile/target 36

Play requires targetSdk ≥ 36 from 2026-08-31, and Android 17 (API 37) was still in beta when
this was decided (2026-07-27) — previews are never targeted. Consequence already felt in Phase 0:
the newest androidx stables (core 1.19, activity 1.13, lifecycle 2.11) require compileSdk 37,
so those three ride one stable behind until the target moves. minSdk 26 allows java.time,
adaptive icons, and notification channels without compat gymnastics while keeping old devices
in reach. Revisit both numbers when API 37 reaches stable.

## AI: the AiEngine ladder

`:core:ai` will expose an `AiEngine` interface with three implementations — `GeminiNanoEngine`
(on-device via the ML Kit GenAI APIs / AICore; beta, Pixel-9/10-class hardware, availability
checked at runtime), `RuleBasedEngine` (deterministic keyword/heuristic categorizer — always
available, fully unit-testable), and a `CloudEngine` seam (Firebase AI Logic; interface-only in
MVP, strictly opt-in, OFF by default). Runtime capability detection picks the best available
engine; degradation is graceful and visible in the UI. The full design is deferred to ADR-0006
(commit 19) — this ADR only fixes the boundary: all AI behind one interface, on-device first.

## License and trademark

Apache-2.0 for the code — permissive enough for a portfolio, patent-explicit enough for
finance-adjacent work. The Hapsum name and logo are not covered by the license; the naming
evidence and its clearance caveat live in ADR-0000.
