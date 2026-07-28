package io.github.sebkoo.hapsum.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.sebkoo.hapsum.AppContainer
import io.github.sebkoo.hapsum.core.mvi.DefaultDispatcherProvider
import io.github.sebkoo.hapsum.feature.ledger.LedgerNavKey
import io.github.sebkoo.hapsum.feature.ledger.ledgerEntry

/**
 * `:app` owns the back stack and assembles the graph from the entries each feature exports —
 * features never see each other (ADR-0004).
 */
@Composable
fun HapsumApp(container: AppContainer) {
    val backStack = rememberNavBackStack(LedgerNavKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                ledgerEntry(
                    repository = container.expenseRepository,
                    dispatchers = DefaultDispatcherProvider,
                )
            },
    )
}
