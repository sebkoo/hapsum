package io.github.sebkoo.hapsum.feature.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.model.Money

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LedgerContent(state = state, modifier = modifier)
}

@Composable
internal fun LedgerContent(
    state: LedgerUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> {
                val loadingDescription = stringResource(R.string.ledger_loading)
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }

            state.error != null -> {
                Text(
                    text = stringResource(R.string.ledger_load_failed),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            state.rows.isEmpty() -> {
                Text(
                    text = stringResource(R.string.ledger_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> {
                LedgerList(rows = state.rows)
            }
        }
    }
}

@Composable
private fun LedgerList(
    rows: List<ExpenseWithCategory>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(rows, key = { it.expense.id.value }) { row ->
            LedgerRow(row = row)
        }
    }
}

@Composable
private fun LedgerRow(
    row: ExpenseWithCategory,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = row.category.name, style = MaterialTheme.typography.titleMedium)
            Text(text = row.expense.date.toString(), style = MaterialTheme.typography.bodySmall)
        }
        Text(text = row.expense.amount.display(), style = MaterialTheme.typography.titleMedium)
    }
}

// Two-decimal placeholder: real locale/currency-aware formatting is deferred presentation
// work (ADR-0002 keeps Money free of formatting); wrong for zero-decimal currencies like KRW.
private fun Money.display(): String {
    val units = minorUnits / 100
    val cents = (minorUnits % 100).toString().padStart(2, '0')
    return "${currency.isoCode} $units.$cents"
}
