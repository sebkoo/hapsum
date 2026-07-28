# ADR-0006: On-device-first AI — the AiEngine interface and the Gemini Nano ladder

- Status: Accepted
- Date: 2026-07-28
- Implemented by: commit 22

## Context

ADR-0001 fixed the boundary and deferred the design to this ADR: all AI behind one interface;
on-device first; `GeminiNanoEngine` checked at runtime; `CloudEngine` is a seam only (interface
in MVP, strictly opt-in, OFF by default); runtime capability detection picks the best available
engine; degradation is graceful. ADR-0003 adds the invariant this design must uphold: **every
`CategoryId` an engine can emit must already exist in the database** — constrain the vocabulary
or floor to `UNCATEGORIZED`; no engine may invent an id. Neither boundary is relitigated here;
this ADR decides the shape inside them.

The design is grounded in the SDK surface as it exists today, not as remembered (checked
2026-07-28 against developer.android.com/ai/gemini-nano, developers.google.com/ml-kit/genai,
…/genai/prompt/android/get-started, and …/genai/prompt/android/structured-output):

- **ML Kit GenAI APIs are Google's recommended access path to Gemini Nano.** The lower-level
  AI Edge SDK / AICore experimental access is positioned as the plumbing underneath, not the
  developer surface. Inference runs in Android's AICore system service on-device; per its
  documentation AICore manages model distribution and updates, isolates each request, and
  retains neither inputs nor outputs after processing.
- **The Prompt API** (`com.google.mlkit:genai-prompt`, currently `1.0.0-beta2`; the catalog
  pins `beta1` and the implementing row bumps it) is the right surface for categorization —
  the task-specific GenAI APIs (summarization, proofreading, rewriting, image description)
  don't cover it. Surface: `Generation.getClient()` → `GenerativeModel`; `checkStatus()` →
  `FeatureStatus` `AVAILABLE`/`DOWNLOADABLE`/`DOWNLOADING`/`UNAVAILABLE`; `download()` as a
  Flow; suspend `generateContent(request)`; request params include `temperature`, `topK`,
  `maxOutputTokens`, `seed`. Minimum API 26 (exactly our minSdk); not supported on unlocked
  bootloaders. Beta: "not subject to any SLA or deprecation policy."
- **Device support is narrow and version-split**: nano-v3 on Pixel-10-class and select newer
  devices, nano-v2 on S25-era Samsung and several OEMs. Most devices — and every CI JVM — have
  no Nano at all. The deterministic floor is the common case, not the edge case.
- **Transient failure is a normal operating condition**: the framework enforces per-app
  inference quotas (`ErrorCode.BUSY`) and requires the app be foreground
  (`BACKGROUND_USE_BLOCKED`). Fallback must therefore be per-request, not just per-device.
- **Structured output is Alpha and best-effort**: `@Generable`/`@Guide(enumValues = …)` via a
  separate KSP processor (`genai-schema-compiler:1.0.0-alpha1`) can request an enum-constrained
  field, but a constraint violation is a documented `finishReason` — the schema is guidance,
  not a guarantee. Post-hoc validation is required regardless of whether it is used.

This is hard to reverse because the interface signature propagates to every engine and every
consumer (confirm now, insights later), and because the vocabulary-enforcement mechanism is
what makes the ADR-0003 invariant hold for a non-deterministic engine.

## Decision

**One `AiEngine` interface in `:core:ai`, typed evidence in, bare `CategoryId` out, suspend:**

```kotlin
data class CategorizationEvidence(
    val merchant: String?,
    val lineItemDescriptions: List<String>,
)

/** Expected runtime inability — the only exception the chain ever catches. */
class AiEngineUnavailable(message: String, cause: Throwable? = null) : Exception(message, cause)

interface AiEngine {
    /**
     * Total for the deterministic floor; generative engines throw [AiEngineUnavailable] —
     * the chain contains exactly that, nothing else.
     */
    suspend fun categorize(evidence: CategorizationEvidence): CategoryId
}
```

