package io.github.sebkoo.hapsum.feature.capture

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sebkoo.hapsum.core.data.ReceiptRepository
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import io.github.sebkoo.hapsum.core.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
        private val cameraCapture: CameraCapture,
        private val ocrEngine: OcrEngine,
        private val receiptRepository: ReceiptRepository,
        private val receiptCurrency: ReceiptCurrencyResolver,
        private val dispatchers: DispatcherProvider,
    ) : MviViewModel<CaptureUiState, CaptureUiIntent, CaptureUiEffect>(
            initialState = CaptureUiState(),
            reducer = reducer,
        ) {
        init {
            viewModelScope.launch(dispatchers.main) {
                cameraCapture.surfaceRequests.collect { request ->
                    dispatch(CaptureUiIntent.Internal.SurfaceRequestAvailable(request))
                }
            }
        }

        /** Called from a `LaunchedEffect` tied to the screen's own lifecycle — never from [react]. */
        suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
            cameraCapture.bind(lifecycleOwner)
        }

        override fun react(
            intent: CaptureUiIntent,
            state: CaptureUiState,
        ) {
            when (intent) {
                CaptureUiIntent.CapturePhotoClicked -> {
                    capturePhoto()
                }

                is CaptureUiIntent.Internal.PhotoSaved -> {
                    recognizeAndSaveReceipt(intent.receiptId, intent.imageRef)
                }

                is CaptureUiIntent.Internal.ReceiptSaved -> {
                    sendEffect(CaptureUiEffect.ReceiptCaptured(intent.receiptId))
                }

                else -> {}
            }
        }

        private fun capturePhoto() {
            viewModelScope.launch(dispatchers.io) {
                val receiptId = ReceiptId(UUID.randomUUID().toString())
                val imageRef = "receipts/${receiptId.value}.jpg"
                val file = File(appContext.filesDir, imageRef)
                file.parentFile?.mkdirs()
                try {
                    cameraCapture.capturePhoto(file)
                    dispatch(CaptureUiIntent.Internal.PhotoSaved(receiptId, imageRef))
                } catch (_: IOException) {
                    dispatch(CaptureUiIntent.Internal.PhotoCaptureFailed)
                }
            }
        }

        private fun recognizeAndSaveReceipt(
            receiptId: ReceiptId,
            imageRef: String,
        ) {
            viewModelScope.launch(dispatchers.io) {
                val ocrText = recognizeOrEmpty(File(appContext.filesDir, imageRef))
                val parsed = parseReceipt(ocrText, receiptCurrency.resolve())
                val receipt =
                    Receipt(
                        id = receiptId,
                        imageRef = imageRef,
                        ocrText = ocrText.raw,
                        merchant = parsed.merchant,
                        purchasedAt = parsed.purchasedAt,
                        total = parsed.total,
                        // Id assignment is the effectful step the pure parser leaves to this flow.
                        lineItems =
                            parsed.lineItems.map { item ->
                                LineItem(
                                    id = LineItemId(UUID.randomUUID().toString()),
                                    description = item.description,
                                    amount = item.amount,
                                )
                            },
                    )
                try {
                    receiptRepository.save(receipt)
                    dispatch(CaptureUiIntent.Internal.ReceiptSaved(receiptId))
                } catch (_: IOException) {
                    dispatch(CaptureUiIntent.Internal.ReceiptSaveFailed)
                }
            }
        }

        /**
         * OCR is best-effort by design: the JPEG evidence is already on disk, so a recognition
         * failure degrades to an unparsed receipt — empty text, all-null fields, what the future
         * confirm screen renders as "fill everything in" — never a lost capture.
         */
        private suspend fun recognizeOrEmpty(imageFile: File): OcrText =
            try {
                ocrEngine.recognize(imageFile)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                OcrText(emptyList())
            }

        companion object {
            /** The screen's single reduction path — the exact value ReducerTestHarness folds with. */
            val reducer: (CaptureUiState, CaptureUiIntent) -> CaptureUiState = ::reduce

            private fun reduce(
                state: CaptureUiState,
                intent: CaptureUiIntent,
            ): CaptureUiState =
                when (intent) {
                    is CaptureUiIntent.PermissionResult -> {
                        state.copy(hasCameraPermission = intent.granted)
                    }

                    CaptureUiIntent.CapturePhotoClicked -> {
                        state.copy(isSaving = true, error = null)
                    }

                    is CaptureUiIntent.Internal.SurfaceRequestAvailable -> {
                        state.copy(surfaceRequest = intent.request)
                    }

                    is CaptureUiIntent.Internal.PhotoSaved -> {
                        state
                    }

                    CaptureUiIntent.Internal.PhotoCaptureFailed -> {
                        state.copy(isSaving = false, error = CaptureError.CaptureFailed)
                    }

                    is CaptureUiIntent.Internal.ReceiptSaved -> {
                        state.copy(isSaving = false, error = null)
                    }

                    CaptureUiIntent.Internal.ReceiptSaveFailed -> {
                        state.copy(isSaving = false, error = CaptureError.SaveFailed)
                    }
                }
        }
    }
