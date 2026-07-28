package io.github.sebkoo.hapsum.feature.capture

import io.github.sebkoo.hapsum.core.model.CurrencyCode
import java.util.Currency
import java.util.Locale

/**
 * Hands [CaptureViewModel] the app-default currency at the moment a receipt is created. An
 * interface rather than an injected [CurrencyCode] for two reasons: Dagger cannot provide a
 * `@JvmInline` value class (its provider's JVM name gets mangled), and resolving per capture —
 * not per ViewModel — is what "derived from device locale at receipt creation" literally means.
 */
fun interface ReceiptCurrencyResolver {
    fun resolve(): CurrencyCode
}

/**
 * The single app-default currency every parsed amount is denominated in, derived from the
 * device locale at receipt creation. Detecting the currency from the OCR text itself is a
 * recorded roadmap note (docs/PROGRESS.md), not silently absent. Falls back to USD when the
 * locale names no country or the country has no currency (Antarctica-class edge cases).
 */
fun defaultReceiptCurrency(locale: Locale = Locale.getDefault()): CurrencyCode =
    runCatching { Currency.getInstance(locale)?.currencyCode }
        .getOrNull()
        ?.let(CurrencyCode::of)
        ?: CurrencyCode.of("USD")
