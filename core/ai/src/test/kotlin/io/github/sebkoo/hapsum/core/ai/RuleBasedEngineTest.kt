package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedEngineTest {
    private val engine = RuleBasedEngine()

    @Test
    fun `categorize — grocery keyword in a line-item description — groceries`() {
        assertEquals(DefaultCategories.GROCERIES.id, engine.categorize("Whole Milk 1L"))
    }

    @Test
    fun `categorize — uppercase merchant text — case-insensitive dining hit`() {
        assertEquals(DefaultCategories.DINING.id, engine.categorize("MAPLE STREET CAFE"))
    }

    @Test
    fun `categorize — keyword embedded in a longer word — no hit, uncategorized`() {
        assertEquals(CategoryId.UNCATEGORIZED, engine.categorize("marketplace listing fee"))
    }

    @Test
    fun `categorize — no keyword matches — uncategorized`() {
        assertEquals(CategoryId.UNCATEGORIZED, engine.categorize("miscellaneous adjustment"))
    }

    @Test
    fun `categorize — empty text — uncategorized`() {
        assertEquals(CategoryId.UNCATEGORIZED, engine.categorize(""))
    }

    @Test
    fun `categorize — text hits two rules — the first rule in table order wins`() {
        // "market" (groceries) and "cafe" (dining) both hit; groceries precedes dining.
        assertEquals(DefaultCategories.GROCERIES.id, engine.categorize("market cafe"))
    }

    @Test
    fun `categorize — ambiguous gas keyword — transport by rule order, deterministically`() {
        assertEquals(DefaultCategories.TRANSPORT.id, engine.categorize("Gas station"))
    }

    @Test
    fun `categorize — custom rule table — replaces the default vocabulary entirely`() {
        val custom = RuleBasedEngine(listOf(CategoryRule(CategoryId("pets"), setOf("kibble"))))

        assertEquals(CategoryId("pets"), custom.categorize("Premium kibble 2kg"))
        assertEquals(CategoryId.UNCATEGORIZED, custom.categorize("Whole Milk 1L"))
    }

    @Test
    fun `default rules — every suggested id exists in the default vocabulary`() {
        val vocabulary = DefaultCategories.all.map(Category::id).toSet()

        RuleBasedEngine.DEFAULT_RULES.forEach { rule ->
            assertTrue(
                "rule for ${rule.category} points outside DefaultCategories.all",
                rule.category in vocabulary,
            )
        }
    }
}
