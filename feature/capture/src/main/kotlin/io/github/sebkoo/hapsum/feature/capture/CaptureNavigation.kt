package io.github.sebkoo.hapsum.feature.capture

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.sebkoo.hapsum.core.designsystem.CollectEffects
import io.github.sebkoo.hapsum.core.model.ReceiptId
import kotlinx.serialization.Serializable

/** The capture feature's exported back-stack key — saveable from day one (ADR-0004). */
@Serializable
data object CaptureNavKey : NavKey

/**
 * The feature's exported Nav3 entry. `:app` owns the back stack and assembles the graph from
 * entries like this one — features never depend on each other (ADR-0004). `CaptureViewModel`
 * resolves its own dependencies through Hilt (ADR-0005); [onReceiptCaptured] is `:app`'s hook
 * for what happens next (returning to the ledger).
 */
fun EntryProviderScope<NavKey>.captureEntry(onReceiptCaptured: (ReceiptId) -> Unit) {
    entry<CaptureNavKey> {
        val viewModel = hiltViewModel<CaptureViewModel>()
        CollectEffects(effects = viewModel.effects) { effect ->
            when (effect) {
                is CaptureUiEffect.ReceiptCaptured -> onReceiptCaptured(effect.receiptId)
            }
        }
        CaptureScreen(viewModel = viewModel)
    }
}
