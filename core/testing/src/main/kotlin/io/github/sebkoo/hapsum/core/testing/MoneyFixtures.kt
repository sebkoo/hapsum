package io.github.sebkoo.hapsum.core.testing

import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Money

object MoneyFixtures {
    fun usd(minorUnits: Long = 1_00): Money = Money(minorUnits, CurrencyCode.of("USD"))
}
