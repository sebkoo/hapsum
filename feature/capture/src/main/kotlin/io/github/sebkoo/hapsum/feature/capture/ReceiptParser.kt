package io.github.sebkoo.hapsum.feature.capture

import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Currency

/**
 * The parser's output: header fields plus id-less line items. Ids are assigned by the capture
 * flow at save time — id generation is effectful, and [parseReceipt] stays a pure function.
 */
data class ParsedReceipt(
    val merchant: ParsedField<String>?,
    val purchasedAt: ParsedField<LocalDate>?,
    val total: ParsedField<Money>?,
    val lineItems: List<ParsedLineItem>,
)

/** One parsed receipt row — see [ParsedReceipt] for why it carries no id yet. */
data class ParsedLineItem(
    val description: String,
    val amount: Money,
)

/**
 * Deterministic receipt parser: a pure function from OCR text structure to [ParsedReceipt] —
 * no clock, no device locale, no I/O — so the committed golden fixtures pin its behavior
 * exactly. Heuristics, and the confidence each one earns:
 * - **total** — the last line carrying a total keyword (never "subtotal") and an amount is
 *   HIGH; with no keyword line, the largest amount on any non-date line is LOW; no amounts
 *   at all, null.
 * - **date** — ISO, named-month, or a numeric date with one component over 12 is HIGH; an
 *   ambiguous numeric date reads month-first and is LOW; nothing plausible, null.
 * - **merchant** — the first line with a letter and no amount, date, or keyword on it.
 *   Always LOW: a position heuristic nothing on the receipt corroborates.
 * - **line items** — description-plus-trailing-amount lines, excluding keyword, date, and
 *   merchant lines.
 *
 * Amounts become [Money] minor units respecting [currency]'s ISO-4217 fraction digits —
 * `12,000` KRW is 12000 minor units, `12.34` USD is 1234, never a hardcoded two decimals.
 * A trailing digit group longer than the fraction digits reads as a thousands group; for
 * currencies with fraction digits an amount must show a decimal separator (which is what
 * keeps years and phone fragments out of the ledger).
 */
fun parseReceipt(
    ocrText: OcrText,
    currency: CurrencyCode,
): ParsedReceipt {
    val fractionDigits = fractionDigitsOf(currency)
    val amountRegex = amountRegex(fractionDigits)
    val rows =
        ocrText.lines
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { line -> Row(line, currency, fractionDigits, amountRegex) }

    val merchantRow =
        rows.firstOrNull { row ->
            row.text.any(Char::isLetter) && row.amount == null && !row.isKeywordRow && row.date == null
        }
    val keywordTotal =
        rows
            .lastOrNull { row -> row.isTotalKeywordRow && row.amount != null }
            ?.let { row -> ParsedField(row.amount!!, ParseConfidence.HIGH) }
    val fallbackTotal =
        rows
            .filter { row -> row.date == null }
            .mapNotNull(Row::amount)
            .maxByOrNull(Money::minorUnits)
            ?.let { amount -> ParsedField(amount, ParseConfidence.LOW) }

    val lineItems =
        rows
            .filter { row ->
                row !== merchantRow &&
                    !row.isKeywordRow &&
                    row.date == null &&
                    row.amount != null &&
                    row.description.any(Char::isLetter)
            }.map { row -> ParsedLineItem(row.description, row.amount!!) }

    return ParsedReceipt(
        merchant = merchantRow?.let { row -> ParsedField(row.text, ParseConfidence.LOW) },
        purchasedAt = rows.firstNotNullOfOrNull(Row::date),
        total = keywordTotal ?: fallbackTotal,
        lineItems = lineItems,
    )
}

/** One trimmed, non-empty OCR line and everything the heuristics read off it. */
private class Row(
    val text: String,
    currency: CurrencyCode,
    fractionDigits: Int,
    amountRegex: Regex,
) {
    private val isSubtotalRow = SUBTOTAL_KEYWORD.containsMatchIn(text)
    val isTotalKeywordRow = !isSubtotalRow && TOTAL_KEYWORDS.containsMatchIn(text)
    val isKeywordRow = isSubtotalRow || isTotalKeywordRow || NOISE_KEYWORDS.containsMatchIn(text)
    val date: ParsedField<LocalDate>? = parseDate(text)

    private val amountMatch = amountRegex.findAll(text).lastOrNull()
    val amount: Money? =
        amountMatch?.let { match ->
            toMinorUnits(match.value, fractionDigits)?.let { minorUnits -> Money(minorUnits, currency) }
        }
    val description: String =
        amountMatch?.let { match -> text.take(match.range.first).trimEnd(' ', '.', ':', '-', '·', '…') } ?: ""
}

private val TOTAL_KEYWORDS =
    Regex("""\b(?:grand\s+total|total(?:\s+due)?|amount\s+due|balance\s+due)\b""", RegexOption.IGNORE_CASE)
