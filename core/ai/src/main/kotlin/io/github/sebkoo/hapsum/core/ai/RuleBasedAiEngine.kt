package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.CategoryId

/**
 * Adapts the pure-text [RuleBasedEngine] to the [AiEngine] seam (ADR-0006) — the deterministic
 * floor of the engine ladder. Joining merchant and line-item descriptions mirrors exactly what
 * confirm's prefill did before this ADR, so the floor's suggestions are unchanged.
 */
class RuleBasedAiEngine(
    private val engine: RuleBasedEngine = RuleBasedEngine(),
) : AiEngine {
    override suspend fun categorize(evidence: CategorizationEvidence): CategoryId =
        engine.categorize(joinEvidence(evidence))

    private fun joinEvidence(evidence: CategorizationEvidence): String =
        (listOf(evidence.merchant.orEmpty()) + evidence.lineItemDescriptions).joinToString(" ").trim()
}
