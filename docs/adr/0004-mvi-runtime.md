# ADR-0004: MVI runtime — hand-rolled, in `:core:mvi`

- Status: Accepted
- Date: 2026-07-27
- Implemented by: commit 12 (`:core:mvi` + `ReducerTestHarness`)

## Context

Every screen from row 13 onward builds on this runtime, and the MVI contract in CLAUDE.md is
project law: sealed state/intent/effect per screen, a single `onIntent` entry, pure reduction,
one state-mutation path, one-shot effects. Whatever enforces that contract is the hardest thing
in the app to swap later — every feature ViewModel inherits from it. The runtime exists to make
the contract cheap to follow, not to reinterpret it.

## Decision

**Hand-rolled, roughly 100 lines, no MVI library.** The entire need is one base class:
state holder, effect channel, intent pipeline. A library outsources the project's central
discipline to a third-party vocabulary and adds a version-pin liability for less code than a
single screen. See Alternatives for the specific rejections.

**Effects are a `Channel(BUFFERED)` exposed via `receiveAsFlow()`.** The rejected default,
`MutableSharedFlow(replay = 0)`, silently drops emissions when no subscriber is attached —
which is precisely the config-change window where one-shot navigation effects are emitted.
`replay > 0` fails the opposite way: it re-delivers consumed effects to the next collector.
The channel buffers across collector gaps and hands each effect to exactly one receiver.
Three hardenings, each closing a reviewed failure mode:

- `sendEffect` is `trySend`-first: the non-overflow path completes synchronously on the calling
  thread — no dispatcher hop to lose at ViewModel teardown, no `Dispatchers.setMain` needed in
  tests. Only a full buffer (64 undelivered effects, a runaway emitter) falls back to a
  suspending send in `viewModelScope`. The fallback is delayed delivery, not loss: senders
  suspended past the buffer resume FIFO as the collector drains — proven by the overflow test
  in `MviViewModelTest` (70 collector-less emissions, all delivered in order). Effects still
  undelivered at ViewModel clear die with it, but that end-of-life applies identically to the
  64 buffered ones — it is the UI-lifetime scope of effects (see Consequences), not an
  overflow-specific hole.
- `onUndeliveredElement` re-queues the one in-flight effect a collector cancelled mid-handoff
  would otherwise eat (the backgrounding race). A re-queued effect re-enters at the tail and can
  arrive after later effects under a burst — accepted: reorder beats silent loss for one-shots.
- `effects` fails fast on a second concurrent collector. `receiveAsFlow` fan-out splits
  delivery nondeterministically between collectors; the guard turns "navigation sometimes
  silently does nothing" into an immediate `IllegalStateException`. The sanctioned collection
  idiom is one collector under `repeatOnLifecycle(STARTED)`; a Compose helper lands with the
  first effect-emitting screen — the ledger (row 13) turned out to emit none, so capture
  (row 16) is the expected first.

**Intents run through a drain loop, and async results re-enter as `InternalUiIntent`s.**
`onIntent` is the single public entry; it rejects `InternalUiIntent` variants, which only the
ViewModel itself may feed back through protected `dispatch()` — result intents live inside the
screen's public sealed intent type (law-compliant) but cannot be forged from the UI. The drain
loop applies intents strictly in arrival order: a `dispatch` made during `react` queues behind
the current intent instead of recursing (reentrancy would otherwise reorder intents, re-run
reducers through CAS retries, and overflow the stack on deep cascades — all silently).
`updateAndGet` pins `react`'s state argument to exactly the state its own intent installed.

**Reducer identity is a convention, enforced in review.** The reducer is a plain `(S, I) -> S`
constructor parameter; each screen defines it as a companion-level function reference, and both
the ViewModel super-call and the screen's `ReducerTestHarness` reference that same value — the
function under test is the production function by construction. Lambda literals in the
super-call are banned in review: they invite capturing constructor dependencies, and purity is
load-bearing (the CAS loop may re-invoke the reducer on contention). `UiState`s are immutable —
`val`-only, immutable collections — or state diffing and every harness trajectory silently lie.

**No blanket UseCase layer.** ViewModels talk to repositories directly. A use case is
introduced only where domain logic exists beyond pass-through; the first expected candidate is
monthly insight aggregation (row 20). "Where are your use cases?" — here, when they earn their
file.

