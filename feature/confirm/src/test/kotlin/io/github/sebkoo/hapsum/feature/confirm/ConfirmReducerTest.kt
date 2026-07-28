package io.github.sebkoo.hapsum.feature.confirm

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.testing.ReducerTestHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Plain JVM, no Android, no coroutines — the reducer under test IS the production reducer. */
class ConfirmReducerTest {
    private val harness =
        ReducerTestHarness(
            initialState = ConfirmUiState(),
            reducer = ConfirmViewModel.reducer,
        )
    private val usd = CurrencyCode.of("USD")

    private fun receipt(
        total: ParsedField<Money>? = ParsedField(Money(5_50, usd), ParseConfidence.HIGH),
        lineItems: List<LineItem> =
            listOf(
                LineItem(LineItemId("li-1"), "Milk", Money(2_50, usd)),
                LineItem(LineItemId("li-2"), "Bread", Money(3_00, usd)),
            ),
        purchasedAt: ParsedField<LocalDate>? = ParsedField(LocalDate.of(2026, 1, 1), ParseConfidence.HIGH),
        merchant: ParsedField<String>? = ParsedField("SYNTH MART", ParseConfidence.LOW),
    ): Receipt =
        Receipt(
            id = ReceiptId("r1"),
            imageRef = "fixture://receipt.jpg",
            ocrText = "",
            merchant = merchant,
            purchasedAt = purchasedAt,
            total = total,
            lineItems = lineItems,
        )

    @Test
    fun `reduce — load receipt — starts loading with the requested id, clears a prior error`() {
        val failed = harness.after(ConfirmUiIntent.Internal.ReceiptLoadFailed)

        val state = harness.after(failed, ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))

        assertTrue(state.isLoading)
        assertEquals(ReceiptId("r1"), state.receiptId)
        assertEquals(null, state.error)
    }

    @Test
    fun `reduce — receipt loaded — prefills amount, date, category, currency from parsed fields`() {
        val state =
            harness.after(
                ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId("groceries")),
            )

        assertEquals(false, state.isLoading)
        assertEquals(Money(5_50, usd), state.amount)
        assertEquals("5.50", state.amountText)
        assertEquals(ParseConfidence.HIGH, state.amountConfidence)
        assertEquals(LocalDate.of(2026, 1, 1), state.date)
        assertEquals(CategoryId("groceries"), state.categoryId)
        assertEquals(usd, state.currency)
        assertEquals(2, state.lineItems.size)
    }

    @Test
    fun `reduce — receipt loaded — line items sum to the total — no mismatch`() {
        val state =
            harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId.UNCATEGORIZED))

        assertFalse(state.lineItemsMismatch)
    }

    @Test
    fun `reduce — receipt loaded — line items don't sum to the total — mismatch flagged`() {
        val mismatched = receipt(total = ParsedField(Money(6_00, usd), ParseConfidence.HIGH))

        val state = harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(mismatched, CategoryId.UNCATEGORIZED))

        assertTrue(state.lineItemsMismatch)
    }

    @Test
    fun `reduce — receipt loaded — all fields null — renders empty, no mismatch, no crash`() {
        val bare = receipt(total = null, lineItems = emptyList(), purchasedAt = null, merchant = null)

        val state = harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(bare, CategoryId.UNCATEGORIZED))

        assertEquals(null, state.amount)
        assertEquals("", state.amountText)
        assertEquals(null, state.date)
        assertEquals("", state.dateText)
        assertEquals(null, state.merchant)
        assertFalse(state.lineItemsMismatch)
        assertFalse(state.canSave)
    }

    @Test
    fun `reduce — receipt load failed — sealed error, stops loading`() {
        val state = harness.after(ConfirmUiIntent.Internal.ReceiptLoadFailed)

        assertEquals(ConfirmUiState(isLoading = false, error = ConfirmError.LoadFailed), state)
    }

    @Test
    fun `reduce — amount changed to a valid decimal — updates amount and re-checks the mismatch hint`() {
        val loaded = harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId.UNCATEGORIZED))

        val state = harness.after(loaded, ConfirmUiIntent.AmountChanged("6.00"))

        assertEquals(Money(6_00, usd), state.amount)
        assertEquals("6.00", state.amountText)
        assertEquals(null, state.amountConfidence)
        assertTrue(state.lineItemsMismatch)
    }

    @Test
    fun `reduce — amount changed to unparseable text — text updates but the last valid amount survives`() {
        val loaded = harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId.UNCATEGORIZED))

        val state = harness.after(loaded, ConfirmUiIntent.AmountChanged("not a number"))

        assertEquals("not a number", state.amountText)
        assertEquals(Money(5_50, usd), state.amount)
    }

    @Test
    fun `reduce — date changed to a valid ISO date — updates date`() {
        val state = harness.after(ConfirmUiIntent.DateChanged("2026-02-14"))

        assertEquals(LocalDate.of(2026, 2, 14), state.date)
        assertEquals("2026-02-14", state.dateText)
    }

    @Test
    fun `reduce — date changed to unparseable text — text updates but the date stays null`() {
        val state = harness.after(ConfirmUiIntent.DateChanged("not a date"))

        assertEquals("not a date", state.dateText)
        assertEquals(null, state.date)
    }

    @Test
    fun `reduce — category selected — replaces the suggested category`() {
        val loaded =
            harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId("groceries")))

        val state = harness.after(loaded, ConfirmUiIntent.CategorySelected(CategoryId("dining")))

        assertEquals(CategoryId("dining"), state.categoryId)
    }

    @Test
    fun `reduce — save clicked — marks saving and clears a prior error`() {
        val state = harness.after(ConfirmUiIntent.SaveClicked)

        assertTrue(state.isSaving)
        assertEquals(null, state.error)
    }

    @Test
    fun `reduce — expense saved — clears saving`() {
        val saving = harness.after(ConfirmUiIntent.SaveClicked)

        val state = harness.after(saving, ConfirmUiIntent.Internal.ExpenseSaved)

        assertFalse(state.isSaving)
    }

    @Test
    fun `reduce — expense save failed — sealed error, stops saving`() {
        val saving = harness.after(ConfirmUiIntent.SaveClicked)

        val state = harness.after(saving, ConfirmUiIntent.Internal.ExpenseSaveFailed)

        assertEquals(false, state.isSaving)
        assertEquals(ConfirmError.SaveFailed, state.error)
    }

    @Test
    fun `canSave — amount and date resolved, not currently saving — true`() {
        val state = harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId.UNCATEGORIZED))

        assertTrue(state.canSave)
    }

    @Test
    fun `canSave — currently saving — false even with amount and date resolved`() {
        val loaded = harness.after(ConfirmUiIntent.Internal.ReceiptLoaded(receipt(), CategoryId.UNCATEGORIZED))

        val state = harness.after(loaded, ConfirmUiIntent.SaveClicked)

        assertFalse(state.canSave)
    }
}
