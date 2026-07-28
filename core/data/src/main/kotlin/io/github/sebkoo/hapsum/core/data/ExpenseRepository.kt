package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>

    fun observeExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    suspend fun addExpense(expense: Expense)
}

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao,
) : ExpenseRepository {
    override fun observeExpenses(): Flow<List<Expense>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeExpensesWithCategory(): Flow<List<ExpenseWithCategory>> =
        dao.observeAllWithCategory().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addExpense(expense: Expense) {
        dao.insert(expense.toEntity())
    }
}
