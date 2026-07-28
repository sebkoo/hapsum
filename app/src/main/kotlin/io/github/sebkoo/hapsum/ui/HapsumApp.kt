package io.github.sebkoo.hapsum.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.sebkoo.hapsum.feature.ledger.LedgerNavKey
import io.github.sebkoo.hapsum.feature.ledger.ledgerEntry

/**
 * `:app` owns the back stack and assembles the graph from the entries each feature exports —
 * features never see each other (ADR-0004). Each entry resolves its own `ViewModel` through
 * Hilt (ADR-0005) — no dependencies threaded through this call site.
 */
@Composable
fun HapsumApp() {
    val backStack = rememberNavBackStack(LedgerNavKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                ledgerEntry()
            },
    )
}
