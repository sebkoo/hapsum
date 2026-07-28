package io.github.sebkoo.hapsum.core.testing

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.Expense
import io.github.sebkoo.hapsum.core.model.ExpenseId
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ReceiptId
import java.time.LocalDate

object ExpenseFixtures {
    fun synthetic(
        id: ExpenseId = ExpenseId("fixture-expense-1"),
        amount: Money = MoneyFixtures.usd(2_50),
        categoryId: CategoryId = CategoryFixtures.groceries().id,
        date: LocalDate = LocalDate.of(2026, 1, 1),
        receiptId: ReceiptId? = null,
        lineItemId: LineItemId? = null,
    ): Expense =
        Expense(
            id = id,
            amount = amount,
            categoryId = categoryId,
            date = date,
            receiptId = receiptId,
            lineItemId = lineItemId,
        )
}
