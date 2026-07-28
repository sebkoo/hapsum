# ADR-0003: Room schema — categories, expenses, and the ledger read model

- Status: Accepted
- Date: 2026-07-27
- Implemented by: commit 8 (`:core:data` carve-out)

## Context

`:core:data` is the first commit to persist anything, so this is the first point where a
modeling mistake becomes expensive: once a device has real expense rows, changing how categories
and expenses relate means a migration, not a rename. Two things need deciding before the schema
is written, not after: what happens to an expense when its category goes away, and what shape
`:feature:ledger` (commit 13) will read — designing that query now, ahead of the UI, means the
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
  insights aggregation (commit 21).
- **Invariant: every `CategoryId` an `AiEngine` can emit must already exist in the database.**
  The `@ForeignKey RESTRICT` above enforces this at write time for any engine's suggestion —
  `RuleBasedEngine` (commit 18) holds it by sharing `DefaultCategories` with the startup seed;
  `GeminiNanoEngine` (commit 20) must hold it too, whether by constraining its output vocabulary
  to seeded categories or by falling back to `UNCATEGORIZED` for anything else. No engine may
  invent a category id the confirm screen (commit 19) or ledger could then fail to resolve.
- Deleting a category is a two-step user action in spirit (archive now, nothing to reconcile
  later) rather than a destructive one; a future "manage categories" screen can safely offer
  "archive" without a confirmation dialog explaining data loss, because there isn't any.
- `:feature:ledger` (commit 13) has its read query already shaped; it wires a `ViewModel` around
  `observeExpensesWithCategory()` rather than designing a join.
- Capture (commit 16) and the OCR parser (commit 17) must always produce a `Receipt` before an
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

## Amendment (2026-07-28): schema v2 — receipts table + FK

Row 14 fills the gap this ADR flagged above: `ReceiptEntity` (`id`, `imageRef`, `ocrText`,
`parseConfidence` — the domain `Receipt`'s scalar fields; `lineItems` stays unpersisted until a
`LineItemEntity` table exists, expected at the OCR parser row 17) and a `receipts` table, with
`expenses.receiptId` RESTRICTed to it. SQLite has no `ALTER TABLE ADD FOREIGN KEY`, so `expenses`
is recreated wholesale — Room's standard table-recreate migration — and `MIGRATION_1_2`'s SQL is
copied verbatim from the generated `schemas/.../2.json`, which is the entire reason exporting
schema v1 was worth doing from day one: `MigrationTestHelperTest` validates the migrated schema
against that JSON, not against hand-written SQL that could quietly drift from what Room actually
generates.

**The migration backfills one placeholder receipt per distinct pre-existing `receiptId`.** Every
v1 `expenses.receiptId` was a free-floating string with no backing row — the FK didn't exist yet.
Adding it without a backfill would make any v1 database with expense data unmigratable (the FK
would reject the copy-into-`expenses_new` step for every row whose `receiptId` isn't already in
`receipts`). The migration runs `INSERT INTO receipts SELECT DISTINCT receiptId, '', '', 0.0 FROM
expenses` before recreating `expenses`, so no pre-existing expense is dropped or orphaned. This
app has no shipped users yet, so this path is exercised by tests, not real devices — but the
migration is written as if it mattered, because eventually it will.

## Amendment (2026-07-28): schema v4 — the `expenses.lineItemId` FK

Row 19 (the confirm screen) closes the gap `ExpenseEntity`'s own KDoc had carried since schema
v1: `lineItemId` existed as a plain nullable column with no FK, because no `LineItemEntity`
table existed yet. The table arrived at schema v3 (row 17) but the FK on `expenses` was never
added — this amendment adds it: `ExpenseEntity` gains a third `ForeignKey` (RESTRICT both ways,
matching `categoryId` and `receiptId`) plus an index, `HapsumDatabase.version` moves to 4, and
`MIGRATION_3_4` recreates `expenses` wholesale (SQLite still has no `ALTER TABLE ADD FOREIGN
KEY`) — the same table-recreate shape as `MIGRATION_1_2`, `createSql` copied verbatim from the
generated `schemas/.../4.json`. No backfill is needed this time: every `lineItemId` already
written by capture (row 16) points at a real `line_items` row, so the recreate cannot orphan
anything.

**Invariant, made explicit for every future `AiEngine`:** every `CategoryId` an engine can emit
must already exist in the database. The `categoryId` FK RESTRICT above enforces this at write
time regardless of which engine suggested it — `RuleBasedEngine` (commit 18) holds it today by
sharing `DefaultCategories` with the startup seed; `GeminiNanoEngine` (commit 20) must hold it
too, whether by constraining its output vocabulary to seeded categories or by falling back to
`UNCATEGORIZED` for anything else. No engine may invent a category id the confirm screen or
ledger could then fail to resolve.

**The confirm screen writes one `Expense` per receipt, `lineItemId = null`.** Row 19 categorizes
at the receipt level, not per line item — consistent with this ADR's existing "per-line-item
categorization is deferred" roadmap note above. `lineItemId` stays populated only by a future
per-line-item write path; today's confirm write never sets it, but the FK now holds regardless
of who writes it next.

**`ReceiptDao` ships insert-only.** `ReceiptEntity` needs *some* Kotlin-level write path to be
reachable at all — without one, every FK-dependent test would drop to raw SQL instead of the
DAO, which is worse. A full read surface and a `ReceiptRepository` belong to capture (row 16),
which is what actually needs to query receipts; adding either now would be exactly the
speculative surface CLAUDE.md's conventions warn against.
