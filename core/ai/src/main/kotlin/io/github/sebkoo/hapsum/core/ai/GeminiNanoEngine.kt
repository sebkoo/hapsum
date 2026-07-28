package io.github.sebkoo.hapsum.core.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import io.github.sebkoo.hapsum.core.model.CategoryId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Gemini Nano behind the [AiEngine] seam (ADR-0006): a thin adapter over ML Kit's Prompt API
 * with zero JVM coverage by design — the `MlKitOcrEngine`/`CameraXCapture` trade, third
 * instance. Prompt construction and response resolution are the pure, golden-tested halves
 * ([buildCategorizationPrompt], [resolveCategory]); everything here is untestable platform
 * glue. `Generation.getClient()` resolves Gemini Nano via AICore internally — no `Context`
 * argument needed.
 *
 * [AiEngineChain] owns fallback policy; this class only ever throws [AiEngineUnavailable] (or
 * rethrows [CancellationException]) — it never decides to retry another strategy itself.
 */
internal class GeminiNanoEngine : AiEngine {
    private val model: GenerativeModel by lazy { Generation.getClient() }
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var downloadRequested = false

    override suspend fun categorize(evidence: CategorizationEvidence): CategoryId {
        when (checkedStatus()) {
            FeatureStatus.AVAILABLE -> {}

            FeatureStatus.DOWNLOADABLE -> {
                requestDownloadOnce()
                throw AiEngineUnavailable("Gemini Nano is downloadable, not yet available")
            }

            else -> {
                throw AiEngineUnavailable("Gemini Nano is not available")
            }
        }
        val response = generateContent(buildCategorizationPrompt(evidence))
        return resolveCategory(response.candidates.firstOrNull()?.text)
    }

    private suspend fun checkedStatus(): Int =
        try {
            model.checkStatus()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: GenAiException) {
            throw AiEngineUnavailable("Gemini Nano status check failed", error)
        }

    private suspend fun generateContent(prompt: String) =
        try {
            model.generateContent(
                generateContentRequest(TextPart(prompt)) {
                    temperature = GEMINI_NANO_TEMPERATURE
                    topK = GEMINI_NANO_TOP_K
                    maxOutputTokens = GEMINI_NANO_MAX_OUTPUT_TOKENS
                    seed = GEMINI_NANO_SEED
                },
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: GenAiException) {
            throw AiEngineUnavailable("Gemini Nano inference failed (errorCode=${error.errorCode})", error)
        }

    /** AICore owns the transfer policy once requested; a later [checkedStatus] sees the progress. */
    private fun requestDownloadOnce() {
        if (downloadRequested) return
        downloadRequested = true
        downloadScope.launch { model.download().collect {} }
    }
}