- **Typed evidence, not a pre-joined string.** A prompt benefits from structure ("merchant: X;
  items: Y, Z"); the rule engine adapts by joining. `RuleBasedEngine.categorize(text: String)`
  stays as the pure core; an interface adapter delegates to it. Widening evidence later is an
  additive data-class change, not an interface break.
- **Bare `CategoryId`, no suggestion-confidence pair.** Confirm treats every suggestion as
  user-changeable regardless of confidence; a keyword engine has no honest confidence scale,
  and inventing one to pair with an LLM's would fake precision (the same argument that made
  `ParseConfidence` a two-level enum, not a float). Uncertainty is spelled `UNCATEGORIZED`,
  which the UI already renders as "pick one." Suggestion-vs-correction history (backlog) layers
  on top without changing this signature.
- **suspend, not Flow.** One scalar result; streaming a category id has no UI meaning.
  Cancellation is structured concurrency — engines rethrow `CancellationException`, the house
  pattern everywhere else.
- **Timeout is chain policy, not engine-internal.** The chain wraps generative engines in
  `withTimeoutOrNull` (budget proposed at 2 s — TODO: tune on device in the implementing row);
  the deterministic floor is exempt (microseconds, total).

**Engine selection is an ordered chain in `:core:ai`, itself an `AiEngine`:** Nano first,
`RuleBasedEngine` as the floor. A generative engine signals expected unavailability by
throwing the typed `AiEngineUnavailable` (for example: unavailable capability, `BUSY` state,
or an unresolvable response). The chain catches only `AiEngineUnavailable` and the timeout it
applies itself, then falls through to the next engine. All other exceptions propagate,
because they represent implementation defects rather than expected runtime conditions. The
floor never throws, so the chain is total by construction. **The chain owns all orchestration policy; individual engines never decide
fallback behavior** — an engine that swallows its own failure to answer with another strategy
is a bug, not a feature, because it would create hidden precedence the chain's tests can't
see. Concretely, `GeminiNanoEngine` consults `checkStatus()` per request: `AVAILABLE` → infer;
`DOWNLOADABLE` → request `download()` once per process (AICore owns the transfer policy) and
fall through; `DOWNLOADING`/`UNAVAILABLE` → fall through. The chain is provided as the one
`@Singleton AiEngine` binding by a new `AiModule` in `:core:ai` — colocated with the layer
that owns the type, the ADR-0005 `DataModule` precedent. `:feature:confirm`'s
`provideRuleBasedEngine` retires; `ConfirmViewModel` depends on `AiEngine`.

**Failure taxonomy — AI failure is expected; AI contract violation is a bug:**

| Failure                                             | Chain behavior                  |
| --------------------------------------------------- | ------------------------------- |
| Model unavailable / downloading (`FeatureStatus`)   | fall through                    |
| Inference timeout (chain budget)                    | fall through                    |
| Quota / foreground (`BUSY`, `BACKGROUND_USE_BLOCKED`) | fall through                  |
| Response unresolvable to a seeded id                | `AiEngineUnavailable` → fall through |
| `CancellationException`                             | rethrown, always                |
| Anything else (programmer error)                    | crash — a test failure, not UX  |

The unresolvable-response row matters: it is treated as engine failure, not as a terminal
`UNCATEGORIZED`, so the floor still answers from the *original evidence* — the user loses
nothing merely because a generative engine rambled.

