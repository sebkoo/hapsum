package io.github.sebkoo.hapsum.core.model

/**
 * Stable identity for a [Category], independent of its display name — user-defined categories
 * (roadmap) can be renamed without breaking anything that references them by id.
 */
@JvmInline
value class CategoryId(
    val value: String,
)

/** A spending category. A value object, not an enum: users will define their own. */
data class Category(
    val id: CategoryId,
    val name: String,
)
