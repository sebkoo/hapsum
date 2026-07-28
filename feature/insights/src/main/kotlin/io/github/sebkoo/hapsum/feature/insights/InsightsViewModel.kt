package io.github.sebkoo.hapsum.feature.insights

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.github.sebkoo.hapsum.core.mvi.MviViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        repository: ExpenseRepository,
        aggregate: AggregateMonthlySummariesUseCase,
        dispatchers: DispatcherProvider,
    ) : MviViewModel<InsightsUiState, InsightsUiIntent, InsightsUiEffect>(
            initialState = InsightsUiState(),
            reducer = reducer,
        ) {
        init {
            viewModelScope.launch(dispatchers.io) {
                repository
                    .observeExpensesWithCategory()
                    .map { rows -> aggregate(rows) }
                    .catch { dispatch(InsightsUiIntent.Internal.LoadFailed) }
                    .collect { summaries -> dispatch(InsightsUiIntent.Internal.Loaded(summaries)) }
            }
        }

        companion object {
            /** The screen's single reduction path — the exact value ReducerTestHarness folds with. */
            val reducer: (InsightsUiState, InsightsUiIntent) -> InsightsUiState = ::reduce

            private fun reduce(
                state: InsightsUiState,
                intent: InsightsUiIntent,
            ): InsightsUiState =
                when (intent) {
                    is InsightsUiIntent.Internal.Loaded -> {
                        state.copy(isLoading = false, summaries = intent.summaries, error = null)
                    }

                    InsightsUiIntent.Internal.LoadFailed -> {
                        state.copy(isLoading = false, error = InsightsError.LoadFailed)
                    }
                }
        }
    }
