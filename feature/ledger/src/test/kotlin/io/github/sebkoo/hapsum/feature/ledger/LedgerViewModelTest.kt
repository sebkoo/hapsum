package io.github.sebkoo.hapsum.feature.ledger

import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.ExpenseFixtures
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}

class LedgerViewModelTest {
    private fun TestScope.viewModel(repository: ExpenseRepository): LedgerViewModel =
        LedgerViewModel(
            repository = repository,
            dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

    private fun fixtureRow(): ExpenseWithCategory {
        val category = CategoryFixtures.groceries()
        return ExpenseWithCategory(ExpenseFixtures.synthetic(categoryId = category.id), category)
    }

    @Test
    fun `state — before the repository emits — is loading`() =
        runTest {
            val repository =
                mockk<ExpenseRepository> {
                    every { observeExpensesWithCategory() } returns flowOf(emptyList())
                }

            val vm = viewModel(repository)

            // StandardTestDispatcher: the init collection is queued, not yet run.
            assertEquals(LedgerUiState(), vm.state.value)
            assertEquals(true, vm.state.value.isLoading)
        }

    @Test
    fun `state — repository emits rows — transitions loading to content`() =
        runTest {
            val row = fixtureRow()
            val repository =
                mockk<ExpenseRepository> {
                    every { observeExpensesWithCategory() } returns flowOf(listOf(row))
                }

            val vm = viewModel(repository)

            vm.state.test {
                assertEquals(LedgerUiState(), awaitItem())
                advanceUntilIdle()
                assertEquals(
                    LedgerUiState(isLoading = false, rows = listOf(row)),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `state — repository emits an empty ledger — content with no rows`() =
        runTest {
            val repository =
                mockk<ExpenseRepository> {
                    every { observeExpensesWithCategory() } returns flowOf(emptyList())
                }

            val vm = viewModel(repository)
            advanceUntilIdle()

            assertEquals(LedgerUiState(isLoading = false, rows = emptyList()), vm.state.value)
        }

    @Test
    fun `state — repository flow fails — sealed load-failed error, not loading`() =
        runTest {
            val repository =
                mockk<ExpenseRepository> {
                    every { observeExpensesWithCategory() } returns flow { throw IOException("disk") }
                }

            val vm = viewModel(repository)
            advanceUntilIdle()

            assertEquals(
                LedgerUiState(isLoading = false, error = LedgerError.LoadFailed),
                vm.state.value,
            )
        }
}
