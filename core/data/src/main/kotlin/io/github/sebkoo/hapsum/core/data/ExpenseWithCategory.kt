package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.Expense

/** Domain-level ledger read model — see [ExpenseWithCategoryRow] for its Room-level shape. */
data class ExpenseWithCategory(
    val expense: Expense,
    val category: Category,
)
