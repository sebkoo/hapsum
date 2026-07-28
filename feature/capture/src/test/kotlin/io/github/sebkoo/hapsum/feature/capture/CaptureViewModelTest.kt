package io.github.sebkoo.hapsum.feature.capture

import android.content.Context
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.data.ReceiptRepository
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
) : CameraCapture {
    override val surfaceRequests: Flow<SurfaceRequest> = emptyFlow()
    var capturedFile: File? = null
        private set

    override suspend fun bind(lifecycleOwner: LifecycleOwner) {}

    override suspend fun capturePhoto(outputFile: File) {
        capturedFile = outputFile
        captureError?.let { throw it }
    }
}

/**
 * Robolectric only for a real [Context.getFilesDir] — `capturePhoto` writes real receipt evidence
 * to app-private storage (ADR-0003), never a MockK-stubbed path. Camera hardware itself is
 * faked via [FakeCameraCapture]; [CameraXCapture] carries zero test coverage by design.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun TestScope.viewModel(
        cameraCapture: CameraCapture,
        receiptRepository: ReceiptRepository,
    ): CaptureViewModel =
        CaptureViewModel(
            appContext = context,
            cameraCapture = cameraCapture,
            receiptRepository = receiptRepository,
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
    fun `permission result — granted — state reflects it`() =
        runTest {
            val vm = viewModel(FakeCameraCapture(), mockk())

            vm.onIntent(CaptureUiIntent.PermissionResult(granted = true))
            advanceUntilIdle()

            assertEquals(true, vm.state.value.hasCameraPermission)
        }
}
