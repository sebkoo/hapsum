package io.github.sebkoo.hapsum.feature.capture

/**
 * The OCR engine's output in reading order, one entry per visual line — the deterministic
 * parser's entire input. This structure is the determinism boundary: everything device- and
 * model-version-dependent stays behind [OcrEngine]; everything after this type is a pure
 * function pinned by golden fixtures. [raw] is what `Receipt.ocrText` persists.
 */
data class OcrText(
    val lines: List<String>,
) {
    val raw: String get() = lines.joinToString(separator = "\n")
}
