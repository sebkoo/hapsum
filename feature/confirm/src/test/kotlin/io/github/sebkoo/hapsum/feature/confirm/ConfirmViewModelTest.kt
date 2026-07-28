package io.github.sebkoo.hapsum.feature.confirm

import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.ai.AiEngine
import io.github.sebkoo.hapsum.core.ai.CategorizationEvidence
import io.github.sebkoo.hapsum.core.ai.RuleBasedAiEngine
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.data.ReceiptRepository
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Expense
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}

class ConfirmViewModelTest {
    private val usd = CurrencyCode.of("USD")

    private fun TestScope.viewModel(
        receiptRepository: ReceiptRepository,
        expenseRepository: ExpenseRepository = mockk(),
        aiEngine: AiEngine = RuleBasedAiEngine(),
    ): ConfirmViewModel =
        ConfirmViewModel(
            receiptRepository = receiptRepository,
            expenseRepository = expenseRepository,
            aiEngine = aiEngine,
            dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

    private fun fixtureReceipt(): Receipt =
        Receipt(
            id = ReceiptId("r1"),
            imageRef = "fixture://receipt.jpg",
            ocrText = "SYNTH CAFE\nCoffee 4.50\nTOTAL 4.50",
            merchant = ParsedField("SYNTH CAFE", ParseConfidence.LOW),
            purchasedAt = ParsedField(LocalDate.of(2026, 1, 1), ParseConfidence.HIGH),
            total = ParsedField(Money(4_50, usd), ParseConfidence.HIGH),
            lineItems = listOf(LineItem(LineItemId("li-1"), "Coffee", Money(4_50, usd))),
        )

    @Test
    fun `load receipt — repository resolves it — state carries the parsed fields and the engine's suggestion`() =
        runTest {
            val receiptRepository =
                mockk<ReceiptRepository> {
                    coEvery { getById(ReceiptId("r1")) } returns
                        fixtureReceipt()
                }
            val vm = viewModel(receiptRepository)

            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            advanceUntilIdle()

            assertEquals(false, vm.state.value.isLoading)
            assertEquals(Money(4_50, usd), vm.state.value.amount)
            assertEquals(LocalDate.of(2026, 1, 1), vm.state.value.date)
            // Merchant "SYNTH CAFE" + line item "Coffee" both hit DEFAULT_RULES' DINING keywords.
            assertEquals(CategoryId("dining"), vm.state.value.categoryId)
        }

    @Test
    fun `load receipt — merchant text matches a rule keyword — category prefills from RuleBasedEngine`() =
        runTest {
            val receipt = fixtureReceipt().copy(merchant = ParsedField("SYNTH MARKET", ParseConfidence.LOW))
            val receiptRepository = mockk<ReceiptRepository> { coEvery { getById(ReceiptId("r1")) } returns receipt }
            val vm = viewModel(receiptRepository)

            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            advanceUntilIdle()

            assertEquals(CategoryId("groceries"), vm.state.value.categoryId)
        }

    @Test
    fun `load receipt — chain refines the floor's suggestion, category untouched — refined category applies`() =
        runTest {
            val receiptRepository =
                mockk<ReceiptRepository> { coEvery { getById(ReceiptId("r1")) } returns fixtureReceipt() }
            // Floor prefill is dining (fixtureReceipt's merchant/items hit DINING keywords) — the
            // chain "improves" it to shopping, simulating a Nano-capable device.
            val aiEngine =
                mockk<AiEngine> {
                    coEvery { categorize(any<CategorizationEvidence>()) } returns
                        CategoryId("shopping")
                }
            val vm = viewModel(receiptRepository, aiEngine = aiEngine)

            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            advanceUntilIdle()

            assertEquals(CategoryId("shopping"), vm.state.value.categoryId)
        }

    @Test
    fun `load receipt — user picks a category before the chain answers — the user's touch survives`() =
        runTest {
            val receiptRepository =
                mockk<ReceiptRepository> { coEvery { getById(ReceiptId("r1")) } returns fixtureReceipt() }
            // A real suspension point (unlike a same-tick mocked return) opens a genuine gap
            // between the floor's phase-1 dispatch and the chain's phase-2 dispatch to correct
            // the category into, the same way a real Nano inference call would suspend.
            val aiEngine =
                mockk<AiEngine> {
                    coEvery { categorize(any<CategorizationEvidence>()) } coAnswers {
                        delay(1_000)
                        CategoryId("shopping")
                    }
                }
            val vm = viewModel(receiptRepository, aiEngine = aiEngine)

            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            runCurrent()
            assertEquals(CategoryId("dining"), vm.state.value.categoryId)

            vm.onIntent(ConfirmUiIntent.CategorySelected(CategoryId("transport")))
            advanceUntilIdle()

            assertEquals(CategoryId("transport"), vm.state.value.categoryId)
        }

    @Test
    fun `load receipt — repository finds nothing — sealed load-failed error`() =
        runTest {
            val receiptRepository = mockk<ReceiptRepository> { coEvery { getById(any()) } returns null }
            val vm = viewModel(receiptRepository)

            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("missing")))
            advanceUntilIdle()

            assertEquals(ConfirmError.LoadFailed, vm.state.value.error)
        }

    @Test
    fun `load receipt — repository throws — sealed load-failed error, no crash`() =
        runTest {
            val receiptRepository = mockk<ReceiptRepository> { coEvery { getById(any()) } throws IOException("disk") }
            val vm = viewModel(receiptRepository)

            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            advanceUntilIdle()

            assertEquals(ConfirmError.LoadFailed, vm.state.value.error)
        }

    @Test
    fun `save clicked — amount and date resolved — writes an Expense with lineItemId null and emits Saved`() =
        runTest {
            val receiptRepository =
                mockk<ReceiptRepository> {
                    coEvery { getById(ReceiptId("r1")) } returns
                        fixtureReceipt()
                }
            val saved = slot<Expense>()
            val expenseRepository = mockk<ExpenseRepository> { coEvery { addExpense(capture(saved)) } returns Unit }
            val vm = viewModel(receiptRepository, expenseRepository)
            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            advanceUntilIdle()

            vm.effects.test {
                vm.onIntent(ConfirmUiIntent.SaveClicked)
                advanceUntilIdle()

                assertEquals(ConfirmUiEffect.Saved, awaitItem())
            }
            assertEquals(Money(4_50, usd), saved.captured.amount)
            assertEquals(LocalDate.of(2026, 1, 1), saved.captured.date)
            assertEquals(ReceiptId("r1"), saved.captured.receiptId)
            assertNull(saved.captured.lineItemId)
        }

    @Test
    fun `save clicked — repository throws — sealed save-failed error, not saving`() =
        runTest {
            val receiptRepository =
                mockk<ReceiptRepository> {
                    coEvery { getById(ReceiptId("r1")) } returns
                        fixtureReceipt()
                }
            val expenseRepository =
                mockk<ExpenseRepository> {
                    coEvery { addExpense(any()) } throws
                        IOException("disk full")
                }
            val vm = viewModel(receiptRepository, expenseRepository)
            vm.onIntent(ConfirmUiIntent.LoadReceipt(ReceiptId("r1")))
            advanceUntilIdle()

            vm.onIntent(ConfirmUiIntent.SaveClicked)
            advanceUntilIdle()

            assertEquals(false, vm.state.value.isSaving)
            assertEquals(ConfirmError.SaveFailed, vm.state.value.error)
        }

    @Test
    fun `save clicked — no receipt loaded yet — amount and date are null, canSave is false`() =
        runTest {
            val vm = viewModel(mockk())

            assertTrue(!vm.state.value.canSave)
        }
}
