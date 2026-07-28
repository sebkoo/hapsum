package io.github.sebkoo.hapsum.feature.confirm

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.mvi.InternalUiIntent
import io.github.sebkoo.hapsum.core.mvi.UiEffect
import io.github.sebkoo.hapsum.core.mvi.UiIntent
import io.github.sebkoo.hapsum.core.mvi.UiState
import java.time.LocalDate

/**
 * `amount`/`date` are the edited [Expense][io.github.sebkoo.hapsum.core.model.Expense] fields
 * confirm writes on save; `amountText`/`dateText` are exactly what the user has typed, which can
 * transiently fail to parse — the last successfully parsed value survives an unparseable
 * keystroke so [canSave] never flips true→false→true on a single edit (ADR-0003 confirm row).
 * `merchant`/`lineItems` are read-only [Receipt] evidence, shown for context only: `Expense` has
 * no merchant field, and line items are already persisted at capture time.
 */
data class ConfirmUiState(
    val isLoading: Boolean = true,
    val receiptId: ReceiptId? = null,
    val merchant: ParsedField<String>? = null,
    val currency: CurrencyCode = CurrencyCode.of("USD"),
    val amountText: String = "",
    val amount: Money? = null,
    val amountConfidence: ParseConfidence? = null,
    val dateText: String = "",
    val date: LocalDate? = null,
    val dateConfidence: ParseConfidence? = null,
    val categoryId: CategoryId = CategoryId.UNCATEGORIZED,
    val lineItems: List<LineItem> = emptyList(),
    val lineItemsMismatch: Boolean = false,
    val isSaving: Boolean = false,
    val error: ConfirmError? = null,
) : UiState {
    /** An `Expense` needs a real amount and date; nothing else here gates the write. */
    val canSave: Boolean get() = amount != null && date != null && !isSaving
}

/** Sealed, never a raw String (ADR-0004); grows variants as failure modes become real. */
sealed interface ConfirmError {
    data object LoadFailed : ConfirmError

    data object SaveFailed : ConfirmError
}

sealed interface ConfirmUiIntent : UiIntent {
    /** Sent once by the screen when it first sees its receipt id (ADR-0004: no assisted Hilt). */
    data class LoadReceipt(
        val receiptId: ReceiptId,
    ) : ConfirmUiIntent

    data class AmountChanged(
        val text: String,
    ) : ConfirmUiIntent

    data class DateChanged(
        val text: String,
    ) : ConfirmUiIntent

    data class CategorySelected(
        val categoryId: CategoryId,
    ) : ConfirmUiIntent

    data object SaveClicked : ConfirmUiIntent

    /** Async results re-entering the reducer — unforgeable from the UI (ADR-0004). */
    sealed interface Internal :
        ConfirmUiIntent,
        InternalUiIntent {
        data class ReceiptLoaded(
            val receipt: Receipt,
            val suggestedCategoryId: CategoryId,
        ) : Internal

        data object ReceiptLoadFailed : Internal

        data object ExpenseSaved : Internal

        data object ExpenseSaveFailed : Internal
    }
}

sealed interface ConfirmUiEffect : UiEffect {
    /** `:app` clears capture/confirm from the back stack and returns to the ledger. */
    data object Saved : ConfirmUiEffect
}
