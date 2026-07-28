package io.github.sebkoo.hapsum.feature.insights

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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sebkoo.hapsum.core.designsystem.format

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InsightsContent(state = state, modifier = modifier)
}

@Composable
internal fun InsightsContent(
    state: InsightsUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> {
                val loadingDescription = stringResource(R.string.insights_loading)
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }

            state.error != null -> {
                Text(
                    text = stringResource(R.string.insights_load_failed),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            state.summaries.isEmpty() -> {
                Text(
                    text = stringResource(R.string.insights_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> {
                InsightsList(summaries = state.summaries)
            }
        }
    }
}

@Composable
private fun InsightsList(
    summaries: List<MonthlySummary>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(summaries, key = { "${it.month}-${it.total.currency.isoCode}" }) { summary ->
            MonthSection(summary = summary)
        }
    }
}

@Composable
private fun MonthSection(
    summary: MonthlySummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = summary.month.toString(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = summary.total.format(LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        summary.categories.forEach { category -> CategoryRow(category = category) }
    }
}

@Composable
private fun CategoryRow(
    category: CategorySummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = category.category.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = category.total.format(LocalLocale.current.platformLocale),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
