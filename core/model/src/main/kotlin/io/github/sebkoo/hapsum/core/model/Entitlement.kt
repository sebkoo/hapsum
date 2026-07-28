package io.github.sebkoo.hapsum.core.model

/**
 * Closed vocabulary of gated capabilities (ADR-0007). New entries arrive with the ADR or row
 * that implements their feature — never speculatively.
 */
enum class Entitlement {
    /** ADR-0006's CloudEngine precondition — the entry that makes its clause checkable. */
    CLOUD_AI,
}
