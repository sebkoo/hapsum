package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReceiptTest {
    private val usd = CurrencyCode.of("USD")

    @Test
    fun `constructor — parseConfidence outside 0 to 1 — throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Receipt(
                id = ReceiptId("r1"),
                imageRef = "file://receipt.jpg",
                ocrText = "raw text",
                parseConfidence = 1.5f,
                lineItems = emptyList(),
            )
        }
    }

    @Test
    fun `constructor — line items parsed from ocr — retained in order`() {
        val first = LineItem(id = LineItemId("li1"), description = "Milk", amount = Money(2_50, usd))
        val second = LineItem(id = LineItemId("li2"), description = "Bread", amount = Money(3_00, usd))

        val receipt =
            Receipt(
                id = ReceiptId("r1"),
                imageRef = "file://receipt.jpg",
                ocrText = "raw text",
                parseConfidence = 0.92f,
                lineItems = listOf(first, second),
            )

        assertEquals(listOf(first, second), receipt.lineItems)
    }
}
