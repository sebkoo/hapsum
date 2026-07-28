package io.github.sebkoo.hapsum.feature.confirm

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sebkoo.hapsum.core.ai.AiEngine
import io.github.sebkoo.hapsum.core.ai.CategorizationEvidence
import io.github.sebkoo.hapsum.core.ai.RuleBasedAiEngine
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.data.ReceiptRepository
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Expense
import io.github.sebkoo.hapsum.core.model.ExpenseId
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.github.sebkoo.hapsum.core.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Currency
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConfirmViewModel
    @Inject
    constructor(
        private val receiptRepository: ReceiptRepository,
        private val expenseRepository: ExpenseRepository,
        private val aiEngine: AiEngine,
        private val dispatchers: DispatcherProvider,
    ) : MviViewModel<ConfirmUiState, ConfirmUiIntent, ConfirmUiEffect>(
            initialState = ConfirmUiState(),
            reducer = reducer,
        ) {
        /**
         * The two-phase suggestion's immediate half (ADR-0006): the deterministic floor, called
         * directly rather than through [aiEngine] so first paint never waits on the chain's
         * timeout budget. Deterministic and stateless — no reason to inject it.
         */
        private val floorEngine: AiEngine = RuleBasedAiEngine()

        override fun react(
            intent: ConfirmUiIntent,
            state: ConfirmUiState,
        ) {
            when (intent) {
                is ConfirmUiIntent.LoadReceipt -> {
                    loadReceipt(intent.receiptId)
                }

                ConfirmUiIntent.SaveClicked -> {
                    saveExpense(state)
                }

                ConfirmUiIntent.Internal.ExpenseSaved -> {
                    sendEffect(ConfirmUiEffect.Saved)
                }

                else -> {}
            }
        }

        private fun loadReceipt(receiptId: ReceiptId) {
            viewModelScope.launch(dispatchers.io) {
                val receipt =
                    try {
                        receiptRepository.getById(receiptId)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        dispatch(ConfirmUiIntent.Internal.ReceiptLoadFailed)
                        return@launch
                    }
                if (receipt == null) {
                    dispatch(ConfirmUiIntent.Internal.ReceiptLoadFailed)
                    return@launch
                }

                // Phase 1 (ADR-0006): the floor never throws, so first paint never waits.
                val evidence =
                    CategorizationEvidence(
                        merchant = receipt.merchant?.value,
                        lineItemDescriptions = receipt.lineItems.map { it.description },
                    )
                val floorCategoryId = floorEngine.categorize(evidence)
                dispatch(ConfirmUiIntent.Internal.ReceiptLoaded(receipt, floorCategoryId))

                // Phase 2: the chain is total by construction (AiEngineUnavailable never
                // escapes it) — anything it throws here is a programmer error, left to crash.
                val refinedCategoryId = aiEngine.categorize(evidence)
                dispatch(ConfirmUiIntent.Internal.SuggestionRefined(floorCategoryId, refinedCategoryId))
            }
        }

        /** [ConfirmUiState.canSave] already guards amount/date; this re-checks defensively. */
        private fun saveExpense(state: ConfirmUiState) {
            val amount = state.amount ?: return
            val date = state.date ?: return
            val receiptId = state.receiptId ?: return
            viewModelScope.launch(dispatchers.io) {
                val expense =
                    Expense(
                        id = ExpenseId(UUID.randomUUID().toString()),
                        amount = amount,
                        categoryId = state.categoryId,
                        date = date,
                        receiptId = receiptId,
                        // Confirm categorizes at the receipt level (ADR-0003) — per-line-item
                        // expenses are a future write path, not this one.
                        lineItemId = null,
                    )
                try {
                    expenseRepository.addExpense(expense)
                    dispatch(ConfirmUiIntent.Internal.ExpenseSaved)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    dispatch(ConfirmUiIntent.Internal.ExpenseSaveFailed)
                }
            }
        }

        companion object {
            /** The screen's single reduction path — the exact value ReducerTestHarness folds with. */
            val reducer: (ConfirmUiState, ConfirmUiIntent) -> ConfirmUiState = ::reduce

            private fun reduce(
                state: ConfirmUiState,
                intent: ConfirmUiIntent,
            ): ConfirmUiState =
                when (intent) {
                    is ConfirmUiIntent.LoadReceipt -> {
                        state.copy(isLoading = true, receiptId = intent.receiptId, error = null)
                    }

                    is ConfirmUiIntent.Internal.ReceiptLoaded -> {
                        applyReceipt(state, intent)
                    }

                    ConfirmUiIntent.Internal.ReceiptLoadFailed -> {
                        state.copy(isLoading = false, error = ConfirmError.LoadFailed)
                    }

                    is ConfirmUiIntent.Internal.SuggestionRefined -> {
                        // The user's touch always wins (ADR-0006) — apply only if the category
                        // is still exactly the floor's own suggestion.
                        if (state.categoryId == intent.floorCategoryId) {
                            state.copy(categoryId = intent.refinedCategoryId)
                        } else {
                            state
                        }
                    }

                    is ConfirmUiIntent.AmountChanged -> {
                        val parsed = parseAmount(intent.text, state.currency)
                        val amount = parsed ?: state.amount
                        state.copy(
                            amountText = intent.text,
                            amount = amount,
                            amountConfidence = null,
                            lineItemsMismatch = hasMismatch(state.lineItems, amount),
                        )
                    }

                    is ConfirmUiIntent.DateChanged -> {
                        val parsed = runCatching { LocalDate.parse(intent.text) }.getOrNull()
                        state.copy(dateText = intent.text, date = parsed ?: state.date, dateConfidence = null)
                    }

                    is ConfirmUiIntent.CategorySelected -> {
                        state.copy(categoryId = intent.categoryId)
                    }

                    ConfirmUiIntent.SaveClicked -> {
                        state.copy(isSaving = true, error = null)
                    }

                    ConfirmUiIntent.Internal.ExpenseSaved -> {
                        state.copy(isSaving = false)
                    }

                    ConfirmUiIntent.Internal.ExpenseSaveFailed -> {
                        state.copy(isSaving = false, error = ConfirmError.SaveFailed)
                    }
                }

            private fun applyReceipt(
                state: ConfirmUiState,
                intent: ConfirmUiIntent.Internal.ReceiptLoaded,
            ): ConfirmUiState {
                val receipt = intent.receipt
                val currency =
                    receipt.total?.value?.currency
                        ?: receipt.lineItems
                            .firstOrNull()
                            ?.amount
                            ?.currency
                        ?: state.currency
                return state.copy(
                    isLoading = false,
                    receiptId = receipt.id,
                    merchant = receipt.merchant,
                    currency = currency,
                    amountText =
                        receipt.total
                            ?.value
                            ?.toDecimalString()
                            .orEmpty(),
                    amount = receipt.total?.value,
                    amountConfidence = receipt.total?.confidence,
                    dateText =
                        receipt.purchasedAt
                            ?.value
                            ?.toString()
                            .orEmpty(),
                    date = receipt.purchasedAt?.value,
                    dateConfidence = receipt.purchasedAt?.confidence,
                    categoryId = intent.suggestedCategoryId,
                    lineItems = receipt.lineItems,
                    lineItemsMismatch = hasMismatch(receipt.lineItems, receipt.total?.value),
                    error = null,
                )
            }

            /** Display-only per PROGRESS.md's roadmap note — never blocks [ConfirmUiState.canSave]. */
            private fun hasMismatch(
                lineItems: List<LineItem>,
                total: Money?,
            ): Boolean {
                if (total == null || lineItems.isEmpty()) return false
                if (lineItems.any { it.amount.currency != total.currency }) return false
                val sum = lineItems.map { it.amount }.reduce(Money::plus)
                return sum != total
            }

            private fun parseAmount(
                text: String,
                currency: CurrencyCode,
            ): Money? {
                val decimal = text.trim().toBigDecimalOrNull() ?: return null
                val scale = fractionDigitsOf(currency)
                val minorUnits =
                    runCatching {
                        decimal.movePointRight(scale).setScale(0, RoundingMode.HALF_UP).longValueExact()
                    }.getOrNull() ?: return null
                return Money(minorUnits, currency)
            }

            private fun Money.toDecimalString(): String {
                val scale = fractionDigitsOf(currency)
                return BigDecimal(minorUnits).movePointLeft(scale).toPlainString()
            }

            /** ISO-4217 fraction digits; an ISO-shaped code this JVM doesn't know reads as 2. */
            private fun fractionDigitsOf(currency: CurrencyCode): Int =
                try {
                    Currency.getInstance(currency.isoCode).defaultFractionDigits.coerceAtLeast(0)
                } catch (_: IllegalArgumentException) {
                    2
                }
        }
    }
