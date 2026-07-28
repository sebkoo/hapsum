package io.github.sebkoo.hapsum.core.ai

import io.github.sebkoo.hapsum.core.model.CategoryId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

private class FakeAiEngine(
    private val behavior: suspend () -> CategoryId,
) : AiEngine {
    var callCount = 0
        private set

    override suspend fun categorize(evidence: CategorizationEvidence): CategoryId {
        callCount++
        return behavior()
    }
}

/** Mirrors [GeminiNanoEngine]'s own once-per-process download gate, without the real SDK. */
private class DownloadGatedFakeEngine : AiEngine {
    var callCount = 0
        private set
    var downloadRequests = 0
        private set
    private var downloaded = false

    override suspend fun categorize(evidence: CategorizationEvidence): CategoryId {
        callCount++
        if (!downloaded) {
            downloadRequests++
            downloaded = true
        }
        throw AiEngineUnavailable("downloadable")
    }
}

class AiEngineChainTest {
    private val evidence = CategorizationEvidence(merchant = "Synth Cafe", lineItemDescriptions = listOf("Coffee"))

    @Test
    fun `constructor — empty engine list — throws, the chain always needs a floor`() {
        assertThrows(IllegalArgumentException::class.java) { AiEngineChain(emptyList()) }
    }

    @Test
    fun `categorize — first engine answers — later engines never called, first engine's result wins`() =
        runTest {
            val first = FakeAiEngine { CategoryId("first") }
            val second = FakeAiEngine { CategoryId("second") }
            val chain = AiEngineChain(listOf(first, second))

            val result = chain.categorize(evidence)

            assertEquals(CategoryId("first"), result)
            assertEquals(1, first.callCount)
            assertEquals(0, second.callCount)
        }

    @Test
    fun `categorize — first engine throws AiEngineUnavailable — falls through to the next engine`() =
        runTest {
            val first = FakeAiEngine { throw AiEngineUnavailable("nano offline") }
            val second = FakeAiEngine { CategoryId("floor") }
            val chain = AiEngineChain(listOf(first, second))

            val result = chain.categorize(evidence)

            assertEquals(CategoryId("floor"), result)
            assertEquals(1, first.callCount)
            assertEquals(1, second.callCount)
        }

    @Test
    fun `categorize — first engine exceeds the timeout budget — falls through to the next engine`() =
        runTest {
            val first =
                FakeAiEngine {
                    delay(10.seconds)
                    CategoryId("too-late")
                }
            val second = FakeAiEngine { CategoryId("floor") }
            val chain = AiEngineChain(listOf(first, second))

            val result = chain.categorize(evidence)

            assertEquals(CategoryId("floor"), result)
        }

    @Test
    fun `categorize — the floor itself — no timeout applied, its own result always wins`() =
        runTest {
            val floor =
                FakeAiEngine {
                    delay(10.seconds)
                    CategoryId("floor")
                }
            val chain = AiEngineChain(listOf(floor))

            val result = chain.categorize(evidence)

            assertEquals(CategoryId("floor"), result)
        }

    @Test
    fun `categorize — first engine throws a non-typed exception — propagates, never falls through`() =
        runTest {
            val first = FakeAiEngine { throw IllegalStateException("bug") }
            val second = FakeAiEngine { CategoryId("floor") }
            val chain = AiEngineChain(listOf(first, second))

            var thrown: Throwable? = null
            try {
                chain.categorize(evidence)
            } catch (e: IllegalStateException) {
                thrown = e
            }

            assertTrue(thrown is IllegalStateException)
            assertEquals(0, second.callCount)
        }

    @Test
    fun `categorize — first engine throws CancellationException — rethrows, never falls through`() =
        runTest {
            val first = FakeAiEngine { throw CancellationException("cancelled") }
            val second = FakeAiEngine { CategoryId("floor") }
            val chain = AiEngineChain(listOf(first, second))

            var thrown: Throwable? = null
            try {
                chain.categorize(evidence)
            } catch (e: CancellationException) {
                thrown = e
            }

            assertTrue(thrown is CancellationException)
            assertEquals(0, second.callCount)
        }

    @Test
    fun `categorize — engine requests a download once — chain still calls the engine on every attempt`() =
        runTest {
            val nano = DownloadGatedFakeEngine()
            val chain = AiEngineChain(listOf(nano, FakeAiEngine { CategoryId("floor") }))

            chain.categorize(evidence)
            chain.categorize(evidence)

            assertEquals(2, nano.callCount)
            assertEquals(1, nano.downloadRequests)
        }
}
