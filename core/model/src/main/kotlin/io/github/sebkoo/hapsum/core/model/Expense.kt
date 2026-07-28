package io.github.sebkoo.hapsum.core.model

import java.time.LocalDate

@JvmInline
value class ExpenseId(
    val value: String,
)

/**
 * An editable ledger entry. Every expense originates from a [Receipt] — `receiptId` is never
 * null in MVP; manual entry without a receipt is a roadmap item that needs its own migration
 * and ADR, not a weakened invariant here (ADR-0003). `lineItemId` is null when the expense
 * represents the receipt's total rather than one specific parsed line.
 */
data class Expense(
    val id: ExpenseId,
    val amount: Money,
    val categoryId: CategoryId,
    val date: LocalDate,
    val receiptId: ReceiptId,
    val lineItemId: LineItemId?,
)
