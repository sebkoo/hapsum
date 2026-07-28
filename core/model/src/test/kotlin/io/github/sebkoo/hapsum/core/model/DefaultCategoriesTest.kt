package io.github.sebkoo.hapsum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {
    @Test
    fun `all — ids are unique`() {
        val ids = DefaultCategories.all.map(Category::id)

        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `all — starts with the reserved uncategorized fallback`() {
        assertEquals(CategoryId.UNCATEGORIZED, DefaultCategories.all.first().id)
    }

    @Test
    fun `all — nothing is archived by default`() {
        assertTrue(DefaultCategories.all.none(Category::isArchived))
    }
}
