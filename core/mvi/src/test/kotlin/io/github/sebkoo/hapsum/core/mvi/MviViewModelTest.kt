package io.github.sebkoo.hapsum.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

private data class CounterState(
    val count: Int = 0,
    val applied: List<String> = emptyList(),
) : UiState

private sealed interface CounterIntent : UiIntent {
    data object Bump : CounterIntent

    data object Fan : CounterIntent

    data class Step(
        val tag: String,
    ) : CounterIntent,
        InternalUiIntent

    data class Cascade(
        val remaining: Int,
    ) : CounterIntent
}

private sealed interface CounterEffect : UiEffect {
    data class Announced(
        val count: Int,
    ) : CounterEffect
}

private fun reduceCounter(
    state: CounterState,
    intent: CounterIntent,
): CounterState =
    when (intent) {
        CounterIntent.Bump -> state.copy(count = state.count + 1)
        CounterIntent.Fan -> state.copy(applied = state.applied + "fan")
        is CounterIntent.Step -> state.copy(applied = state.applied + intent.tag)
        is CounterIntent.Cascade -> state.copy(count = state.count + 1)
    }

// The reducer reference mirrors the production convention: the SAME function value the
// ViewModel reduces with is the one a ReducerTestHarness would fold with (ADR-0004).
private class CounterViewModel :
    MviViewModel<CounterState, CounterIntent, CounterEffect>(
        initialState = CounterState(),
        reducer = ::reduceCounter,
    ) {
    override fun react(
        intent: CounterIntent,
        state: CounterState,
    ) {
        when (intent) {
            CounterIntent.Bump -> {
                sendEffect(CounterEffect.Announced(state.count))
            }

            CounterIntent.Fan -> {
                dispatch(CounterIntent.Step("a"))
                dispatch(CounterIntent.Step("b"))
            }

            is CounterIntent.Step -> {
                if (intent.tag == "a") dispatch(CounterIntent.Step("a-child"))
            }

            is CounterIntent.Cascade -> {
                if (intent.remaining > 0) dispatch(CounterIntent.Cascade(intent.remaining - 1))
            }
        }
    }
}

class MviViewModelTest {
    @Test
    fun `state — before any intent — is the initial state`() {
        assertEquals(CounterState(), CounterViewModel().state.value)
    }

    @Test
    fun `onIntent — public intent — reduces state through the injected reducer`() {
        val vm = CounterViewModel()

        vm.onIntent(CounterIntent.Bump)

        assertEquals(1, vm.state.value.count)
    }

    @Test
    fun `onIntent — internal intent through the public entry — fails fast`() {
        assertThrows(IllegalStateException::class.java) {
            CounterViewModel().onIntent(CounterIntent.Step("forged"))
        }
    }

    @Test
    fun `dispatch — cascade from react — applies intents in arrival order, not recursion order`() {
        val vm = CounterViewModel()

        vm.onIntent(CounterIntent.Fan)

        // Queue semantics: fan, a, b, a-child. Recursive semantics would give fan, a, a-child, b.
        assertEquals(listOf("fan", "a", "b", "a-child"), vm.state.value.applied)
    }

    @Test
    fun `dispatch — ten-thousand-deep self-cascade — completes without stack overflow`() {
        val vm = CounterViewModel()

        vm.onIntent(CounterIntent.Cascade(10_000))

        assertEquals(10_001, vm.state.value.count)
    }

    @Test
    fun `effects — emitted with no collector — buffered and delivered in order to the first collector`() =
        runTest {
            val vm = CounterViewModel()
            vm.onIntent(CounterIntent.Bump)
            vm.onIntent(CounterIntent.Bump)

            vm.effects.test {
                assertEquals(CounterEffect.Announced(1), awaitItem())
                assertEquals(CounterEffect.Announced(2), awaitItem())
            }
        }

    @Test
    fun `effects — consumed by one collector — never re-delivered to a later collector`() =
        runTest {
            val vm = CounterViewModel()
            vm.onIntent(CounterIntent.Bump)

            vm.effects.test {
                assertEquals(CounterEffect.Announced(1), awaitItem())
            }

            // Delivery is synchronous in the non-overflow path, so "no events" is a real
            // assertion here, not a not-yet-dispatched false green.
            vm.effects.test {
                expectNoEvents()
            }
        }

    @Test
    fun `effects — second concurrent collector — fails fast`() =
        runTest {
            val vm = CounterViewModel()
            val first = launch(start = CoroutineStart.UNDISPATCHED) { vm.effects.collect {} }
            var caught: IllegalStateException? = null

            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    vm.effects.collect {}
                } catch (expected: IllegalStateException) {
                    caught = expected
                }
            }

            assertNotNull(caught)
            first.cancel()
        }
}
