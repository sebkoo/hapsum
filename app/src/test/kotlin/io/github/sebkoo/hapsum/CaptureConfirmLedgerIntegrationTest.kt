package io.github.sebkoo.hapsum

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.ai.RuleBasedAiEngine
import io.github.sebkoo.hapsum.core.data.CategoryRepositoryImpl
import io.github.sebkoo.hapsum.core.data.ExpenseRepositoryImpl
import io.github.sebkoo.hapsum.core.data.HapsumDatabase
import io.github.sebkoo.hapsum.core.data.ReceiptRepositoryImpl
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.github.sebkoo.hapsum.feature.confirm.ConfirmUiIntent
import io.github.sebkoo.hapsum.feature.confirm.ConfirmViewModel
import io.github.sebkoo.hapsum.feature.ledger.LedgerViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}

/**
 * Real Room I/O crosses Room's own background executor, not the injected [DispatcherProvider] —
 * `advanceUntilIdle()` alone can return before that real work lands. Awaiting through Turbine
 * performs a genuine suspend-until-arrival instead of relying on virtual-time bookkeeping.
 */
private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}

/**
 * The executable version of the manual boot walkthrough (row 20): the loop — capture seeds a
 * Receipt, confirm turns it into an Expense, the ledger reads it back — is the product, so the
 * loop gets a test. Real Room in-memory database, real repositories, real `RuleBasedEngine`;
 * only the dispatcher is a test double. `:app` is the only module that already sees
 * `:feature:confirm`, `:feature:ledger`, and `:core:data` together — no new module for this row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureConfirmLedgerIntegrationTest {
    private val usd = CurrencyCode.of("USD")

    private fun newDatabase(): HapsumDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room
            .inMemoryDatabaseBuilder(context, HapsumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    /** One HIGH field (`purchasedAt`), one LOW field (`total`); merchant unparsed (null). */
    private fun seededReceipt(): Receipt =
        Receipt(
            id = ReceiptId("receipt-1"),
            imageRef = "fixture://receipt.jpg",
            ocrText = "WIDGET 4.00\nGADGET 5.00\nTOTAL 9.99",
            merchant = null,
            purchasedAt = ParsedField(LocalDate.of(2026, 3, 15), ParseConfidence.HIGH),
            total = ParsedField(Money(9_99, usd), ParseConfidence.LOW),
            lineItems =
                listOf(
                    LineItem(LineItemId("line-widget"), "Widget", Money(4_00, usd)),
                    LineItem(LineItemId("line-gadget"), "Gadget", Money(5_00, usd)),
                ),
        )

    @Test
    fun `capture to confirm to ledger — edited amount and chosen category — one Expense, receipt untouched`() =
        runTest {
            val db = newDatabase()
            val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
            val receiptRepository = ReceiptRepositoryImpl(db.receiptDao())
            val expenseRepository = ExpenseRepositoryImpl(db.expenseDao())
            val categoryRepository = CategoryRepositoryImpl(db.categoryDao())
            categoryRepository.seedDefaults()

            // Capture: a receipt with parsed evidence lands in the database, no Expense yet.
            val receipt = seededReceipt()
            receiptRepository.save(receipt)

            // Confirm: load, verify prefills, correct the LOW-confidence amount and the
            // suggested category (line-item text matches no RuleBasedEngine keyword, so the
            // engine floors to Uncategorized — the user's SHOPPING pick is a real correction).
            val confirmViewModel =
                ConfirmViewModel(
                    receiptRepository = receiptRepository,
                    expenseRepository = expenseRepository,
                    aiEngine = RuleBasedAiEngine(),
                    dispatchers = dispatchers,
                )

            confirmViewModel.state.test {
                awaitItem() // initial default state

                confirmViewModel.onIntent(ConfirmUiIntent.LoadReceipt(receipt.id))
                val loaded = awaitUntil { !it.isLoading }

                assertEquals(ParseConfidence.HIGH, loaded.dateConfidence)
                assertEquals(LocalDate.of(2026, 3, 15), loaded.date)
                assertEquals(ParseConfidence.LOW, loaded.amountConfidence)
                assertEquals(Money(9_99, usd), loaded.amount)
                assertTrue("line items ($4.00 + $5.00) mismatch the parsed total ($9.99)", loaded.lineItemsMismatch)
                assertTrue("the mismatch hint must never gate save", loaded.canSave)
                assertEquals(CategoryId.UNCATEGORIZED, loaded.categoryId)

                confirmViewModel.onIntent(ConfirmUiIntent.AmountChanged("12.50"))
                assertEquals(Money(12_50, usd), awaitItem().amount)

                confirmViewModel.onIntent(ConfirmUiIntent.CategorySelected(CategoryId("shopping")))
                assertEquals(CategoryId("shopping"), awaitItem().categoryId)

                confirmViewModel.onIntent(ConfirmUiIntent.SaveClicked)
                assertTrue("save must mark isSaving before the write completes", awaitItem().isSaving)
                val saved = awaitUntil { !it.isSaving }
                assertEquals(null, saved.error)

                cancelAndIgnoreRemainingEvents()
            }

            // The Receipt is immutable evidence — confirm must never write back to it.
            assertEquals(receipt, receiptRepository.getById(receipt.id))

            // Ledger: exactly one Expense, carrying the correction (not the engine's
            // suggestion), the edited amount, and the receipt linkage. lineItemId stays null —
            // row 19 writes one receipt-level Expense by design (split transactions is backlog).
            val ledgerViewModel = LedgerViewModel(repository = expenseRepository, dispatchers = dispatchers)
            ledgerViewModel.state.test {
                val loadedLedger = awaitUntil { !it.isLoading }

                assertEquals(null, loadedLedger.error)
                assertEquals(1, loadedLedger.rows.size)
                val row = loadedLedger.rows.single()
                assertEquals(Money(12_50, usd), row.expense.amount)
                assertEquals(CategoryId("shopping"), row.expense.categoryId)
                assertEquals("Shopping", row.category.name)
                assertEquals(receipt.id, row.expense.receiptId)
                assertNull(row.expense.lineItemId)
                assertEquals(LocalDate.of(2026, 3, 15), row.expense.date)

                cancelAndIgnoreRemainingEvents()
            }

            db.close()
        }
}
