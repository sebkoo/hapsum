package io.github.sebkoo.hapsum.feature.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Real CameraX session — one instance per [CaptureViewModel] (Hilt `ViewModelComponent` scope). */
class CameraXCapture
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : CameraCapture {
        private val latestSurfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
        override val surfaceRequests: Flow<SurfaceRequest> = latestSurfaceRequest.filterNotNull()

        private val previewUseCase =
            Preview.Builder().build().apply {
                setSurfaceProvider { request -> latestSurfaceRequest.value = request }
            }
        private val imageCaptureUseCase = ImageCapture.Builder().build()

        override suspend fun bind(lifecycleOwner: LifecycleOwner) {
            val provider = awaitCameraProvider()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                imageCaptureUseCase,
            )
            try {
                awaitCancellation()
            } finally {
                provider.unbindAll()
            }
        }

        private suspend fun awaitCameraProvider(): ProcessCameraProvider =
            suspendCancellableCoroutine { continuation ->
                val future = ProcessCameraProvider.getInstance(context)
                // Harmless for this specific singleton future (there is nothing to actually
                // interrupt), but the wrapper is a pattern future ListenableFuture call sites
                // will copy, so it models the correct shape: cancellation propagates outward.
                continuation.invokeOnCancellation { future.cancel(false) }
                future.addListener(
                    { continuation.resume(future.get()) },
                    ContextCompat.getMainExecutor(context),
                )
            }

        override suspend fun capturePhoto(outputFile: File) {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    imageCaptureUseCase.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                continuation.resume(Unit)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                continuation.resumeWithException(exc)
                            }
                        },
                    )
                }
            } catch (exc: ImageCaptureException) {
                throw IOException("Photo capture failed", exc)
            }
        }
    }
