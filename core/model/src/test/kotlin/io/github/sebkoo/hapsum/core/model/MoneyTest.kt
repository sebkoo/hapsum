package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    private val usd = CurrencyCode.of("USD")

    @Test
    fun `plus — same currency — sums minor units`() {
        val a = Money(minorUnits = 1_00, currency = usd)
        val b = Money(minorUnits = 2_50, currency = usd)

        val result = a + b

        assertEquals(Money(minorUnits = 3_50, currency = usd), result)
    }

    @Test
    fun `plus — mismatched currency — throws`() {
        val eur = CurrencyCode.of("EUR")
        val a = Money(minorUnits = 1_00, currency = usd)
        val b = Money(minorUnits = 1_00, currency = eur)

        assertThrows(IllegalArgumentException::class.java) { a + b }
    }

    @Test
    fun `minus — same currency — subtracts minor units`() {
        val a = Money(minorUnits = 5_00, currency = usd)
        val b = Money(minorUnits = 2_00, currency = usd)

        val result = a - b

        assertEquals(Money(minorUnits = 3_00, currency = usd), result)
    }
}
