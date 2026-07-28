package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExpenseTest {
    private val usd = CurrencyCode.of("USD")

    @Test
    fun `constructor — derived from a receipt line item — links back via receiptId and lineItemId`() {
        val expense =
            Expense(
                id = ExpenseId("e1"),
                amount = Money(2_50, usd),
                categoryId = CategoryId("groceries"),
                date = LocalDate.of(2026, 7, 27),
                receiptId = ReceiptId("r1"),
                lineItemId = LineItemId("li1"),
            )

        assertEquals(ReceiptId("r1"), expense.receiptId)
        assertEquals(LineItemId("li1"), expense.lineItemId)
    }

    @Test
    fun `constructor — entered manually without a receipt — receiptId and lineItemId are null`() {
        val expense =
            Expense(
                id = ExpenseId("e2"),
                amount = Money(10_00, usd),
                categoryId = CategoryId("transport"),
                date = LocalDate.of(2026, 7, 27),
                receiptId = null,
                lineItemId = null,
            )

        assertNull(expense.receiptId)
        assertNull(expense.lineItemId)
    }
}
