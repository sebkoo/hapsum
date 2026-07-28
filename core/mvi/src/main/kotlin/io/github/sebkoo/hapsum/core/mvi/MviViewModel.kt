package io.github.sebkoo.hapsum.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for every screen's ViewModel — the MVI contract (CLAUDE.md, ADR-0004) made cheap
 * to follow: a single [onIntent] entry, reduction through one pure `reducer`, and one-shot
 * [effects] with guaranteed delivery across collector gaps.
 *
 * Convention: `reducer` must reference the screen's companion-level reduce function — never a
 * lambda literal in the super-call — so the exact function value the ViewModel reduces with is
 * the one under test in `ReducerTestHarness`. Purity is load-bearing: the CAS loop inside
 * [MutableStateFlow.updateAndGet] may re-invoke the reducer on contention.
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
    private val reducer: (S, I) -> S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // A Channel, not SharedFlow(replay = 0): effects emitted while no collector is attached
    // (the config-change gap) buffer until the next collector instead of dropping (ADR-0004).
    // onUndeliveredElement re-queues the one effect a cancelled-mid-handoff collector received
    // but never processed — re-entry lands at the tail, which can reorder under a burst; that
    // is the accepted price of never losing a one-shot (ADR-0004).
    private val _effects = Channel<E>(Channel.BUFFERED, onUndeliveredElement = { requeue(it) })
    private val collecting = AtomicBoolean(false)

    /** Single-collector by contract; a second concurrent collector fails fast (fan-out is silent). */
    val effects: Flow<E> =
        flow {
            check(collecting.compareAndSet(false, true)) {
                "effects already has an active collector — one-shot effects are single-collector"
            }
            try {
                emitAll(_effects.receiveAsFlow())
            } finally {
                collecting.set(false)
            }
        }

    private val pendingIntents = ConcurrentLinkedQueue<I>()
    private val draining = AtomicBoolean(false)

    /** The single public entry. Rejects [InternalUiIntent]s — those re-enter via [dispatch]. */
    fun onIntent(intent: I) {
        check(intent !is InternalUiIntent) {
            "internal intents re-enter via dispatch(), never through the public entry"
        }
        dispatch(intent)
    }

    /**
     * Re-entry point for the ViewModel's own async results. Intents are applied strictly in
     * arrival order through a drain loop — a dispatch made during [react] is queued behind the
     * intent being processed, never recursed into.
     */
    protected fun dispatch(intent: I) {
        pendingIntents += intent
        drainIfIdle()
    }

    private fun drainIfIdle() {
        while (draining.compareAndSet(false, true)) {
            try {
                while (true) {
                    val intent = pendingIntents.poll() ?: break
                    val newState = _state.updateAndGet { reducer(it, intent) }
                    react(intent, newState)
                }
            } finally {
                draining.set(false)
            }
            if (pendingIntents.isEmpty()) return
        }
    }

    /**
     * Post-reduction hook for side effects: launch async work, [sendEffect], [dispatch]
     * results. `state` is exactly the state this intent's reduction installed — valid
     * synchronously; inside launched coroutines read [state]`.value` at the point of use.
     * State changes NEVER happen here — they re-enter through [dispatch] (ADR-0004).
     */
    protected open fun react(
        intent: I,
        state: S,
    ) {}

    /**
     * Guaranteed delivery without a dispatcher hop: trySend succeeds synchronously unless the
     * buffer is full (64 undelivered effects), and only that overflow path suspends inside
     * [viewModelScope] — so tests need no Main dispatcher and teardown cannot eat an effect
     * that a synchronous send already placed in the channel.
     */
    protected fun sendEffect(effect: E) {
        _effects.trySend(effect).onFailure {
            viewModelScope.launch { _effects.send(effect) }
        }
    }

    private fun requeue(effect: E) {
        _effects.trySend(effect)
    }
}
