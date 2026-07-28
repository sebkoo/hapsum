package io.github.sebkoo.hapsum.feature.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * The sanctioned one-collector effect idiom (ADR-0004): collects [effects] only while the
 * lifecycle is at least STARTED. Feature-local for now — capture is the first screen to emit
 * effects, so this stays here until a second effect-emitting screen needs it shared (the same
 * "wait for the second consumer" call ADR-0004 already made for `DispatcherProvider`'s test
 * double).
 */
@Composable
internal fun <E> CollectEffects(
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
