package io.github.sebkoo.hapsum.core.data

/** A receipt joined to its ordered line items — assembled by [ReceiptDao.getByIdWithLineItems]. */
data class ReceiptWithLineItems(
    val receipt: ReceiptEntity,
    val lineItems: List<LineItemEntity>,
)
