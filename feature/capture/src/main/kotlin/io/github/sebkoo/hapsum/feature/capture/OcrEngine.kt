package io.github.sebkoo.hapsum.feature.capture

import java.io.File

/**
 * Seams ML Kit text recognition behind an interface, the same trade [CameraCapture] makes for
 * the camera session: [CaptureViewModel]'s recognize → parse → save orchestration stays
 * unit-testable, and [MlKitOcrEngine] — the only implementation — carries zero test coverage
 * by design. Golden tests target [parseReceipt] with committed synthetic OCR text, never
 * image→parse end-to-end, which would pin the test to a device and an ML Kit model version.
 */
interface OcrEngine {
    /**
     * Recognizes the text in [imageFile]. Throws [java.io.IOException] when the image cannot
     * be read; recognition failures propagate as the engine's own exceptions.
     */
    suspend fun recognize(imageFile: File): OcrText
}
