package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.CategoryId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Engine selection as one ordered chain, itself an [AiEngine] (ADR-0006): tries [engines] in
 * order, falling through to the next on [AiEngineUnavailable] or a timeout. The chain owns all
 * orchestration policy — an engine that swallows its own failure to answer with another
 * strategy is a bug, not a feature, because it would create hidden precedence these tests can't
 * see. Any other exception propagates: a programmer error, not expected AI unavailability.
 * [kotlinx.coroutines.CancellationException] always rethrows.
 *
 * The last engine in [engines] is the deterministic floor: exempt from the timeout budget
 * (microseconds, total) and never expected to throw, which is what makes the chain total by
 * construction.
 */
class AiEngineChain(
    private val engines: List<AiEngine>,
) : AiEngine {
    init {
        require(engines.isNotEmpty()) { "AiEngineChain needs at least one engine — the deterministic floor" }
    }

    override suspend fun categorize(evidence: CategorizationEvidence): CategoryId {
        engines.forEachIndexed { index, engine ->
            val isFloor = index == engines.lastIndex
            try {
                if (isFloor) {
                    return engine.categorize(evidence)
                }
                val result = withTimeoutOrNull(TIMEOUT) { engine.categorize(evidence) }
                if (result != null) return result
                // Timeout: falls through to the next engine, same as AiEngineUnavailable below.
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unavailable: AiEngineUnavailable) {
                // Expected: fall through to the next engine.
            }
        }
        error("AiEngineChain: the floor engine (${engines.last()}) failed to answer — the floor must never throw")
    }

    companion object {
        // TODO: tune on device (ADR-0006).
        private val TIMEOUT = 2.seconds
    }
}
