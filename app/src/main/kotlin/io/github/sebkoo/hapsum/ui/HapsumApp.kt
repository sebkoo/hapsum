package io.github.sebkoo.hapsum.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.sebkoo.hapsum.R
import io.github.sebkoo.hapsum.feature.capture.CaptureNavKey
import io.github.sebkoo.hapsum.feature.capture.captureEntry
import io.github.sebkoo.hapsum.feature.confirm.ConfirmNavKey
import io.github.sebkoo.hapsum.feature.confirm.confirmEntry
import io.github.sebkoo.hapsum.feature.insights.InsightsNavKey
import io.github.sebkoo.hapsum.feature.insights.insightsEntry
import io.github.sebkoo.hapsum.feature.ledger.LedgerNavKey
import io.github.sebkoo.hapsum.feature.ledger.ledgerEntry

/**
 * `:app` owns the back stack and assembles the graph from the entries each feature exports —
 * features never see each other (ADR-0004). Each entry resolves its own `ViewModel` through
 * Hilt (ADR-0005) — no dependencies threaded through this call site. The "add receipt" FAB lives
 * here, not inside the ledger's MVI contract: it needs no state and no reduction, only a push
 * onto the back stack this composable already owns. Capture pops itself and pushes confirm;
 * confirm clears the whole stack back to the ledger on save — the new expense must be visible in
 * the list, and neither capture nor confirm's now-consumed state should reappear on back (row 19).
 * Insights (row 23) is a destination owned by this graph, not a ledger state transition: the
 * TopAppBar's "Insights" action is visible only from the ledger, pushing [InsightsNavKey]; back
 * from insights is system back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapsumApp() {
    val backStack = rememberNavBackStack(LedgerNavKey)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    if (backStack.lastOrNull() == LedgerNavKey) {
                        TextButton(onClick = { backStack.add(InsightsNavKey) }) {
                            Text(text = stringResource(R.string.insights_action))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (backStack.lastOrNull() == LedgerNavKey) {
                val description = stringResource(R.string.add_receipt)
                FloatingActionButton(
                    onClick = { backStack.add(CaptureNavKey) },
                    modifier = Modifier.semantics { contentDescription = description },
                ) {
                    Text(text = "+")
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(padding),
            entryProvider =
                entryProvider {
                    ledgerEntry()
                    captureEntry(
                        onReceiptCaptured = { receiptId ->
                            backStack.removeLastOrNull()
                            backStack.add(ConfirmNavKey(receiptId.value))
                        },
                    )
                    confirmEntry(
                        onSaved = {
                            backStack.clear()
                            backStack.add(LedgerNavKey)
                        },
                    )
                    insightsEntry()
                },
        )
    }
}
