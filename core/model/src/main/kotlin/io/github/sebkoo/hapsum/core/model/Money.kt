package io.github.sebkoo.hapsum.core.model

/**
 * An amount of money as integer minor units (cents) plus its currency — never Double/Float,
 * which cannot represent decimal amounts exactly. See ADR-0002.
 */
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode,
) {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minorUnits + other.minorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minorUnits - other.minorUnits, currency)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "cannot combine ${currency.isoCode} with ${other.currency.isoCode}"
        }
    }
}
