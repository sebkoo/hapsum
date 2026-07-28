package io.github.sebkoo.hapsum.core.model

import java.time.LocalDate

@JvmInline
value class ExpenseId(
    val value: String,
)

/**
 * An editable ledger entry. May originate from a [Receipt] line item (both ids set) or be
 * entered manually (both null) — [Receipt] stays immutable evidence either way.
 */
data class Expense(
    val id: ExpenseId,
    val amount: Money,
    val categoryId: CategoryId,
    val date: LocalDate,
    val receiptId: ReceiptId?,
    val lineItemId: LineItemId?,
)
