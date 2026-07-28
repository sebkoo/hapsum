package io.github.sebkoo.hapsum.core.testing

import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import java.time.LocalDate

/** Synthetic receipt evidence — never a real captured image (project law). */
object ReceiptFixtures {
    fun synthetic(
        id: ReceiptId = ReceiptId("fixture-receipt-1"),
        merchant: ParsedField<String>? = ParsedField("SYNTH MART", ParseConfidence.LOW),
        purchasedAt: ParsedField<LocalDate>? = ParsedField(LocalDate.of(2026, 1, 1), ParseConfidence.HIGH),
        total: ParsedField<Money>? = ParsedField(MoneyFixtures.usd(5_50), ParseConfidence.HIGH),
        lineItems: List<LineItem> =
            listOf(
                LineItem(LineItemId("fixture-line-1"), "Milk", MoneyFixtures.usd(2_50)),
                LineItem(LineItemId("fixture-line-2"), "Bread", MoneyFixtures.usd(3_00)),
            ),
    ): Receipt =
        Receipt(
            id = id,
            imageRef = "fixture://receipt.jpg",
            ocrText = "MILK 2.50\nBREAD 3.00",
            merchant = merchant,
            purchasedAt = purchasedAt,
            total = total,
            lineItems = lineItems,
        )
}
