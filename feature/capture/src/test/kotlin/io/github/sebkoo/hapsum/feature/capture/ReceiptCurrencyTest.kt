package io.github.sebkoo.hapsum.feature.capture

import io.github.sebkoo.hapsum.core.model.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ReceiptCurrencyTest {
    @Test
    fun `defaultReceiptCurrency — US locale — USD`() {
        assertEquals(CurrencyCode.of("USD"), defaultReceiptCurrency(Locale.US))
    }

    @Test
    fun `defaultReceiptCurrency — Korea locale — KRW`() {
        assertEquals(CurrencyCode.of("KRW"), defaultReceiptCurrency(Locale.KOREA))
    }

    @Test
    fun `defaultReceiptCurrency — language-only locale — falls back to USD`() {
        assertEquals(CurrencyCode.of("USD"), defaultReceiptCurrency(Locale.forLanguageTag("en")))
    }
}
