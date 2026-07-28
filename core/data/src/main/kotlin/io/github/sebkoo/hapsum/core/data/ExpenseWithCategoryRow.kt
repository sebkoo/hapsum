package io.github.sebkoo.hapsum.core.data

import androidx.room3.Embedded
import androidx.room3.Relation

/**
 * The ledger list's read shape, designed now so `:feature:ledger` (commit 11) consumes it
 * directly instead of joining two independently-observed flows itself (ADR-0003).
 */
data class ExpenseWithCategoryRow(
    @Embedded val expense: ExpenseEntity,
    @Relation(entity = CategoryEntity::class, parentColumns = ["categoryId"], entityColumns = ["id"])
    val category: CategoryEntity,
)
