package io.github.sebkoo.hapsum.core.model

/**
 * ISO-4217 alphabetic currency code (e.g. "USD", "KRW"). Validated at construction because
 * receipts arrive from OCR — untrusted input — rather than being trusted internal state.
 */
@JvmInline
value class CurrencyCode private constructor(
    val isoCode: String,
) {
    companion object {
        private val ISO_4217_ALPHA = Regex("[A-Z]{3}")

        fun of(isoCode: String): CurrencyCode {
            require(ISO_4217_ALPHA.matches(isoCode)) {
                "currency code must be 3 uppercase letters (ISO-4217), was \"$isoCode\""
            }
            return CurrencyCode(isoCode)
        }
    }
}
