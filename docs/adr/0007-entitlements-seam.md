# ADR-0007: Entitlements seam — monetization without lock-in

- Status: Accepted
- Date: 2026-07-28
- Implemented by: commit 24

## Context

Three committed statements make this decision due now, before any Pro feature exists:

- **ADR-0006 (Accepted) created a contractual dependency on an undefined term.** Its privacy
  invariant reads: a future `CloudEngine` must arrive with its own ADR, an explicit persisted
  opt-in gate, *and "the Entitlements machinery."* Until that machinery is a named type in the
  codebase, the clause is aspirational — nothing a future reviewer can check a CloudEngine PR
  against. This ADR's job is to make it concrete.
- **The README's bird's-eye view draws the `Entitlements` box dashed** — "designed, not built —
  a pure-domain boundary where a paid tier could plug in later without refactoring." v0.1.0
  ships that diagram; the claim is honest only if the design exists as an accepted ADR with a
  real boundary type behind it.
- **"Without lock-in" is product law, not marketing.** The entire current loop
  (capture→confirm→ledger→insights) stays fully functional in the free tier forever; no
  existing feature ever moves behind the gate; the user's local data is never hostage. A law
  is only reviewable if the gate has a type: once `Entitlement` is a closed enum, any future
  diff that gates a feature must add or name an enum entry — visible, greppable, reviewable.

Pro-tier candidates named in the repo so far — none implemented, none arriving with this ADR:
cloud AI opt-in (ADR-0006), CSV/PDF export, split transactions, suggestion-vs-correction
history (all backlog). MVP ships **no billing SDK, no purchase flow, no server** — seam only.

The future-compat sketch below is grounded in the rendered Play Billing documentation, not
memory (checked 2026-07-28 in a browser against
developer.android.com/google/play/billing/release-notes and …/billing/integrate):

- Current library is **9.1.0** (2026-06-18); Play requires **Billing Library 8+ for all new
  apps and updates by 2026-08-31** (extension available to 2026-11-01). Any future billing row
  pins whatever is current *then* — the deprecation treadmill is itself an argument against
  taking the dependency before there is something to sell.
- **Entitlement grants only in `PURCHASED` state, never `PENDING`**; acknowledgment must
  happen **within three days** of the `PENDING`→`PURCHASED` transition or Play refunds and
  revokes the purchase. Client-only apps without a backend use
  `BillingClient.acknowledgePurchase()` after an `isAcknowledged()` check — a documented,
  supported path.
- The docs instruct apps to **"update your entitlement storage"** and reconcile via
  `queryPurchasesAsync()` on connection/foreground — purchases can complete while the app is
  offline, on another device, or outside the app entirely. Play's model already assumes the
  app owns a local persisted grant store and that grants change at arbitrary runtime moments.

Hard to reverse because: the query shape propagates to every future gated call site (the
AiEngine-signature argument, one row later); and the enum's entries become the product's
public free/paid boundary — renaming or regating after v0.1.0 is a user-visible promise
change, not a refactor.

## Decision

**A closed enum `Entitlement` in `:core:model`; a Flow-shaped `Entitlements` read surface with
a static free-tier default in `:core:data`; zero consumers in MVP, documented as such.**

```kotlin
// :core:model — domain vocabulary, pure JVM, no new dependencies
enum class Entitlement {
    /** ADR-0006's CloudEngine precondition — the entry that makes its clause checkable. */
    CLOUD_AI,
}

// :core:data — alongside the other read surfaces
interface Entitlements {
    /** Emits the current grant and every subsequent change; never errors. */
    fun isGranted(entitlement: Entitlement): Flow<Boolean>
}

/** MVP default: everyone is free tier; nothing is granted, ever. */
object FreeTierEntitlements : Entitlements {
    override fun isGranted(entitlement: Entitlement): Flow<Boolean> = flowOf(false)
}
```

- **Closed enum, one entry.** `CLOUD_AI` is the only capability an Accepted ADR obligates
  (ADR-0006); export, split transactions, and suggestion history are backlog candidates
  without ADRs, and enumerating them now would be speculation wearing a type. New entries are
  additive and arrive with the ADR or row that implements their feature. String keys are
  rejected inside this decision (no compile-time exhaustiveness, typo-prone, stringly-typed
  domain law); a Free/Pro tier model is rejected too — call sites ask "may I use cloud AI?",
  not "is this user Pro?", and bundling capabilities into tiers is pricing strategy, which
  belongs to the billing layer later, not the domain boundary now.
- **`Flow<Boolean>`, not a synchronous val, not suspend.** Entitlements change at arbitrary
  runtime moments — the verified Play model delivers grants via `onPurchasesUpdated` while
  the app runs (a pending purchase completing, an out-of-app promo redemption) and via
  `queryPurchasesAsync` reconciliation on foreground. A synchronous val bakes in
  restart-to-unlock; a suspend answer goes stale the moment a purchase lands; revocation
  (refund, the three-day acknowledgment failure) must also propagate. Every read surface in
  this codebase is already Flow-shaped (Room flows, `StateFlow` ViewModels), so a gated
  screen `combine`s this flow like any other — the same cheap future-proofing that made
  `AiEngine.categorize` suspend in ADR-0006. The static default emits once and completes,
  which is the honest encoding of "in a build with no billing, the answer never changes."
