package io.github.sebkoo.hapsum.core.designsystem

import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Money
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {
    @Test
    fun `format — zero-fraction currency KRW — scales 12000 minor units to 12,000, never 120,00`() {
        val money = Money(minorUnits = 12_000, currency = CurrencyCode.of("KRW"))

        val formatted = money.format(Locale.US)

        assertTrue(formatted.contains("12,000"))
        assertFalse(formatted.contains("120.00"))
    }

    @Test
    fun `format — two-fraction currency USD — scales 1250 minor units to 12,50`() {
        val money = Money(minorUnits = 1_250, currency = CurrencyCode.of("USD"))

        val formatted = money.format(Locale.US)

        assertTrue(formatted.contains("12.50"))
        assertFalse(formatted.contains("1,250.00"))
    }

    @Test
    fun `format — three-fraction currency KWD — scales 1234 minor units to 1,234`() {
        val money = Money(minorUnits = 1_234, currency = CurrencyCode.of("KWD"))

        val formatted = money.format(Locale.US)

        assertTrue(formatted.contains("1.234"))
        assertFalse(formatted.contains("1,234.00"))
    }
}
