package io.github.sebkoo.hapsum.core.mvi

/** Marker for a screen's immutable state (`val`-only, immutable collections — ADR-0004). */
interface UiState

/** Marker for a screen's sealed intent hierarchy — the only way anything reaches the reducer. */
interface UiIntent

/**
 * Marker for intent variants that carry async results back into the reducer (repository
 * emissions, work completions). They live inside the screen's public sealed [UiIntent] type,
 * but [MviViewModel.onIntent] rejects them — only the ViewModel itself may re-enter with one,
 * via [MviViewModel.dispatch] (ADR-0004).
 */
interface InternalUiIntent : UiIntent

/** Marker for one-shot effects: navigation, snackbars — anything delivered exactly once. */
interface UiEffect
