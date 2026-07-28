package io.github.sebkoo.hapsum.feature.capture

import androidx.camera.core.SurfaceRequest
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.testing.ReducerTestHarness
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JVM, no Android, no coroutines — the reducer under test IS the production reducer. */
class CaptureReducerTest {
    private val harness =
        ReducerTestHarness(
            initialState = CaptureUiState(),
            reducer = CaptureViewModel.reducer,
        )

    @Test
    fun `reduce — permission granted — sets hasCameraPermission`() {
        val state = harness.after(CaptureUiIntent.PermissionResult(granted = true))

        assertEquals(CaptureUiState(hasCameraPermission = true), state)
    }

    @Test
    fun `reduce — permission revoked after being granted — clears hasCameraPermission`() {
        val granted = harness.after(CaptureUiIntent.PermissionResult(granted = true))

        val state = harness.after(granted, CaptureUiIntent.PermissionResult(granted = false))

        assertEquals(CaptureUiState(hasCameraPermission = false), state)
    }

    @Test
    fun `reduce — surface request available — stores it in state`() {
        val request = mockk<SurfaceRequest>(relaxed = true)

        val state = harness.after(CaptureUiIntent.Internal.SurfaceRequestAvailable(request))

        assertEquals(CaptureUiState(surfaceRequest = request), state)
    }

    @Test
    fun `reduce — capture photo clicked — marks saving and clears a prior error`() {
        val failed = harness.after(CaptureUiIntent.Internal.PhotoCaptureFailed)

        val state = harness.after(failed, CaptureUiIntent.CapturePhotoClicked)

        assertEquals(CaptureUiState(isSaving = true, error = null), state)
    }

    @Test
    fun `reduce — photo capture failed — sealed error, stops saving`() {
        val state = harness.after(CaptureUiIntent.Internal.PhotoCaptureFailed)

        assertEquals(CaptureUiState(isSaving = false, error = CaptureError.CaptureFailed), state)
    }

    @Test
    fun `reduce — receipt saved — clears saving and any error`() {
        val failed = harness.after(CaptureUiIntent.Internal.PhotoCaptureFailed)

        val state =
            harness.after(failed, CaptureUiIntent.Internal.ReceiptSaved(ReceiptId("fixture-1")))

        assertEquals(CaptureUiState(isSaving = false, error = null), state)
    }

    @Test
    fun `reduce — receipt save failed — sealed error, stops saving`() {
        val state = harness.after(CaptureUiIntent.Internal.ReceiptSaveFailed)

        assertEquals(CaptureUiState(isSaving = false, error = CaptureError.SaveFailed), state)
    }
}
