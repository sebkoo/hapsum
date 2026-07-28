package io.github.sebkoo.hapsum.core.designsystem

import io.github.sebkoo.hapsum.core.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Locale-aware Money display — the presentation-layer concern ADR-0002 deliberately keeps out
 * of `:core:model`. Fraction digits come from ISO-4217 (`Currency.defaultFractionDigits`), not
 * a hardcoded two decimals, so a zero-fraction currency (KRW) or three-fraction currency (KWD)
 * scales correctly; symbol position and grouping are left to [NumberFormat] per [locale].
 */
fun Money.format(locale: Locale): String {
    val isoCurrency = Currency.getInstance(currency.isoCode)
    val fractionDigits = isoCurrency.defaultFractionDigits
    val scale = BigDecimal.TEN.pow(fractionDigits)
    val amount = BigDecimal(minorUnits).divide(scale)
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = isoCurrency
    // NumberFormat.setCurrency() does not itself widen the fraction-digit bounds (they stay
    // at the format's own locale default of 2), so a 3-fraction currency like KWD would
    // silently round its last digit away without this — set explicitly per ISO-4217.
    formatter.minimumFractionDigits = fractionDigits
    formatter.maximumFractionDigits = fractionDigits
    return formatter.format(amount)
}
