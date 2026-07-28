package io.github.sebkoo.hapsum.feature.capture

import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Seams the CameraX session behind an interface so [CaptureViewModel]'s orchestration logic
 * (capture → save → effect) is unit-testable without real camera hardware — the same role
 * `ExpenseRepository`/`DispatcherProvider` play elsewhere (ADR-0004). [CameraXCapture] is the
 * only implementation and is never exercised by a unit test; binding a physical camera is
 * platform glue, not project logic.
 */
interface CameraCapture {
    /** One preview frame request per camera bind — [CaptureUiState.surfaceRequest] mirrors this. */
    val surfaceRequests: Flow<SurfaceRequest>

    /** Suspends for as long as the camera should stay bound; unbinds on cancellation. */
    suspend fun bind(lifecycleOwner: LifecycleOwner)

    /** Writes a JPEG to [outputFile]. Throws [java.io.IOException] on failure. */
    suspend fun capturePhoto(outputFile: File)
}
