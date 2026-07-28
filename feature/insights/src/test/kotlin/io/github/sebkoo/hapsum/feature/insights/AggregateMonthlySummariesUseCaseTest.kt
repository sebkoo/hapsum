package io.github.sebkoo.hapsum.feature.insights

import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.ExpenseFixtures
import io.github.sebkoo.hapsum.core.testing.MoneyFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Plain JVM, no Android — bucketing is pure function of the persisted date, never a Clock. */
class AggregateMonthlySummariesUseCaseTest {
    private val useCase = AggregateMonthlySummariesUseCase()
    private val groceries = CategoryFixtures.groceries()

    private fun row(
        category: Category = groceries,
        amount: Money = MoneyFixtures.usd(1_00),
        date: LocalDate = LocalDate.of(2026, 1, 1),
    ): ExpenseWithCategory =
        ExpenseWithCategory(
            expense = ExpenseFixtures.synthetic(amount = amount, categoryId = category.id, date = date),
            category = category,
        )

    @Test
    fun `invoke — expenses in two months — buckets newest-first`() {
        val january = row(date = LocalDate.of(2026, 1, 15))
        val march = row(date = LocalDate.of(2026, 3, 3))

        val summaries = useCase(listOf(january, march))

        assertEquals(listOf(YearMonth.of(2026, 3), YearMonth.of(2026, 1)), summaries.map { it.month })
    }

    @Test
    fun `invoke — two categories in one month — per-category sums in descending order`() {
        val transport = Category(CategoryId("fixture-transport"), "Transport")
        val big = row(category = groceries, amount = MoneyFixtures.usd(30_00))
        val small = row(category = transport, amount = MoneyFixtures.usd(5_00))

        val summaries = useCase(listOf(small, big))

        assertEquals(1, summaries.size)
        assertEquals(
            listOf(groceries.name, transport.name),
            summaries.single().categories.map { it.category.name },
        )
    }

    @Test
    fun `invoke — refund exceeding purchases — negative category and month totals`() {
        val purchase = row(amount = MoneyFixtures.usd(10_00))
        val refund = row(amount = MoneyFixtures.usd(-25_00))

        val summaries = useCase(listOf(purchase, refund))

        val summary = summaries.single()
        assertEquals(-15_00L, summary.total.minorUnits)
        assertEquals(
            -15_00L,
            summary.categories
                .single()
                .total.minorUnits,
        )
    }

    @Test
    fun `invoke — mixed currencies in one month — one summary per currency, never throws`() {
        val usd = row(amount = Money(10_00, CurrencyCode.of("USD")))
        val krw = row(amount = Money(12_000, CurrencyCode.of("KRW")))

        val summaries = useCase(listOf(usd, krw))

        assertEquals(2, summaries.size)
        assertTrue(summaries.all { it.month == YearMonth.of(2026, 1) })
        assertEquals(setOf("USD", "KRW"), summaries.map { it.total.currency.isoCode }.toSet())
    }

    @Test
    fun `invoke — archived category — still appears under its resolved name`() {
        val archived = Category(CategoryId("fixture-archived"), "Old Category", isArchived = true)

        val summaries = useCase(listOf(row(category = archived)))

        assertEquals(
            "Old Category",
            summaries
                .single()
                .categories
                .single()
                .category.name,
        )
    }

    @Test
    fun `invoke — no expenses — empty list`() {
        val summaries = useCase(emptyList())

        assertEquals(emptyList<MonthlySummary>(), summaries)
    }
}
