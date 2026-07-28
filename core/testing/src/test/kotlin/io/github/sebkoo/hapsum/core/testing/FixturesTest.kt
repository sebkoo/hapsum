package io.github.sebkoo.hapsum.core.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixturesTest {
    @Test
    fun `receipt synthetic — default — has synthetic line items and valid confidence`() {
        val receipt = ReceiptFixtures.synthetic()

        assertTrue(receipt.lineItems.isNotEmpty())
        assertTrue(receipt.parseConfidence in 0f..1f)
    }

    @Test
    fun `expense synthetic — default — categorized under the groceries fixture`() {
        val expense = ExpenseFixtures.synthetic()

        assertEquals(CategoryFixtures.groceries().id, expense.categoryId)
    }
}
