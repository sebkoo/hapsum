package io.github.sebkoo.hapsum.feature.insights

import io.github.sebkoo.hapsum.core.mvi.InternalUiIntent
import io.github.sebkoo.hapsum.core.mvi.UiEffect
import io.github.sebkoo.hapsum.core.mvi.UiIntent
import io.github.sebkoo.hapsum.core.mvi.UiState

data class InsightsUiState(
    val isLoading: Boolean = true,
    val summaries: List<MonthlySummary> = emptyList(),
    val error: InsightsError? = null,
) : UiState

/** Sealed, never a raw String (ADR-0004); grows variants as failure modes become real. */
sealed interface InsightsError {
    data object LoadFailed : InsightsError
}

sealed interface InsightsUiIntent : UiIntent {
    /** Async results re-entering the reducer — unforgeable from the UI (ADR-0004). */
    sealed interface Internal :
        InsightsUiIntent,
        InternalUiIntent {
        data class Loaded(
            val summaries: List<MonthlySummary>,
        ) : Internal

        data object LoadFailed : Internal
    }
}

/** No variants: insights originates no navigation — back is system back. */
sealed interface InsightsUiEffect : UiEffect