**Navigation is an Effect.** Screens emit navigation as one-shot effects; features export
their Nav3 entries; `:app` owns the back stack and assembles the graph, mapping each feature's
effects onto other features' exported entries — so no feature ever depends on another feature.
Contract fixed now; wiring lands with the first screen (row 13).

**Error state is a sealed type, never a raw String.** Each screen's error vocabulary lives in
its own `XUiState`; shared failure causes belong to `:core:model`/`:core:data` and are mapped
into feature errors inside reducers. `:core:mvi` never learns what an error is.

**Module: `:core:mvi`, generic infrastructure only.** Depends on `androidx.lifecycle` +
coroutines, never on `:core:model` or any domain type — the runtime is parameterized over
`<State, Intent, Effect>`. Not in `:core:designsystem`: one concern per module (tokens/theme is
not runtime plumbing). Not in `:core:model`: that module is pure Kotlin with no Android
dependency, and `MviViewModel` needs `androidx.lifecycle.ViewModel`. The CLAUDE.md module map
gains `:core:mvi` (approved at this row's gate). `ReducerTestHarness` stays in the pure-JVM
`:core:testing` — reducers are testable with no Android and no coroutines, which is the point.

## Consequences

- Runtime tests are plain JVM: no Robolectric, and — because the effect path is synchronous in
  the non-overflow case — no `Dispatchers.setMain`, with one deliberate exception: the
  overflow-path proof installs a test Main dispatcher to drive the suspending fallback.
  State-only feature tests need no coroutine machinery at all.
- Row-13 pattern, fixed now: repository observation starts in `init`/`viewModelScope`, results
  come back as nested `Internal` intent variants via `dispatch`, loading→content→error is a
  pure reduction. Feature tests drive internal intents through their real sources (a MockK'd
  repository flow), never by calling them directly. `DispatcherProvider` (in `:core:mvi` — a
  pure-coroutines abstraction, inside this module's dependency rule) landed at row 13 by
  decision, injected into the ledger ViewModel; its test double stays feature-local until a
  second feature needs it, because pure-JVM `:core:testing` cannot depend on this Android
  library — sharing it is the trigger to revisit that boundary. A shared `MainDispatcherRule`
  waits for the first test that actually dispatches to Main: neither row 12's (trySend-first
  effects) nor row 13's (dispatcher-injected collection) ever touch it.
- Effects are UI-lifetime-scoped, fire-and-forget: anything that must survive the screen
  belongs in the data layer or in state, never in an effect.
- Process death is out of MVP scope: ledger state is re-derivable from Room; `initialState`
  being a constructor parameter keeps the later SavedStateHandle retrofit non-breaking
  (restore via `initialState = restoreFrom(handle)`, save via one optional hook). `:app`'s
  Nav3 back-stack keys must be saveable from day one regardless.
- The sequencing gap this ADR's review surfaced — capture's "navigation effect carrying the
  new receipt id" had no receipts table behind it — was approved at the row-12 gate as ladder
  row 14: `ReceiptEntity`, FK RESTRICT from `expenses.receiptId`, and the v1→v2 table-recreate
  migration land there, before the capture screen (row 16).

## Alternatives considered

- **Orbit MVI** — container DSL hides the reduce step inside `intent {}` blocks; the contract
  this project treats as law becomes a library convention, for less code than the runtime.
- **MVIKotlin** — Store/Executor/Bootstrapper architecture with its own lifecycle vocabulary;
  heavyweight indirection for one base class, and the pedagogy of the git history would narrate
  someone else's design.
- **Circuit** — collapses presenter and UI into its own composition model with opinionated
  navigation; conflicts directly with Nav3-in-`:app` and navigation-as-effect.
- **`MutableSharedFlow(replay = 0)` for effects** — drops emissions with no subscriber
  attached; fails the exact rotation-gap case the channel is chosen for.
- **A `Reducer` fun-interface + `:core:mvi` as a pure-JVM module** — would let the harness
  share a named reducer type; rejected for now because the bare stdlib function type cannot
  drift between modules, and `androidx.lifecycle`'s JVM-variant resolution is unverified on
  this dependency set. Revisit only if the convention proves too weak in review.
- **Abstract `reduce` method on the ViewModel** — puts the reducer where the harness cannot
  reach it without instantiating an Android class; breaks plain-JVM reducer testing.
- **A blanket UseCase-per-interaction layer** — pass-through ceremony between ViewModel and
  repository; rejected until real domain logic exists (row 20 aggregation is the first
  candidate).
