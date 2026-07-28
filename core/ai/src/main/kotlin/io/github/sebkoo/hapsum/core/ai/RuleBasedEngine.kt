package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories

/** One categorization rule: whole-word, case-insensitive keywords that vote for [category]. */
data class CategoryRule(
    val category: CategoryId,
    val keywords: Set<String>,
)

/**
 * The AiEngine ladder's deterministic floor (ADR-0001): a pure keyword categorizer — no model,
 * no network, no clock — so the same text maps to the same [CategoryId] on every device, every
 * time. That determinism is what makes it always available and fully unit-testable; the
 * `AiEngine` interface this class will eventually sit behind is ADR-0006's decision (row 19),
 * and nothing here pre-empts it.
 *
 * Matching is whole-word and case-insensitive — "marketplace" never hits "market". Rule order
 * is precedence: the first rule with any keyword hit wins, which is how genuinely ambiguous
 * words ("gas": fuel or utility) get exactly one deterministic answer. No hit falls back to
 * [CategoryId.UNCATEGORIZED] — the engine never invents a category, and every id in
 * [DEFAULT_RULES] exists in [DefaultCategories.all] by construction (pinned by a test).
 */
class RuleBasedEngine(
    rules: List<CategoryRule> = DEFAULT_RULES,
) {
    private val compiledRules =
        rules.map { rule ->
            rule.category to
                rule.keywords.map { keyword ->
                    Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE)
                }
        }

    /** [text] is whatever evidence the caller holds — a line-item description, a merchant, or both. */
    fun categorize(text: String): CategoryId =
        compiledRules
            .firstOrNull { (_, patterns) -> patterns.any { pattern -> pattern.containsMatchIn(text) } }
            ?.first
            ?: CategoryId.UNCATEGORIZED

    companion object {
        /** Generic words only — never a brand, client, or personal-context name (project law). */
        val DEFAULT_RULES: List<CategoryRule> =
            listOf(
                CategoryRule(
                    category = DefaultCategories.GROCERIES.id,
                    keywords =
                        setOf("grocery", "grocer", "supermarket", "market", "mart", "produce", "milk", "bread", "eggs"),
                ),
                CategoryRule(
                    category = DefaultCategories.DINING.id,
                    keywords =
                        setOf(
                            "cafe",
                            "coffee",
                            "espresso",
                            "restaurant",
                            "diner",
                            "bakery",
                            "pizza",
                            "burger",
                            "sushi",
                        ),
                ),
                CategoryRule(
                    category = DefaultCategories.TRANSPORT.id,
                    keywords =
                        setOf(
                            "taxi",
                            "bus",
                            "metro",
                            "subway",
                            "train",
                            "fare",
                            "fuel",
                            "gas",
                            "parking",
                            "toll",
                        ),
                ),
                CategoryRule(
                    category = DefaultCategories.UTILITIES.id,
                    keywords =
                        setOf(
                            "electric",
                            "electricity",
                            "water",
                            "internet",
                            "broadband",
                            "utility",
                            "utilities",
                        ),
                ),
                CategoryRule(
                    category = DefaultCategories.SHOPPING.id,
                    keywords =
                        setOf(
                            "clothing",
                            "apparel",
                            "shoes",
                            "electronics",
                            "pharmacy",
                            "bookstore",
                            "hardware",
                        ),
                ),
            )
    }
}
