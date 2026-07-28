package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryTest {
    @Test
    fun `equals — same id — categories are comparable by id`() {
        assertEquals(CategoryId("groceries"), CategoryId("groceries"))
    }

    @Test
    fun `equals — same id renamed — category values differ`() {
        val original = Category(id = CategoryId("groceries"), name = "Groceries")
        val renamed = Category(id = CategoryId("groceries"), name = "Food & Groceries")

        assertNotEquals(original, renamed)
        assertEquals(original.id, renamed.id)
    }
}
