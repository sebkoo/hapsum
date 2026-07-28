package io.github.sebkoo.hapsum.feature.insights

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

class InsightsViewModelTest {
    private fun TestScope.viewModel(repository: ExpenseRepository): InsightsViewModel =
        InsightsViewModel(
            repository = repository,
            aggregate = AggregateMonthlySummariesUseCase(),
            dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

    private fun fixtureRow(): ExpenseWithCategory {
        val category = CategoryFixtures.groceries()
        return ExpenseWithCategory(ExpenseFixtures.synthetic(categoryId = category.id), category)
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
                assertEquals(InsightsUiState(), awaitItem())
                advanceUntilIdle()
                val loaded = awaitItem()
                assertEquals(false, loaded.isLoading)
                assertEquals(1, loaded.summaries.size)
                assertEquals(null, loaded.error)
            }
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
                InsightsUiState(isLoading = false, error = InsightsError.LoadFailed),
                vm.state.value,
            )
        }
}
