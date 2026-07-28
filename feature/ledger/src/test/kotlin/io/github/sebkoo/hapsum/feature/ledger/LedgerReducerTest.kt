package io.github.sebkoo.hapsum.feature.ledger

import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.ExpenseFixtures
import io.github.sebkoo.hapsum.core.testing.ReducerTestHarness
import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JVM, no Android, no coroutines — the reducer under test IS the production reducer. */
class LedgerReducerTest {
    private val harness =
        ReducerTestHarness(
            initialState = LedgerUiState(),
            reducer = LedgerViewModel.reducer,
        )

    private fun fixtureRow(): ExpenseWithCategory {
        val category = CategoryFixtures.groceries()
        return ExpenseWithCategory(ExpenseFixtures.synthetic(categoryId = category.id), category)
    }

    @Test
    fun `reduce — rows loaded — clears loading, sets rows, no error`() {
        val row = fixtureRow()

        val state = harness.after(LedgerUiIntent.Internal.Loaded(listOf(row)))

        assertEquals(LedgerUiState(isLoading = false, rows = listOf(row)), state)
    }

    @Test
    fun `reduce — load failed — sealed error replaces loading`() {
        val state = harness.after(LedgerUiIntent.Internal.LoadFailed)

        assertEquals(LedgerUiState(isLoading = false, error = LedgerError.LoadFailed), state)
    }

    @Test
    fun `reduce — failure then successful reload — error clears when rows arrive`() {
        val row = fixtureRow()

        val trajectory =
            harness.trajectory(
                LedgerUiIntent.Internal.LoadFailed,
                LedgerUiIntent.Internal.Loaded(listOf(row)),
            )

        assertEquals(
            listOf(
                LedgerUiState(),
                LedgerUiState(isLoading = false, error = LedgerError.LoadFailed),
                LedgerUiState(isLoading = false, rows = listOf(row)),
            ),
            trajectory,
        )
    }
}
