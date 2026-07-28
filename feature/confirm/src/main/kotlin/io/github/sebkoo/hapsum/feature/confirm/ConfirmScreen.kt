package io.github.sebkoo.hapsum.feature.confirm

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ReceiptId

@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    receiptId: ReceiptId,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(receiptId) {
        viewModel.onIntent(ConfirmUiIntent.LoadReceipt(receiptId))
    }

    ConfirmContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun ConfirmContent(
    state: ConfirmUiState,
    onIntent: (ConfirmUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.error == ConfirmError.LoadFailed -> {
                Text(
                    text = stringResource(R.string.confirm_load_failed),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> {
                ConfirmForm(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun ConfirmForm(
    state: ConfirmUiState,
    onIntent: (ConfirmUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.merchant?.let { merchant ->
            Text(
                text = merchant.value,
                style = MaterialTheme.typography.titleLarge,
                color = merchant.confidence.contentColor(),
            )
        }

        OutlinedTextField(
            value = state.amountText,
            onValueChange = { text -> onIntent(ConfirmUiIntent.AmountChanged(text)) },
            label = { Text(stringResource(R.string.confirm_amount_label)) },
            supportingText = { LowConfidenceHint(state.amountConfidence) },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.dateText,
            onValueChange = { text -> onIntent(ConfirmUiIntent.DateChanged(text)) },
            label = { Text(stringResource(R.string.confirm_date_label)) },
            supportingText = { LowConfidenceHint(state.dateConfidence) },
            modifier = Modifier.fillMaxWidth(),
        )

        CategorySelector(
            selected = state.categoryId,
            onSelected = { categoryId -> onIntent(ConfirmUiIntent.CategorySelected(categoryId)) },
        )

        if (state.lineItems.isNotEmpty()) {
            LineItemsList(lineItems = state.lineItems)
        }

        if (state.lineItemsMismatch) {
            Text(
                text = stringResource(R.string.confirm_totals_mismatch),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (state.error == ConfirmError.SaveFailed) {
            Text(
                text = stringResource(R.string.confirm_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = { onIntent(ConfirmUiIntent.SaveClicked) },
            enabled = state.canSave,
        ) {
            Text(text = stringResource(R.string.confirm_save))
        }
    }
}

@Composable
private fun LowConfidenceHint(confidence: ParseConfidence?) {
    if (confidence == ParseConfidence.LOW) {
        Text(text = stringResource(R.string.confirm_low_confidence))
    }
}

@Composable
private fun ParseConfidence.contentColor() =
    if (this == ParseConfidence.LOW) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

@Composable
private fun CategorySelector(
    selected: CategoryId,
    onSelected: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DefaultCategories.all.forEach { category ->
            FilterChip(
                selected = category.id == selected,
                onClick = { onSelected(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun LineItemsList(
    lineItems: List<LineItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lineItems.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
                Text(text = item.amount.display(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Two-decimal placeholder, same known limitation as `:feature:ledger`'s Money.display() (ADR-0002
// keeps Money free of formatting): wrong for zero-decimal currencies like KRW.
private fun Money.display(): String {
    val units = minorUnits / 100
    val cents = (minorUnits % 100).toString().padStart(2, '0')
    return "${currency.isoCode} $units.$cents"
}
