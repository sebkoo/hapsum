package io.github.sebkoo.hapsum.feature.ledger

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.github.sebkoo.hapsum.core.mvi.MviViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedgerViewModel
    @Inject
    constructor(
        repository: ExpenseRepository,
        dispatchers: DispatcherProvider,
    ) : MviViewModel<LedgerUiState, LedgerUiIntent, LedgerUiEffect>(
            initialState = LedgerUiState(),
            reducer = reducer,
        ) {
        init {
            viewModelScope.launch(dispatchers.io) {
                repository
                    .observeExpensesWithCategory()
                    .catch { dispatch(LedgerUiIntent.Internal.LoadFailed) }
                    .collect { rows -> dispatch(LedgerUiIntent.Internal.Loaded(rows)) }
            }
        }

        companion object {
            /** The screen's single reduction path — the exact value ReducerTestHarness folds with. */
            val reducer: (LedgerUiState, LedgerUiIntent) -> LedgerUiState = ::reduce

            private fun reduce(
                state: LedgerUiState,
                intent: LedgerUiIntent,
            ): LedgerUiState =
                when (intent) {
                    is LedgerUiIntent.Internal.Loaded -> {
                        state.copy(isLoading = false, rows = intent.rows, error = null)
                    }

                    LedgerUiIntent.Internal.LoadFailed -> {
                        state.copy(isLoading = false, error = LedgerError.LoadFailed)
                    }
                }
        }
    }
