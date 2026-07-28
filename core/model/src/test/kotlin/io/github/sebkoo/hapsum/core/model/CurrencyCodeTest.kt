package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrencyCodeTest {
    @Test
    fun `of — valid three-letter uppercase code — creates instance`() {
        val currency = CurrencyCode.of("USD")

        assertEquals("USD", currency.isoCode)
    }

    @Test
    fun `of — lowercase code — throws`() {
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode.of("usd") }
    }

    @Test
    fun `of — wrong length — throws`() {
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode.of("US") }
    }
}
