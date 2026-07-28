# ADR-0003: Room schema — categories, expenses, and the ledger read model

- Status: Accepted
- Date: 2026-07-27
- Implemented by: commit 8 (`:core:data` carve-out)

## Context

`:core:data` is the first commit to persist anything, so this is the first point where a
modeling mistake becomes expensive: once a device has real expense rows, changing how categories
and expenses relate means a migration, not a rename. Two things need deciding before the schema
is written, not after: what happens to an expense when its category goes away, and what shape
`:feature:ledger` (commit 11) will read — designing that query now, ahead of the UI, means the
feature commit consumes it rather than improvising a join under its own time pressure.

## Decision

**Categories are archived, never hard-deleted, once referenced.** `CategoryEntity` carries an
`isArchived: Boolean` column; `CategoryDao` has no `@Delete` method at all — the only removal
path is `archive()`, an `UPDATE ... SET isArchived = 1`. A reserved category is seeded into every
database at a stable, well-known id — `CategoryId.UNCATEGORIZED` (`"uncategorized"`), defined in
`:core:model` since it's domain knowledge, not a storage detail — via
`CategoryDao.insertDefaults()`, an idempotent `@Insert(onConflict = IGNORE)` call safe to run on
every app start. `ExpenseEntity.categoryId` carries a `@ForeignKey` to `CategoryEntity` with
`onDelete = RESTRICT` and `onUpdate = RESTRICT`: the database itself refuses to let a referenced
category disappear, as a second line of defense behind the DAO already not exposing a delete.
Together these mean an expense's category reference is never left dangling, and archiving a
category is always safe — nothing downstream needs to null-check or fall back at read time.

**`Expense` is the query-primary ledger table; `Receipt` is linked evidence.**
`ExpenseEntity.receiptId` is `NOT NULL` in MVP — every expense originates from a receipt, so the
ledger never has to handle "which receipt was this?" as a maybe. `lineItemId` stays nullable: an
expense can represent a receipt's total rather than one specific parsed line, but it always
traces back to *a* receipt. Manual entry without a receipt is real future scope, not a corner
case to half-support now — adding it later is a genuine schema change (nullable column + a
migration) and gets its own ADR when it's actually being built, rather than a nullable field
sitting unused from day one weakening the invariant for a feature that doesn't exist yet.

**The ledger join is designed now.** `ExpenseDao.observeExpensesWithCategory()` returns a
`@Transaction`-wrapped `@Relation` query producing `ExpenseWithCategoryRow` (`@Embedded`
`ExpenseEntity` + related `CategoryEntity`) — Room's own join mechanism, not two separately
observed flows combined by hand. `ExpenseRepository` maps this to a domain-level
`ExpenseWithCategory(expense: Expense, category: Category)`, which is what `:feature:ledger`
will consume directly for its list.

**Room3's schema JSON is exported and checked in from schema v1.** `HapsumDatabase` sets
`exportSchema = true` explicitly (not relying on the annotation default) and the module's
`build.gradle.kts` configures `room3 { schemaDirectory("$projectDir/schemas") }`; the generated
`core/data/schemas/.../1.json` is committed. Migration discipline costs nothing to turn on now,
at the first schema, versus retrofitting it once a real migration is already overdue.

## Consequences

- Every expense's category reference is always resolvable — no defensive null-handling for "the
  category this expense pointed at no longer exists" anywhere downstream, including the
  insights aggregation (commit 16).
- Deleting a category is a two-step user action in spirit (archive now, nothing to reconcile
  later) rather than a destructive one; a future "manage categories" screen can safely offer
  "archive" without a confirmation dialog explaining data loss, because there isn't any.
- `:feature:ledger` (commit 11) has its read query already shaped; it wires a `ViewModel` around
  `observeExpensesWithCategory()` rather than designing a join.
- Capture (commit 12) and the OCR parser (commit 13) must always produce a `Receipt` before an
  `Expense` — there is no code path today that can construct an `Expense` without one.
- Roadmap notes, explicitly out of scope here:
  - **Manual entry** (an `Expense` without a `Receipt`) needs a schema migration (`receiptId`
    becoming nullable) and its own ADR when it's actually being built.
  - **Per-line-item categorization.** Today, categorization happens at the `Expense` level, not
    per `LineItem` within a `Receipt`. Splitting one receipt's line items into different
    categories is deferred to whenever multi-category receipts become a real product requirement.

## Alternatives considered

- **Cascading delete from category to expense** — rejected outright: a user archiving or
  removing a category should never silently delete their spending history. That's a data-loss
  bug wearing a feature's clothes.
- **`categoryId` as a plain string column with no `@ForeignKey`** — simpler, but gives up
  referential integrity for free; an orphaned `categoryId` pointing at nothing would only surface
  as a bug report, not a build-time or query-time guarantee.
- **Building the ledger join in the `ViewModel`** via `combine()` over two independently-observed
  flows — rejected because it pushes the same join to every future consumer and reintroduces a
  window where the two flows briefly disagree (an expense flow update landing before or after the
  categories flow's), which `@Transaction` avoids entirely.
- **Nullable `receiptId` now, to leave room for manual entry** — rejected: a nullable column no
  code path ever sets is a weakened invariant, not a feature. Every reader downstream would need
  to consider a case that cannot occur, for a capability that isn't built. Adding it when manual
  entry is real work means a migration and an ADR at that point, not now.
