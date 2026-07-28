package io.github.sebkoo.hapsum.core.testing

import org.junit.Assert.assertEquals
import org.junit.Test

private sealed interface ToyIntent {
    data class Add(
        val n: Int,
    ) : ToyIntent

    data object Reset : ToyIntent
}

private fun reduceToy(
    state: Int,
    intent: ToyIntent,
): Int =
    when (intent) {
        is ToyIntent.Add -> state + intent.n
        ToyIntent.Reset -> 0
    }

class ReducerTestHarnessTest {
    private val harness = ReducerTestHarness(initialState = 0, reducer = ::reduceToy)

    @Test
    fun `after — sequence of intents — folds them from the initial state`() {
        assertEquals(5, harness.after(ToyIntent.Add(2), ToyIntent.Add(3)))
    }

    @Test
    fun `after — explicit starting condition — folds from that state, not the initial one`() {
        assertEquals(7, harness.after(from = 4, ToyIntent.Add(3)))
    }

    @Test
    fun `trajectory — sequence of intents — returns the initial and every intermediate state`() {
        assertEquals(
            listOf(0, 2, 5, 0),
            harness.trajectory(ToyIntent.Add(2), ToyIntent.Add(3), ToyIntent.Reset),
        )
    }

    @Test
    fun `trajectory — explicit starting condition — walks from that state`() {
        assertEquals(listOf(10, 12), harness.trajectory(from = 10, ToyIntent.Add(2)))
    }
}
