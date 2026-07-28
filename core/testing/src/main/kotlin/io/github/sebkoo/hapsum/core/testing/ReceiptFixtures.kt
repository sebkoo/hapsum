package io.github.sebkoo.hapsum.core.testing

import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId

/** Synthetic receipt evidence — never a real captured image (project law). */
object ReceiptFixtures {
    fun synthetic(
        id: ReceiptId = ReceiptId("fixture-receipt-1"),
        lineItems: List<LineItem> =
            listOf(
                LineItem(LineItemId("fixture-line-1"), "Milk", MoneyFixtures.usd(2_50)),
                LineItem(LineItemId("fixture-line-2"), "Bread", MoneyFixtures.usd(3_00)),
            ),
        parseConfidence: Float = 0.95f,
    ): Receipt =
        Receipt(
            id = id,
            imageRef = "fixture://receipt.jpg",
            ocrText = "MILK 2.50\nBREAD 3.00",
            parseConfidence = parseConfidence,
            lineItems = lineItems,
        )
}
