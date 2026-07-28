package io.github.sebkoo.hapsum.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * The sanctioned one-collector effect idiom (ADR-0004): collects [effects] only while the
 * lifecycle is at least STARTED. Started feature-local to `:feature:capture` (its own KDoc:
 * "stays here until a second effect-emitting screen needs it shared") and promoted here once
 * `:feature:confirm` became that second screen — the same "wait for the second consumer" call
 * ADR-0004 already made for `DispatcherProvider`'s test double.
 */
@Composable
fun <E> CollectEffects(
    effects: Flow<E>,
    onEffect: (E) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect(onEffect)
        }
    }
}
