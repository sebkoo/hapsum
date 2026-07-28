package io.github.sebkoo.hapsum.feature.ledger

import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.mvi.InternalUiIntent
import io.github.sebkoo.hapsum.core.mvi.UiEffect
import io.github.sebkoo.hapsum.core.mvi.UiIntent
import io.github.sebkoo.hapsum.core.mvi.UiState

data class LedgerUiState(
    val isLoading: Boolean = true,
    val rows: List<ExpenseWithCategory> = emptyList(),
    val error: LedgerError? = null,
) : UiState

/** Sealed, never a raw String (ADR-0004); grows variants as failure modes become real. */
sealed interface LedgerError {
    data object LoadFailed : LedgerError
}

sealed interface LedgerUiIntent : UiIntent {
    /** Async results re-entering the reducer — unforgeable from the UI (ADR-0004). */
    sealed interface Internal :
        LedgerUiIntent,
        InternalUiIntent {
        data class Loaded(
            val rows: List<ExpenseWithCategory>,
        ) : Internal

        data object LoadFailed : Internal
    }
}

/** No variants yet: navigation effects arrive with their targets (no detail screen exists). */
sealed interface LedgerUiEffect : UiEffect
