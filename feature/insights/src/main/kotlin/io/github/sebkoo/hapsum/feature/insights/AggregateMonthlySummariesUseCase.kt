package io.github.sebkoo.hapsum.feature.insights

import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.model.Money
import java.time.YearMonth
import javax.inject.Inject

/**
 * Feature-local domain logic (ADR-0004's named first use-case candidate): buckets the ledger's
 * joined read model into monthly summaries. Timezone-free by construction — grouping on the
 * persisted, user-confirmed [java.time.LocalDate] never touches a `Clock` or `Instant`. Currency
 * partitioning happens before any [Money.plus] fold, so a mixed-currency month can never throw
 * (triage law): it yields one [MonthlySummary] per currency instead.
 */
class AggregateMonthlySummariesUseCase
    @Inject
    constructor() {
        operator fun invoke(rows: List<ExpenseWithCategory>): List<MonthlySummary> =
            rows
                .groupBy { YearMonth.from(it.expense.date) }
                .flatMap { (month, monthRows) ->
                    monthRows
                        .groupBy { it.expense.amount.currency }
                        .map { (_, currencyRows) -> summarize(month, currencyRows) }
                }.sortedWith(
                    compareByDescending<MonthlySummary> { it.month }
                        .thenBy { it.total.currency.isoCode },
                )

        private fun summarize(
            month: YearMonth,
            rows: List<ExpenseWithCategory>,
        ): MonthlySummary {
            val categories =
                rows
                    .groupBy { it.category }
                    .map { (category, categoryRows) ->
                        CategorySummary(
                            category = category,
                            total = categoryRows.map { it.expense.amount }.reduce(Money::plus),
                        )
                    }.sortedWith(
                        compareByDescending<CategorySummary> { it.total.minorUnits }
                            .thenBy { it.category.name },
                    )
            return MonthlySummary(
                month = month,
                total = categories.map { it.total }.reduce(Money::plus),
                categories = categories,
            )
        }
    }
