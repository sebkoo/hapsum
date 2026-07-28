package io.github.sebkoo.hapsum.feature.insights

import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.MoneyFixtures
import io.github.sebkoo.hapsum.core.testing.ReducerTestHarness
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

/** Plain JVM, no Android, no coroutines — the reducer under test IS the production reducer. */
class InsightsReducerTest {
    private val harness =
        ReducerTestHarness(
            initialState = InsightsUiState(),
            reducer = InsightsViewModel.reducer,
        )

    private fun fixtureSummary(): MonthlySummary {
        val category = CategoryFixtures.groceries()
        return MonthlySummary(
            month = YearMonth.of(2026, 1),
            total = MoneyFixtures.usd(2_50),
            categories = listOf(CategorySummary(category, MoneyFixtures.usd(2_50))),
        )
    }

    @Test
    fun `reduce — summaries loaded — clears loading, sets summaries, no error`() {
        val summary = fixtureSummary()

        val state = harness.after(InsightsUiIntent.Internal.Loaded(listOf(summary)))

        assertEquals(InsightsUiState(isLoading = false, summaries = listOf(summary)), state)
    }

    @Test
    fun `reduce — load failed — sealed error replaces loading`() {
        val state = harness.after(InsightsUiIntent.Internal.LoadFailed)

        assertEquals(InsightsUiState(isLoading = false, error = InsightsError.LoadFailed), state)
    }
}
