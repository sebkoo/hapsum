package io.github.sebkoo.hapsum.core.testing

/**
 * Folds intents through a pure reducer on plain JVM — no Android, no coroutines (ADR-0004).
 * Construct one per screen with the SAME initial state and reducer reference the ViewModel
 * uses, so the function under test is the function in production by construction.
 */
class ReducerTestHarness<S, I>(
    private val initialState: S,
    private val reducer: (S, I) -> S,
) {
    /** Final state after reducing the intents in order from the initial state. */
    fun after(vararg intents: I): S = after(initialState, *intents)

    /** Final state after reducing from an explicit starting condition. */
    fun after(
        from: S,
        vararg intents: I,
    ): S = intents.fold(from, reducer)

    /** Every state the reduction passes through, starting state included. */
    fun trajectory(vararg intents: I): List<S> = trajectory(initialState, *intents)

    /** The trajectory from an explicit starting condition. */
    fun trajectory(
        from: S,
        vararg intents: I,
    ): List<S> = intents.runningFold(from, reducer)
}
