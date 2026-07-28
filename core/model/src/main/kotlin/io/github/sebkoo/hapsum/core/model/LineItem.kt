package io.github.sebkoo.hapsum.core.model

@JvmInline
value class LineItemId(
    val value: String,
)

/** One parsed row of a [Receipt] — the join between raw OCR evidence and an [Expense]. */
data class LineItem(
    val id: LineItemId,
    val description: String,
    val amount: Money,
)
