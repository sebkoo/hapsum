package io.github.sebkoo.hapsum.core.model

@JvmInline
value class ReceiptId(
    val value: String,
)

/**
 * Immutable evidence captured from a photographed receipt: the image reference, the raw OCR
 * text, and the parser's confidence — never edited after capture. Editable ledger state lives
 * on [Expense]; a receipt relates to expenses 1:N through its [lineItems].
 */
data class Receipt(
    val id: ReceiptId,
    val imageRef: String,
    val ocrText: String,
    val parseConfidence: Float,
    val lineItems: List<LineItem>,
) {
    init {
        require(parseConfidence in 0f..1f) {
            "parseConfidence must be within 0..1, was $parseConfidence"
        }
    }
}