- **Placement mirrors the Category/CategoryRepository split.** The enum joins the domain
  vocabulary in `:core:model` (pure JVM — deliberately not the interface, which would drag
  kotlinx-coroutines into a data-types-only module). The interface, default, and a
  `@Singleton` `@Provides` binding in the existing `DataModule` live in `:core:data`, which
  already owns Flow read surfaces, Hilt, and — decisive for the future — the local
  persistence layer where grants will be stored. No new module: three small files do not
  justify a carve-out (module law); the future billing implementation, an AAR dependency no
  current `:core:data` consumer should inherit, is the natural carve-out trigger.
- **Zero consumers in MVP, stated plainly.** No Pro feature exists, and inventing a gate on
  an existing free feature would violate the no-lock-in law on day one. The Hilt binding is
  wired but nothing injects it; the seam's MVP consumers are its unit tests and ADR-0006's
  clause. Why actual code when `CloudEngine` shipped as "the interface's openness, not code"?
  Because CloudEngine is a *future implementation* of an interface that already exists
  (`AiEngine`) — its seam is real, named, and testable today. Entitlements had no such type:
  the openness ADR-0006 relies on has nowhere to live until this row creates it.
- **Future-compat sketch — how Play Billing backs this seam later, none of it now.** Play
  Billing is one possible grant *provider* behind the seam, not the entitlement contract
  itself: consumers depend on `Entitlements` and never on a billing SDK, so an enterprise
  grant, a promotion, or migration grandfathering could back the same interface unchanged. A
  `PlayBillingEntitlements` (its own ADR, its own row): `launchBillingFlow` → purchase
  arrives via `onPurchasesUpdated` or `queryPurchasesAsync` reconciliation → grant only on
  `purchaseState == PURCHASED` → persist the grant set locally (DataStore — the app-owned
  "entitlement storage" the docs mandate) → `acknowledgePurchase()` within the three-day
  window → `isGranted` flows read from the local store, never from a live billing
  connection, so entitlements work offline indefinitely; reconciliation runs whenever a
  connection is available and absorbs both grants and revocations through the same Flow.
  With no server and no accounts (privacy stance), verification is client-side: MVP
  client-only entitlement storage provides offline availability but is **not** a
  server-authoritative anti-fraud system — local persistence is an availability mechanism,
  not a security one. A future server-backed verification model requires its own ADR; the
  piracy-risk trade-off is recorded here so that ADR inherits it consciously.
- **Testing.** Pure JVM throughout: enum + `object` + `flowOf`, no Android types, no time, no
  mocks. Lands one row before the Kover gate (row 25) and enters it at full coverage by
  construction.

## Consequences

- ADR-0006's CloudEngine clause becomes checkable: a CloudEngine PR must gate on
  `isGranted(Entitlement.CLOUD_AI)` (alongside its own ADR and persisted opt-in) or it is
  visibly non-compliant.
- The free/paid boundary is now a reviewable type: gating any feature requires touching the
  `Entitlement` enum in a diff, which is where the no-lock-in law gets enforced in review.
- The README's dashed box gains its referent; v0.1.0's "designed, not built" claim is true.
- A future gate on `:core:ai`'s chain wiring will need sight of `Entitlements` — either
  `:core:ai` gains a `:core:data` dependency then, or the gating moves to `:app`'s graph;
  deliberately not decided until CloudEngine's ADR needs it.
- Revisit triggers: the first real Pro feature (its row adds the enum entry and first
  consumer); the billing ADR (library version re-verified then — 8+ is already mandatory for
  new apps, 9.x current as of 2026-07-28); any decision to bundle entitlements into tiers
  (pricing, belongs to the billing layer).

## Alternatives considered

- **No seam until the first Pro feature arrives** — the strongest rival: YAGNI, and an
  interface with zero consumers cannot be validated by use. Rejected because the seam already
  has a non-code consumer: ADR-0006's Accepted text depends on "the Entitlements machinery,"
  and leaving that term undefined makes an accepted invariant unenforceable. The ladder
  pre-committed row 24 at a human phase gate (deleting it is a human ladder act, not a loop
  decision), the wrong-shape risk is minimal at one method returning `Flow<Boolean>`, and the
  cost is three pure files landing at full coverage right before the coverage gate.
- **Play Billing now** — nothing to sell (no Pro feature exists), a Play-services AAR plus
  merchant setup in an offline-first no-network MVP, and an enforced upgrade treadmill
  (verified: 8+ mandatory for new apps by 2026-08-31, 9.1.0 current) — recurring cost,
  zero shipped value.
- **Server-backed entitlement service** — violates the privacy stance (no accounts, nothing
  leaves the device) and adds operated infrastructure for zero paying users.
- **Remote-config feature flags** — a rollout/kill-switch tool, not ownership: entitlements
  are user-owned grants that must keep working offline forever (the lock-in law), while
  remote config is developer-owned, network-dependent, and revocable at whim — the exact
  opposite trust model — and would put a network SDK into the no-network MVP.
