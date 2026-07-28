package io.github.sebkoo.hapsum.feature.capture

import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Golden tests: each committed synthetic OCR-text fixture pins the parser's complete output.
 * Never image→parse end-to-end — that would test ML Kit's model version, not our determinism.
 */
class ReceiptParserTest {
    private val usd = CurrencyCode.of("USD")
    private val eur = CurrencyCode.of("EUR")
    private val krw = CurrencyCode.of("KRW")

    private fun fixture(name: String): OcrText {
        val resource = checkNotNull(javaClass.classLoader?.getResource("ocr/$name")) { "missing fixture ocr/$name" }
        return OcrText(resource.readText().lines())
    }

    @Test
    fun `parse — cafe fixture — keyword total HIGH, ambiguous numeric date LOW, noise lines excluded`() {
        val result = parseReceipt(fixture("cafe-usd.txt"), usd)

        assertEquals(
            ParsedReceipt(
                merchant = ParsedField("MAPLE STREET CAFE", ParseConfidence.LOW),
                purchasedAt = ParsedField(LocalDate.of(2026, 7, 4), ParseConfidence.LOW),
                total = ParsedField(Money(20_96, usd), ParseConfidence.HIGH),
                lineItems =
                    listOf(
                        ParsedLineItem("Flat White", Money(4_50, usd)),
                        ParsedLineItem("Blueberry Muffin", Money(3_75, usd)),
                        ParsedLineItem("Avocado Toast", Money(11_00, usd)),
                    ),
            ),
            result,
        )
    }

    @Test
    fun `parse — market fixture — comma decimals, dotted day-first date unambiguous so HIGH`() {
        val result = parseReceipt(fixture("market-eur.txt"), eur)

        assertEquals(
            ParsedReceipt(
                merchant = ParsedField("SYNTH MARKT", ParseConfidence.LOW),
                purchasedAt = ParsedField(LocalDate.of(2026, 7, 15), ParseConfidence.HIGH),
                total = ParsedField(Money(8_67, eur), ParseConfidence.HIGH),
                lineItems =
                    listOf(
                        ParsedLineItem("Brot", Money(2_49, eur)),
                        ParsedLineItem("Milch", Money(1_19, eur)),
                        ParsedLineItem("Kaese", Money(4_99, eur)),
                    ),
            ),
            result,
        )
    }

    @Test
    fun `parse — mart fixture — zero-fraction currency, thousands separators are not decimals`() {
        val result = parseReceipt(fixture("mart-krw.txt"), krw)

        assertEquals(
            ParsedReceipt(
                merchant = ParsedField("SYNTH SEOUL MART", ParseConfidence.LOW),
                purchasedAt = ParsedField(LocalDate.of(2026, 7, 15), ParseConfidence.HIGH),
                total = ParsedField(Money(7_500, krw), ParseConfidence.HIGH),
                lineItems =
                    listOf(
                        ParsedLineItem("Kimbap", Money(3_500, krw)),
                        ParsedLineItem("Ramyeon", Money(4_000, krw)),
                    ),
            ),
            result,
        )
    }

    @Test
    fun `parse — deli fixture — no total keyword, largest amount falls back at LOW`() {
        val result = parseReceipt(fixture("deli-no-total-keyword.txt"), usd)

        assertEquals(
            ParsedReceipt(
                merchant = ParsedField("CORNER DELI", ParseConfidence.LOW),
                purchasedAt = null,
                total = ParsedField(Money(10_35, usd), ParseConfidence.LOW),
                lineItems =
                    listOf(
                        ParsedLineItem("Sandwich", Money(8_25, usd)),
                        ParsedLineItem("Chips", Money(2_10, usd)),
                    ),
            ),
            result,
        )
    }

    @Test
    fun `parse — unparseable fixture — every field null, no line items`() {
        val result = parseReceipt(fixture("unparseable.txt"), usd)

        assertEquals(ParsedReceipt(merchant = null, purchasedAt = null, total = null, lineItems = emptyList()), result)
    }

    @Test
    fun `parse — lowercase total keyword — still HIGH`() {
        val result = parseReceipt(OcrText(listOf("total 5.00")), usd)

        assertEquals(ParsedField(Money(5_00, usd), ParseConfidence.HIGH), result.total)
    }

    @Test
    fun `parse — subtotal only — never a keyword total, falls back at LOW`() {
        val result = parseReceipt(OcrText(listOf("SUBTOTAL 10.00")), usd)

        assertEquals(ParsedField(Money(10_00, usd), ParseConfidence.LOW), result.total)
    }

    @Test
    fun `parse — total then grand total — the last keyword line wins`() {
        val result = parseReceipt(OcrText(listOf("TOTAL 9.99", "GRAND TOTAL 12.00")), usd)

        assertEquals(ParsedField(Money(12_00, usd), ParseConfidence.HIGH), result.total)
    }

    @Test
    fun `parse — month-name date — HIGH`() {
        val result = parseReceipt(OcrText(listOf("Jul 4, 2026")), usd)

        assertEquals(ParsedField(LocalDate.of(2026, 7, 4), ParseConfidence.HIGH), result.purchasedAt)
    }

    @Test
    fun `parse — numeric date with day over twelve — unambiguous, HIGH`() {
        val result = parseReceipt(OcrText(listOf("25/12/2026")), usd)

        assertEquals(ParsedField(LocalDate.of(2026, 12, 25), ParseConfidence.HIGH), result.purchasedAt)
    }

    @Test
    fun `parse — JVM-unknown currency code — reads two fraction digits`() {
        val zzz = CurrencyCode.of("ZZZ")

        val result = parseReceipt(OcrText(listOf("TOTAL 5.00")), zzz)

        assertEquals(ParsedField(Money(5_00, zzz), ParseConfidence.HIGH), result.total)
    }
}
