package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiNanoPromptTest {
    @Test
    fun `buildCategorizationPrompt — merchant and line items — enumerates seeded names and both evidence lines`() {
        val evidence = CategorizationEvidence(merchant = "Synth Cafe", lineItemDescriptions = listOf("Coffee", "Bagel"))

        val prompt = buildCategorizationPrompt(evidence)

        assertEquals(
            "Classify this receipt into exactly one of: " +
                "Uncategorized, Groceries, Dining, Transport, Utilities, Shopping.\n" +
                "Respond with only the category name, nothing else.\n\n" +
                "Merchant: Synth Cafe\n" +
                "Items: Coffee, Bagel",
            prompt,
        )
    }

    @Test
    fun `buildCategorizationPrompt — no merchant, no line items — evidence body is the no-evidence placeholder`() {
        val evidence = CategorizationEvidence(merchant = null, lineItemDescriptions = emptyList())

        val prompt = buildCategorizationPrompt(evidence)

        assertEquals(
            "Classify this receipt into exactly one of: " +
                "Uncategorized, Groceries, Dining, Transport, Utilities, Shopping.\n" +
                "Respond with only the category name, nothing else.\n\n" +
                "(no evidence)",
            prompt,
        )
    }

    @Test
    fun `buildCategorizationPrompt — blank merchant — merchant line omitted, not rendered blank`() {
        val evidence = CategorizationEvidence(merchant = "  ", lineItemDescriptions = listOf("Milk"))

        val prompt = buildCategorizationPrompt(evidence)

        assertEquals(
            "Classify this receipt into exactly one of: " +
                "Uncategorized, Groceries, Dining, Transport, Utilities, Shopping.\n" +
                "Respond with only the category name, nothing else.\n\n" +
                "Items: Milk",
            prompt,
        )
    }

    @Test
    fun `resolveCategory — exact seeded name — matching category id`() {
        assertEquals(DefaultCategories.DINING.id, resolveCategory("Dining"))
    }

    @Test
    fun `resolveCategory — different case and surrounding whitespace — still resolves`() {
        assertEquals(DefaultCategories.GROCERIES.id, resolveCategory("  groceries  "))
    }

    @Test
    fun `resolveCategory — seeded id instead of name — still resolves`() {
        assertEquals(CategoryId("transport"), resolveCategory("transport"))
    }

    @Test
    fun `resolveCategory — text outside the seeded vocabulary — AiEngineUnavailable, never a fabricated category`() {
        assertThrows(AiEngineUnavailable::class.java) { resolveCategory("Food") }
    }

    @Test
    fun `resolveCategory — null response — AiEngineUnavailable`() {
        assertThrows(AiEngineUnavailable::class.java) { resolveCategory(null) }
    }

    @Test
    fun `resolveCategory — rambling response beyond the category name — AiEngineUnavailable`() {
        assertThrows(
            AiEngineUnavailable::class.java,
        ) { resolveCategory("I think this is Dining because of the coffee") }
    }
}
