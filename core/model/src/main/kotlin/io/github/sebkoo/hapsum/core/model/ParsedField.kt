package io.github.sebkoo.hapsum.core.model

/**
 * How sure the deterministic parser is about one extracted field. Two levels on purpose: the
 * future confirm screen renders exactly two treatments — prefill quietly (HIGH) or highlight
 * for review (LOW) — and an absent field is a null [ParsedField], the third treatment. A float
 * would fake precision a keyword heuristic does not have.
 */
enum class ParseConfidence { HIGH, LOW }

/** One field extracted from OCR text, tagged with how the extraction earned its trust. */
data class ParsedField<out T>(
    val value: T,
    val confidence: ParseConfidence,
)
