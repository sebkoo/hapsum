package io.github.sebkoo.hapsum.feature.ledger

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The ledger's exported back-stack key — saveable from day one (ADR-0004). */
@Serializable
data object LedgerNavKey : NavKey

/**
 * The feature's exported Nav3 entry. `:app` owns the back stack and assembles the graph from
 * entries like this one — features never depend on each other (ADR-0004). `LedgerViewModel`
 * resolves its own dependencies through Hilt (ADR-0005).
 */
fun EntryProviderScope<NavKey>.ledgerEntry() {
    entry<LedgerNavKey> {
        val viewModel = hiltViewModel<LedgerViewModel>()
        LedgerScreen(viewModel = viewModel)
    }
}
