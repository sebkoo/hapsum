package io.github.sebkoo.hapsum.core.data

import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.ExpenseFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseRepositoryImplTest {
    private val dao = mockk<ExpenseDao>()
    private val repository = ExpenseRepositoryImpl(dao)

    @Test
    fun `observeExpenses — dao emits entities — maps each to a domain expense`() =
        runTest {
            val expense = ExpenseFixtures.synthetic()
            every { dao.observeAll() } returns flowOf(listOf(expense.toEntity()))

            repository.observeExpenses().test {
                assertEquals(listOf(expense), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `addExpense — delegates to dao insert with the mapped entity`() =
        runTest {
            val expense = ExpenseFixtures.synthetic()
            coEvery { dao.insert(any()) } returns Unit

            repository.addExpense(expense)

            coVerify { dao.insert(expense.toEntity()) }
        }

    @Test
    fun `observeExpensesWithCategory — dao emits joined rows — maps each to expense-with-category`() =
        runTest {
            val category = CategoryFixtures.groceries()
            val expense = ExpenseFixtures.synthetic(categoryId = category.id)
            val row =
                ExpenseWithCategoryRow(
                    expense = expense.toEntity(),
                    category =
                        CategoryEntity(
                            id = category.id.value,
                            name = category.name,
                            isArchived = category.isArchived,
                        ),
                )
            every { dao.observeAllWithCategory() } returns flowOf(listOf(row))

            repository.observeExpensesWithCategory().test {
                assertEquals(listOf(ExpenseWithCategory(expense, category)), awaitItem())
                awaitComplete()
            }
        }
}
