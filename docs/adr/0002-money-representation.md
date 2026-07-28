# ADR-0002: Money representation

- Status: Accepted
- Date: 2026-07-27
- Implemented by: commit 7 (`:core:model` carve-out)

## Context

Every screen in Hapsum — capture, ledger, insights — ultimately displays and sums money.
`Expense.amount` and `LineItem.amount` are the first fields to touch this, and whatever
representation is chosen here is load-bearing everywhere downstream: Room columns (commit 8),
arithmetic in the insights aggregation (commit 16), and any future export/sync format. Changing
it later means a data migration across every persisted receipt and expense, so it must be
right before `:core:model` lands.

The two forces: money must never lose precision (a receipt total that's off by a cent is a
correctness bug, not a rounding footnote), and Hapsum is multi-currency-capable from day one —
OCR reads whatever currency symbol is on the receipt, so an amount without its currency is
meaningless.

## Decision

`Money` is a data class of two fields: `minorUnits: Long` (the amount in the currency's
smallest unit — cents for USD, won for KRW) and `currency: CurrencyCode`, a validated
`@JvmInline value class` wrapping an ISO-4217 alphabetic code. `Money` itself is a regular data
class rather than a single-field `value class` because Kotlin inline value classes (stable in
2.3.21) can only wrap one underlying value, and packing amount + currency into one `Long` (bit
field or similar) would trade a real readability and debuggability cost for a boxing saving that
doesn't matter at this app's scale. `CurrencyCode` carries the `value class` idiom instead,
since it *does* wrap exactly one primitive (a validated 3-letter string) and benefits from
avoiding a wrapper allocation.

Amounts are never represented as `Double` or `Float`: binary floating point cannot represent
decimal fractions like `0.10` exactly, so repeated arithmetic on receipt totals would drift from
the receipt's printed total. Integer minor units make every operation exact addition/subtraction
on `Long`.

`Money.plus`/`Money.minus` throw `IllegalArgumentException` on a currency mismatch rather than
silently converting — Hapsum does no currency conversion in MVP, so a mismatch is always a bug
at the call site, not a legitimate multi-currency sum.

## Consequences

- Every persisted amount is an exact integer; no rounding-mode decisions needed anywhere in the
  codebase.
- Formatting for display (inserting the decimal separator, currency symbol/placement) is pushed
  to a presentation-layer concern, not modeled here — `:core:model` has no Android/locale
  dependency to keep it a pure Kotlin JVM module with fast, non-instrumented unit tests.
- Multi-currency arithmetic (summing a USD and a KRW expense together) is a hard error today.
  If Hapsum ever needs cross-currency totals (e.g. a travel mode), that requires an explicit
  conversion step with a rate and a timestamp — deferred, not designed here.
- Any future change to this representation (e.g. adding fractional minor units for a currency
  with sub-cent pricing) touches the Room schema (commit 8) and needs a migration; this ADR is
  the record of why the simpler integer scheme was chosen first.

## Alternatives considered

- **`BigDecimal`** — exact and library-standard for money, but is a heap-allocated, non-`Long`
  type; every `Money` becomes a boxed object, and Room/Kotlin serialization needs a custom
  converter either way. Minor units keep the underlying storage a primitive `Long`.
- **Single value class packing amount and currency into one `Long`** — would satisfy "value
  class" literally, but bit-packing a 3-letter currency code into spare bits of a `Long` sacrifices
  readability and debuggability for a boxing optimization this app doesn't need at its scale.
- **`Double`/`Float` amount** — rejected outright: binary floating point cannot represent
  decimal fractions exactly, so it is unsound for money regardless of convenience.
