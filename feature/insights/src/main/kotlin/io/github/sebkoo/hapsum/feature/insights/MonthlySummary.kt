package io.github.sebkoo.hapsum.feature.insights

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.Money
import java.time.YearMonth

/** One calendar month's aggregated spend, partitioned by currency — see [AggregateMonthlySummariesUseCase]. */
data class MonthlySummary(
    val month: YearMonth,
    val total: Money,
    val categories: List<CategorySummary>,
)

data class CategorySummary(
    val category: Category,
    val total: Money,
)
