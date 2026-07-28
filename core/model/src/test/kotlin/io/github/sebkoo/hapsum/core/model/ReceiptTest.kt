package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReceiptTest {
    private val usd = CurrencyCode.of("USD")

    private fun receipt(
        merchant: ParsedField<String>? = null,
        purchasedAt: ParsedField<LocalDate>? = null,
        total: ParsedField<Money>? = null,
        lineItems: List<LineItem> = emptyList(),
    ): Receipt =
        Receipt(
            id = ReceiptId("r1"),
            imageRef = "fixture://receipt.jpg",
            ocrText = "raw text",
            merchant = merchant,
            purchasedAt = purchasedAt,
            total = total,
            lineItems = lineItems,
        )

    @Test
    fun `parseConfidence — all three header fields HIGH — is 1`() {
        val result =
            receipt(
                merchant = ParsedField("SYNTH MART", ParseConfidence.HIGH),
                purchasedAt = ParsedField(LocalDate.of(2026, 7, 4), ParseConfidence.HIGH),
                total = ParsedField(Money(20_96, usd), ParseConfidence.HIGH),
            )

        assertEquals(1f, result.parseConfidence, 0f)
    }

    @Test
    fun `parseConfidence — HIGH total, LOW merchant, missing date — averages to a half`() {
        val result =
            receipt(
                merchant = ParsedField("SYNTH MART", ParseConfidence.LOW),
                purchasedAt = null,
                total = ParsedField(Money(20_96, usd), ParseConfidence.HIGH),
            )

        assertEquals(0.5f, result.parseConfidence, 0f)
    }

    @Test
    fun `parseConfidence — nothing parsed — is 0`() {
        assertEquals(0f, receipt().parseConfidence, 0f)
    }

    @Test
    fun `constructor — line items parsed from ocr — retained in order`() {
        val first = LineItem(id = LineItemId("li1"), description = "Milk", amount = Money(2_50, usd))
        val second = LineItem(id = LineItemId("li2"), description = "Bread", amount = Money(3_00, usd))

        val result = receipt(lineItems = listOf(first, second))

        assertEquals(listOf(first, second), result.lineItems)
    }
}
