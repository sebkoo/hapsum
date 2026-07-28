package io.github.sebkoo.hapsum.feature.capture

import android.content.Context
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.data.ReceiptRepository
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}

/** Never exercises real camera hardware — [CameraXCapture] is the only untested piece (ADR). */
private class FakeCameraCapture(
    private val captureError: IOException? = null,
    private val bindError: Exception? = null,
) : CameraCapture {
    override val surfaceRequests: Flow<SurfaceRequest> = emptyFlow()
    var capturedFile: File? = null
        private set

    override suspend fun bind(lifecycleOwner: LifecycleOwner) {
        bindError?.let { throw it }
    }

    override suspend fun capturePhoto(outputFile: File) {
        capturedFile = outputFile
        captureError?.let { throw it }
    }
}

/** Never exercises ML Kit — [MlKitOcrEngine] is platform glue with zero coverage by design. */
private class FakeOcrEngine(
    private val result: OcrText = OcrText(emptyList()),
    private val error: Exception? = null,
) : OcrEngine {
    override suspend fun recognize(imageFile: File): OcrText {
        error?.let { throw it }
        return result
    }
}

/**
 * Robolectric only for a real [Context.getFilesDir] — `capturePhoto` writes real receipt evidence
 * to app-private storage (ADR-0003), never a MockK-stubbed path. Camera hardware and ML Kit are
 * faked via [FakeCameraCapture]/[FakeOcrEngine]; their CameraX/ML Kit implementations carry zero
 * test coverage by design.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val usd = CurrencyCode.of("USD")

    private fun TestScope.viewModel(
        cameraCapture: CameraCapture,
        receiptRepository: ReceiptRepository,
        ocrEngine: OcrEngine = FakeOcrEngine(),
    ): CaptureViewModel =
        CaptureViewModel(
            appContext = context,
            cameraCapture = cameraCapture,
            ocrEngine = ocrEngine,
            receiptRepository = receiptRepository,
            receiptCurrency = ReceiptCurrencyResolver { usd },
            dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

    @Test
    fun `capture photo clicked — camera and repository succeed — emits ReceiptCaptured`() =
        runTest {
            val repository = mockk<ReceiptRepository> { coEvery { save(any()) } returns Unit }
            val vm = viewModel(FakeCameraCapture(), repository)

            vm.effects.test {
                vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
                advanceUntilIdle()

                assertTrue(awaitItem() is CaptureUiEffect.ReceiptCaptured)
            }
        }

    @Test
    fun `capture photo clicked — writes the JPEG under filesDir slash receipts`() =
        runTest {
            val repository = mockk<ReceiptRepository> { coEvery { save(any()) } returns Unit }
            val cameraCapture = FakeCameraCapture()
            val vm = viewModel(cameraCapture, repository)

            vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
            advanceUntilIdle()

            val file = cameraCapture.capturedFile
            assertEquals(File(context.filesDir, "receipts"), file?.parentFile)
            assertTrue(file!!.name.endsWith(".jpg"))
        }

    @Test
    fun `capture photo clicked — ocr text parses — saved receipt carries text, header fields, line items`() =
        runTest {
            val saved = slot<Receipt>()
            val repository = mockk<ReceiptRepository> { coEvery { save(capture(saved)) } returns Unit }
            val ocrEngine = FakeOcrEngine(OcrText(listOf("SYNTH CAFE", "Coffee 4.50", "TOTAL 4.50")))
            val vm = viewModel(FakeCameraCapture(), repository, ocrEngine)

            vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
            advanceUntilIdle()

            val receipt = saved.captured
            assertEquals("SYNTH CAFE\nCoffee 4.50\nTOTAL 4.50", receipt.ocrText)
            assertEquals(ParsedField("SYNTH CAFE", ParseConfidence.LOW), receipt.merchant)
            assertEquals(ParsedField(Money(4_50, usd), ParseConfidence.HIGH), receipt.total)
            assertEquals(listOf("Coffee"), receipt.lineItems.map { it.description })
            assertEquals(listOf(Money(4_50, usd)), receipt.lineItems.map { it.amount })
        }

    @Test
    fun `capture photo clicked — ocr engine throws — receipt still saved unparsed, capture not lost`() =
        runTest {
            val saved = slot<Receipt>()
            val repository = mockk<ReceiptRepository> { coEvery { save(capture(saved)) } returns Unit }
            val ocrEngine = FakeOcrEngine(error = IOException("unreadable image"))
            val vm = viewModel(FakeCameraCapture(), repository, ocrEngine)

            vm.effects.test {
                vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
                advanceUntilIdle()

                assertTrue(awaitItem() is CaptureUiEffect.ReceiptCaptured)
            }
            val receipt = saved.captured
            assertEquals("", receipt.ocrText)
            assertNull(receipt.merchant)
            assertNull(receipt.purchasedAt)
            assertNull(receipt.total)
            assertEquals(emptyList<Any>(), receipt.lineItems)
        }

    @Test
    fun `capture photo clicked — camera capture throws — sealed capture-failed error, not saving`() =
        runTest {
            val vm = viewModel(FakeCameraCapture(IOException("camera busy")), mockk())

            vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
            advanceUntilIdle()

            assertEquals(
                CaptureUiState(isSaving = false, error = CaptureError.CaptureFailed),
                vm.state.value,
            )
        }

    @Test
    fun `capture photo clicked — repository save throws — sealed save-failed error`() =
        runTest {
            val repository =
                mockk<ReceiptRepository> {
                    coEvery { save(any()) } throws IOException("disk full")
                }
            val vm = viewModel(FakeCameraCapture(), repository)

            vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
            advanceUntilIdle()

            assertEquals(
                CaptureUiState(isSaving = false, error = CaptureError.SaveFailed),
                vm.state.value,
            )
        }

    @Test
    fun `capture photo clicked — repository save throws a non-IOException — still sealed save-failed, no crash`() =
        runTest {
            val repository =
                mockk<ReceiptRepository> {
                    coEvery { save(any()) } throws IllegalStateException("disk full")
                }
            val vm = viewModel(FakeCameraCapture(), repository)

            vm.onIntent(CaptureUiIntent.CapturePhotoClicked)
            advanceUntilIdle()

            assertEquals(
                CaptureUiState(isSaving = false, error = CaptureError.SaveFailed),
                vm.state.value,
            )
        }

    @Test
    fun `bindCamera — no camera available — sealed bind-failed error, exception does not escape`() =
        runTest {
            val vm = viewModel(FakeCameraCapture(bindError = IllegalStateException("no camera")), mockk())

            vm.bindCamera(mockk<LifecycleOwner>(relaxed = true))
            advanceUntilIdle()

            assertEquals(CaptureUiState(error = CaptureError.BindFailed), vm.state.value)
        }

    @Test
    fun `retry bind clicked — clears the bind error, bindCamera called again succeeds`() =
        runTest {
            val vm = viewModel(FakeCameraCapture(bindError = IllegalStateException("no camera")), mockk())
            vm.bindCamera(mockk<LifecycleOwner>(relaxed = true))
            advanceUntilIdle()
            assertEquals(CaptureError.BindFailed, vm.state.value.error)

            vm.onIntent(CaptureUiIntent.RetryBindClicked)
            advanceUntilIdle()

            assertEquals(null, vm.state.value.error)
            assertEquals(1, vm.state.value.bindAttempt)
        }

    @Test
    fun `permission result — granted — state reflects it`() =
        runTest {
            val vm = viewModel(FakeCameraCapture(), mockk())

            vm.onIntent(CaptureUiIntent.PermissionResult(granted = true))
            advanceUntilIdle()

            assertEquals(true, vm.state.value.hasCameraPermission)
        }
}
