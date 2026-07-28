package io.github.sebkoo.hapsum.feature.capture

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Latin-script text recognition with the bundled on-device model — recognition works
 * offline from the first capture, no dynamic model download (offline-first). Blocks and lines
 * flatten to [OcrText] in ML Kit's reading order. The Tasks API is wrapped with a
 * `suspendCancellableCoroutine` the same way [CameraXCapture] wraps `ProcessCameraProvider`'s
 * `ListenableFuture` — one call site does not buy the kotlinx-coroutines-play-services
 * dependency. Never unit-tested: platform glue, not project logic — the [CameraXCapture] trade.
 */
internal class MlKitOcrEngine(
    private val context: Context,
) : OcrEngine {
    override suspend fun recognize(imageFile: File): OcrText {
        val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = recognizer.process(image).awaitResult()
            return OcrText(text.textBlocks.flatMap { block -> block.lines }.map { line -> line.text })
        } finally {
            recognizer.close()
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }
