# ADR-0005: Dependency injection — Hilt, retiring the manual containers

- Status: Accepted
- Date: 2026-07-28
- Implemented by: commit 15 (Hilt modules; `AppContainer` and `HapsumDataContainer` retired)

## Context

`AppContainer` said what it was from the start — its own doc comment: "Manual composition
root — holds until a DI ladder row exists. Everything here is constructor-injectable, so a
future Hilt commit replaces this file, not its clients." `HapsumDataContainer` carried the same
deal one layer down, as `:core:data`'s composition root. Both were the right call while the
graph was one repository, one `ExpenseRepository` consumer, and one Nav3 entry hand-wiring
`LedgerViewModel(repository, dispatchers)` — a container you can read top to bottom beats a
framework for a graph that small, which is the same trade ADR-0004 made for the MVI runtime.

That trade stops paying for itself at the pre-committed trigger: the second `ViewModel`
approaching. Capture (row 16) is that trigger — it adds a `CaptureViewModel`, `ReceiptRepository`
as a second injected repository, and CameraX/permission dependencies a manual container would
have to thread through `LedgerNavigation.kt`-style parameter lists at every Nav3 entry site. Rows
17–18 add an OCR parser and a rule-based categorizer behind it. Hand-wiring construction
ceremony at every entry point scales linearly with screens and dependencies; a missing wire is
also a silent human error, not a build failure, until something is threaded to test it.

## Decision

**Hilt, not Koin, not a scaled-up manual container.** `hilt = "2.59.2"` and
`alias(libs.plugins.hilt) apply false` were pinned at commit 2, foreshadowing this row.
`HapsumApplication` becomes `@HiltAndroidApp`; `MainActivity` becomes `@AndroidEntryPoint`;
`LedgerViewModel` becomes `@HiltViewModel` with an `@Inject` constructor; the Nav3 entry in
`LedgerNavigation.kt` fetches it with `hiltViewModel()`
(`androidx.hilt:hilt-navigation-compose:1.3.0`) instead of manually invoking
`viewModel { LedgerViewModel(repository, dispatchers) }`.

**`AppContainer.kt` and `HapsumDataContainer`/`DatabaseFactory.kt` are deleted outright** — no
shim, no dual path, no deprecation period. The loop commits one atomic swap; a bridge file that
outlives the commit is scope the diff doesn't need.

**Hilt modules are colocated with the layer that owns the bound type, not centralized in a new
`:core:di` module:**

- `:core:data` gains `DataModule` (`@InstallIn(SingletonComponent::class)`), providing
  `HapsumDatabase`, `ExpenseRepository`, and `CategoryRepository` — the exact three bindings
  `HapsumDataContainer` used to assemble by hand.
- `:app` gains `AppModule`, binding `DispatcherProvider` to the existing
  `DefaultDispatcherProvider` singleton and providing the one application-scoped
  `CoroutineScope` that drives category seeding at startup.

**`:core:mvi` stays Hilt-free.** A binding does not have to live in the module that defines the
type — Hilt only requires the *bound* type in the app module's transitive compile path, which
`:app` already has (`implementation(project(":core:mvi"))` since row 12). Adding the Hilt
annotation-processor dependency to the leanest, most foundational module in the graph for the
sake of one `@Provides` function would cost more (KSP overhead on every downstream build) than
it saves, and ADR-0004's "lifecycle + coroutines only" rule for this module reads more literally
with zero DI framework in it, not one binding's worth.

No new module for DI itself: CLAUDE.md carves out a module when its own code arrives, never as
an empty shell. Two `@Module` objects living beside the layers that already exist is not a body
of DI-specific code — it is two seams on existing types.

**Why Hilt over Koin:** Koin resolves at runtime — a missing binding surfaces as a crash the
first time a screen opens, not as a build failure. That gap widens exactly as the graph grows
past one `ViewModel`, which is the situation this row exists to address. Hilt is also the stack
CameraX's own lifecycle/ViewModel guidance assumes, so capture (row 16) follows a documented
integration path instead of hand-adapting Koin scopes to `ViewModelStoreOwner`.

**Why Hilt over continuing manual containers:** `AppContainer`'s own comment named this as a
stopgap with an explicit trigger ("a future Hilt commit replaces this file"); capture approaching
with a second `ViewModel` and a second repository is that trigger having arrived.

## Consequences

- KSP annotation processing (already run by `:core:data` for Room) now also runs in `:app` and
  `:feature:ledger` — every module hosting a Hilt annotation needs the `hilt-compiler` KSP
  processor, whether or not it also carries the Hilt Gradle plugin (only `:app` needs the
  plugin itself, since it compiles the `Application` class). Build time grows modestly, accepted.
- Category seeding moves from `AppContainer.init { applicationScope.launch { ... } }` to
  `HapsumApplication.onCreate()`, injecting `CategoryRepository` and the new `CoroutineScope`
  directly. Same idempotent-seed behavior (ADR-0003), new call site.
- Existing `LedgerViewModelTest` is unaffected: it constructs `LedgerViewModel` directly through
  its constructor, and `@HiltViewModel`/`@Inject` are additive annotations, not a behavior
  change. **No Hilt test infrastructure lands with this row** — `hilt-android-testing`
  (`@HiltAndroidTest`, `HiltTestRule`, a custom test `Application`) is deferred until a test
  actually needs an injected Android component under test; that is more likely an instrumented
  test than a Robolectric unit test, given `@HiltAndroidTest` requires its own test
  `Application` class.
- Every future `ViewModel` (capture, insights) follows the same `@HiltViewModel` +
  `hiltViewModel()` pattern fixed here — no per-screen decision left to make.

## Alternatives considered

- **Koin** — runtime service-locator resolution; a missing binding is a crash on screen-open,
  not a build failure. Loses ground exactly as the graph grows, which is the situation motivating
  this row.
- **Scale up `AppContainer`/`HapsumDataContainer` by hand** — was the right call at one
  `ViewModel`/one repository; `AppContainer`'s own doc comment pre-committed to replacing it
  once a second `ViewModel` approached. Capture (row 16) is that trigger.
- **A centralized `:core:di` module** — rejected under the same rule ADR-0004 module carve-outs
  follow: modules arrive with their own code, not as containers for other modules' wiring. The
  two `@Module` objects this row needs read better living beside the types they provide.
- **Anvil / kotlin-inject (compile-time, no annotation processing)** — smaller build-time
  footprint, but far less common in the CameraX/Compose ecosystem the next three rows (16–18)
  commit to; Hilt's documented integration paths for CameraX and `ViewModel` outweigh the KSP
  processing cost.
