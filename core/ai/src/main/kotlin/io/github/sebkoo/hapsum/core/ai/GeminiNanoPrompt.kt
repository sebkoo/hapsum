package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories

/** Fixed decoding params (ADR-0006): zero temperature and a seed for determinism, a small budget. */
internal const val GEMINI_NANO_TEMPERATURE = 0f
internal const val GEMINI_NANO_TOP_K = 1
internal const val GEMINI_NANO_MAX_OUTPUT_TOKENS = 16
internal const val GEMINI_NANO_SEED = 0

/**
 * Pure prompt construction (ADR-0006, golden-tested): enumerates [categories]' names so the
 * model can only ever echo a seeded name back — [resolveCategory] still validates that, since
 * structured output is best-effort, not a guarantee.
 */
internal fun buildCategorizationPrompt(
    evidence: CategorizationEvidence,
    categories: List<Category> = DefaultCategories.all,
): String {
    val names = categories.joinToString(", ") { it.name }
    val evidenceLines =
        listOfNotNull(
            evidence.merchant?.takeIf { it.isNotBlank() }?.let { "Merchant: $it" },
            evidence.lineItemDescriptions.takeIf { it.isNotEmpty() }?.let { "Items: ${it.joinToString(", ")}" },
        )
    val evidenceText = evidenceLines.joinToString("\n").ifBlank { "(no evidence)" }
    return "Classify this receipt into exactly one of: $names.\n" +
        "Respond with only the category name, nothing else.\n\n$evidenceText"
}

/**
 * Pure response resolution (ADR-0006, golden-tested): case-insensitive match against
 * [categories]' seeded names or ids. Resolution can only ever produce a seeded id — an
 * unresolvable response is engine failure, not a terminal answer, so the floor still gets to
 * answer from the original evidence (the ADR-0003 invariant holds by construction).
 */
internal fun resolveCategory(
    responseText: String?,
    categories: List<Category> = DefaultCategories.all,
): CategoryId {
    val candidate = responseText?.trim().orEmpty()
    return categories
        .firstOrNull {
            candidate.equals(
                it.name,
                ignoreCase = true,
            ) || candidate.equals(it.id.value, ignoreCase = true)
        }?.id
        ?: throw AiEngineUnavailable("unresolvable Gemini Nano response: \"$responseText\"")
}