private val SUBTOTAL_KEYWORD = Regex("""\bsub\s*total\b""", RegexOption.IGNORE_CASE)
private val NOISE_KEYWORDS =
    Regex(
        """\b(?:tax|vat|tip|change|cash|card|visa|mastercard|tel|phone|receipt|invoice|thank(?:s|\s+you)?)\b""",
        RegexOption.IGNORE_CASE,
    )

/**
 * For zero-fraction currencies any digit run (optionally thousands-grouped) is an amount; for
 * the rest an amount must carry a decimal separator followed by at most the currency's
 * fraction digits. When both `.` and `,` appear, the last one is the decimal separator.
 */
private fun amountRegex(fractionDigits: Int): Regex =
    if (fractionDigits == 0) {
        Regex("""(?<![\d.,])(?:\d{1,3}(?:[.,\s]\d{3})+|\d+)(?!\d)""")
    } else {
        Regex("""(?<![\d.,])(?:\d{1,3}(?:[.,\s]\d{3})*|\d+)[.,]\d{1,$fractionDigits}(?![\d%])""")
    }

private fun toMinorUnits(
    token: String,
    fractionDigits: Int,
): Long? {
    val compact = token.filterNot(Char::isWhitespace)
    if (fractionDigits == 0) return compact.filter(Char::isDigit).toLongOrNull()
    var scale = 1L
    repeat(fractionDigits) { scale *= 10 }
    val separatorIndex = compact.lastIndexOfAny(charArrayOf('.', ','))
    if (separatorIndex < 0) return compact.toLongOrNull()?.let { whole -> whole * scale }
    val whole = compact.take(separatorIndex).filter(Char::isDigit).ifEmpty { "0" }
    val fraction = compact.drop(separatorIndex + 1).padEnd(fractionDigits, '0')
    val wholeUnits = whole.toLongOrNull() ?: return null
    return wholeUnits * scale + fraction.toLong()
}

/** ISO-4217 fraction digits; an ISO-shaped code this JVM doesn't know reads as 2, the dominant case. */
private fun fractionDigitsOf(currency: CurrencyCode): Int =
    try {
        Currency.getInstance(currency.isoCode).defaultFractionDigits.coerceAtLeast(0)
    } catch (_: IllegalArgumentException) {
        2
    }

private val ISO_DATE = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
private val NUMERIC_DATE = Regex("""\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})\b""")
private val MONTH_NAMES = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
private val MONTH_PATTERN = MONTH_NAMES.joinToString(separator = "|")
private val MONTH_DAY_YEAR =
    Regex("""\b(${MONTH_PATTERN})[a-z]*\.?\s+(\d{1,2}),?\s+(\d{4})\b""", RegexOption.IGNORE_CASE)
private val DAY_MONTH_YEAR =
    Regex("""\b(\d{1,2})\s+(${MONTH_PATTERN})[a-z]*\.?,?\s+(\d{4})\b""", RegexOption.IGNORE_CASE)

private fun parseDate(text: String): ParsedField<LocalDate>? {
    ISO_DATE.find(text)?.let { match ->
        val (year, month, day) = match.destructured
        localDateOrNull(year.toInt(), month.toInt(), day.toInt())
            ?.let { date -> return ParsedField(date, ParseConfidence.HIGH) }
    }
    MONTH_DAY_YEAR.find(text)?.let { match ->
        val (month, day, year) = match.destructured
        localDateOrNull(year.toInt(), monthNumber(month), day.toInt())
            ?.let { date -> return ParsedField(date, ParseConfidence.HIGH) }
    }
    DAY_MONTH_YEAR.find(text)?.let { match ->
        val (day, month, year) = match.destructured
        localDateOrNull(year.toInt(), monthNumber(month), day.toInt())
            ?.let { date -> return ParsedField(date, ParseConfidence.HIGH) }
    }
    NUMERIC_DATE.find(text)?.let { match ->
        val (first, second, rawYear) = match.destructured
        val a = first.toInt()
        val b = second.toInt()
        val year = rawYear.toInt().let { if (it < 100) 2000 + it else it }
        val candidate =
            when {
                a > 12 -> localDateOrNull(year, b, a)?.let { ParsedField(it, ParseConfidence.HIGH) }
                b > 12 -> localDateOrNull(year, a, b)?.let { ParsedField(it, ParseConfidence.HIGH) }
                else -> localDateOrNull(year, a, b)?.let { ParsedField(it, ParseConfidence.LOW) }
            }
        if (candidate != null) return candidate
    }
    return null
}

private fun localDateOrNull(
    year: Int,
    month: Int,
    day: Int,
): LocalDate? =
    try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        null
    }

private fun monthNumber(name: String): Int = MONTH_NAMES.indexOf(name.lowercase().take(3)) + 1
