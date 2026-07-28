package io.github.sebkoo.hapsum.feature.capture

import androidx.camera.core.SurfaceRequest
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.mvi.InternalUiIntent
import io.github.sebkoo.hapsum.core.mvi.UiEffect
import io.github.sebkoo.hapsum.core.mvi.UiIntent
import io.github.sebkoo.hapsum.core.mvi.UiState

data class CaptureUiState(
    val hasCameraPermission: Boolean = false,
    val surfaceRequest: SurfaceRequest? = null,
    val isSaving: Boolean = false,
    val error: CaptureError? = null,
    /** Bumped by [CaptureUiIntent.RetryBindClicked] — the key that re-triggers `bindCamera`. */
    val bindAttempt: Int = 0,
) : UiState

/** Sealed, never a raw String (ADR-0004); grows variants as failure modes become real. */
sealed interface CaptureError {
    data object CaptureFailed : CaptureError

    data object SaveFailed : CaptureError

    /** Binding the camera session itself failed — no camera available, or one already in use. */
    data object BindFailed : CaptureError
}

sealed interface CaptureUiIntent : UiIntent {
    data class PermissionResult(
        val granted: Boolean,
    ) : CaptureUiIntent

    data object CapturePhotoClicked : CaptureUiIntent

    /** User-triggered retry after [CaptureError.BindFailed] — bumps `bindAttempt` to rebind. */
    data object RetryBindClicked : CaptureUiIntent

    /** Async results re-entering the reducer — unforgeable from the UI (ADR-0004). */
    sealed interface Internal :
        CaptureUiIntent,
        InternalUiIntent {
        data class SurfaceRequestAvailable(
            val request: SurfaceRequest,
        ) : Internal

        data class PhotoSaved(
            val receiptId: ReceiptId,
            val imageRef: String,
        ) : Internal

        data object PhotoCaptureFailed : Internal

        data class ReceiptSaved(
            val receiptId: ReceiptId,
        ) : Internal

        data object ReceiptSaveFailed : Internal

        data object BindFailed : Internal
    }
}

/** The capture screen's first — and the app's first — effect: the ledger consumes it in `:app`. */
sealed interface CaptureUiEffect : UiEffect {
    data class ReceiptCaptured(
        val receiptId: ReceiptId,
    ) : CaptureUiEffect
}
