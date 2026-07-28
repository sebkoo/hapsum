package io.github.sebkoo.hapsum.feature.confirm

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.sebkoo.hapsum.core.designsystem.CollectEffects
import io.github.sebkoo.hapsum.core.model.ReceiptId
import kotlinx.serialization.Serializable

/**
 * The confirm feature's exported back-stack key — saveable from day one (ADR-0004). Carries a
 * plain `String`, not [ReceiptId]: the value class isn't `@Serializable`, and adding
 * kotlinx-serialization to `:core:model` for one nav argument would be scope this row doesn't
 * need.
 */
@Serializable
data class ConfirmNavKey(
    val receiptId: String,
) : NavKey

/**
 * The feature's exported Nav3 entry. `:app` owns the back stack and assembles the graph from
 * entries like this one — features never depend on each other (ADR-0004). `ConfirmViewModel`
 * resolves its Hilt-injected dependencies on its own; [onSaved] is `:app`'s hook for what happens
 * next (clearing capture/confirm and returning to the ledger).
 */
fun EntryProviderScope<NavKey>.confirmEntry(onSaved: () -> Unit) {
    entry<ConfirmNavKey> { key ->
        val viewModel = hiltViewModel<ConfirmViewModel>()
        CollectEffects(effects = viewModel.effects) { effect ->
            when (effect) {
                ConfirmUiEffect.Saved -> onSaved()
            }
        }
        ConfirmScreen(viewModel = viewModel, receiptId = ReceiptId(key.receiptId))
    }
}
