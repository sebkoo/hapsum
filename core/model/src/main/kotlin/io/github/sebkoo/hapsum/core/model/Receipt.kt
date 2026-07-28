package io.github.sebkoo.hapsum.core.model

import java.time.LocalDate

@JvmInline
value class ReceiptId(
    val value: String,
)

/**
 * Immutable evidence captured from a photographed receipt: the image reference, the raw OCR
 * text, and the parser's per-field output — never edited after capture. Editable ledger state
 * lives on [Expense]; a receipt relates to expenses 1:N through its [lineItems].
 *
 * Each parsed header field is a [ParsedField] or null: null means the parser found nothing
 * (the future confirm screen renders it empty), LOW means found but uncertain (highlighted
 * for review), HIGH means prefill quietly.
 */
data class Receipt(
    val id: ReceiptId,
    val imageRef: String,
    val ocrText: String,
    val merchant: ParsedField<String>?,
    val purchasedAt: ParsedField<LocalDate>?,
    val total: ParsedField<Money>?,
    val lineItems: List<LineItem>,
) {
    /**
     * Coarse aggregate over the three header fields (HIGH = 1, LOW = 0.5, absent = 0) — a
     * derived sort key persisted for a future unconfirmed-receipts inbox to ORDER BY in SQL,
     * never the per-field truth the confirm screen reads.
     */
    val parseConfidence: Float
        get() =
            listOf(merchant, purchasedAt, total)
                .map { field ->
                    when (field?.confidence) {
                        ParseConfidence.HIGH -> 1f
                        ParseConfidence.LOW -> 0.5f
                        null -> 0f
                    }
                }.sum() / 3f
}
