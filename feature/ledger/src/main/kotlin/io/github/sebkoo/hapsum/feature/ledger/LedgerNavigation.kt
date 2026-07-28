package io.github.sebkoo.hapsum.feature.ledger

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import kotlinx.serialization.Serializable

/** The ledger's exported back-stack key — saveable from day one (ADR-0004). */
@Serializable
data object LedgerNavKey : NavKey

/**
 * The feature's exported Nav3 entry. `:app` owns the back stack and assembles the graph from
 * entries like this one — features never depend on each other (ADR-0004). Dependencies arrive
 * as parameters until a DI row exists.
 */
fun EntryProviderScope<NavKey>.ledgerEntry(
    repository: ExpenseRepository,
    dispatchers: DispatcherProvider,
) {
    entry<LedgerNavKey> {
        val viewModel = viewModel { LedgerViewModel(repository, dispatchers) }
        LedgerScreen(viewModel = viewModel)
    }
}