**Suggestion never blocks the confirm screen.** The UI must never block receipt confirmation
on AI availability: the second phase can only improve a pending suggestion — it never gates
`canSave` and never delays first paint. Confirm prefills from the deterministic floor
immediately (today's behavior, unchanged latency), then dispatches the chain's result as an
internal intent that applies **only if the category is still exactly the floor's suggestion** —
the user's touch always wins. Degradation is thereby graceful and honest: on an unsupported
device the second phase simply never improves on the first, and the UI never labels a
suggestion as AI-generated when it wasn't — MVP shows no engine identity at all.

**Vocabulary constraint: post-hoc validation with the `UNCATEGORIZED` floor.** The prompt
enumerates the allowed category names (from `DefaultCategories`, temperature 0, fixed `seed`,
small `maxOutputTokens`); the response is resolved by a pure function — case-insensitive match
against seeded names/ids. An unresolvable response is an engine failure per the taxonomy
above: if the model answers `"Food"` when the seeded vocabulary is Uncategorized/Groceries/
Dining/Transport/Utilities/Shopping, `GeminiNanoEngine` throws `AiEngineUnavailable` and the
chain falls through — nothing anywhere creates a category row. Resolution can only produce seeded ids, and the
floor's own honest no-match answer is `UNCATEGORIZED`, so the ADR-0003 invariant holds by
construction for any engine output whatsoever. Structured output's `enumValues` is rejected
for now: it is Alpha, costs an extra KSP processor, and is best-effort anyway (constraint
violation is a documented `finishReason`) — it would add machinery without removing the
validation obligation. Revisit when it reaches a stable API surface **and** demonstrates a
measurable reliability gain over plain-text resolution.

**`:core:ai` converts to android-library inside the implementing row** — the `genai-prompt`
dependency is an Android AAR, and that row is the point of need (the module carve-out law).
`Generation.getClient()` itself takes no `Context` — AICore resolution is internal, verified
against the beta2 AAR. Existing pure-JVM tests run unchanged; the module gains the Hilt compiler
(for `AiModule`) and the `genai-prompt` dependency (catalog bump `beta1` → `beta2`).

**Privacy invariant, enforced by construction:** receipt text never leaves the device in MVP.
The MVP binary contains no network-capable AI dependency and no class implementing a remote
call — the `CloudEngine` seam is the interface's openness, not code. It cannot be enabled
silently because there is nothing to enable: a future `CloudEngine` must arrive with its own
ADR, an explicit persisted opt-in gate, and the Entitlements machinery. Nano inference itself
stays on-device in AICore, which per its documentation isolates requests and retains nothing.

**Testing (honest boundary):** `GeminiNanoEngine` is a thin adapter with zero JVM coverage by
design — the `MlKitOcrEngine`/`CameraXCapture` trade, third instance. The testable surface is
maximized by keeping prompt construction and response resolution as pure functions (golden
tests), and the chain is fully unit-tested with fake engines under `runTest` (ordering,
fallback on `AiEngineUnavailable`/timeout/status, a non-typed exception propagating — the
programmer-error row of the taxonomy is itself a test — unresolvable-response fall-through,
download-requested-once, the confirm two-phase guard). What the implementing row's tests **cannot** prove: Nano's
actual output quality, real device capability behavior, the download lifecycle — device-only,
outside verify.sh/CI by design.

## Consequences

- Confirm's suggestions improve on Nano-capable devices with zero behavior change anywhere
  else; every device and CI keeps the deterministic floor. The differentiator is device-gated —
  the screenshots/release rows must use a supported (Pixel-9/10-class) device to show it.
- Insights (its own row) reuses the chain infrastructure; summarization arrives as a new
  capability on the boundary then — deliberately not designed now.
- Beta-SDK churn risk is contained to one adapter file plus one catalog line; no SLA on beta
  is accepted and pinned.
- Revisit triggers: structured output reaching a stable surface with a measured reliability
  gain (constrained decoding reconsidered); the second capability (insights summarization);
  any AiCore `ErrorCode` surface change at the next pin bump.

## Alternatives considered

- **Cloud-first with on-device fallback** — inverts the product's core promise; receipt text
  would leave the device by default. Rejected outright.
- **Single hardcoded engine, no interface** — device-gated availability makes a runtime choice
  mandatory: hardcoding Nano breaks every unsupported device, hardcoding rules abandons the
  differentiator. ADR-0001 already fixed this boundary.
- **Rules-only through v0.1.0** — the honest floor, and tempting for schedule; rejected
  because it defers the product's stated differentiator past release while the seam cost
  (interface + chain, both small and JVM-testable) is essentially sunk.
- **AI Edge SDK / direct AICore access** — Google positions ML Kit GenAI as the supported
  high-level surface; dropping a level buys churn and no MVP capability.
