package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.CategoryId

/**
 * Typed evidence a categorizer reasons over — structure a prompt benefits from ("merchant: X;
 * items: Y, Z"); [RuleBasedAiEngine] adapts by joining. Widening this later is an additive
 * data-class change, not an interface break (ADR-0006).
 */
data class CategorizationEvidence(
    val merchant: String?,
    val lineItemDescriptions: List<String>,
)

/**
 * Expected runtime inability to categorize — the only exception [AiEngineChain] ever catches
 * (ADR-0006). Everything else propagates: a programmer error, not expected AI unavailability.
 */
class AiEngineUnavailable(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * One seam for every categorizer, deterministic or generative (ADR-0006). Bare [CategoryId] out
 * — no suggestion-confidence pair, because a keyword engine has no honest confidence scale and
 * inventing one to pair with an LLM's would fake precision. Uncertainty is spelled
 * [CategoryId.UNCATEGORIZED]. Cancellation is structured concurrency: implementations rethrow
 * [kotlinx.coroutines.CancellationException].
 */
interface AiEngine {
    /**
     * Total for the deterministic floor; generative engines throw [AiEngineUnavailable] — the
     * chain contains exactly that, nothing else.
     */
    suspend fun categorize(evidence: CategorizationEvidence): CategoryId
}
