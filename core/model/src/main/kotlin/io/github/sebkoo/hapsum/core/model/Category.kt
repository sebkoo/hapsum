package io.github.sebkoo.hapsum.core.model

/**
 * Stable identity for a [Category], independent of its display name — user-defined categories
 * (roadmap) can be renamed without breaking anything that references them by id.
 */
@JvmInline
value class CategoryId(
    val value: String,
) {
    companion object {
        /** Reserved fallback category, seeded into every database — never absent, never archived. */
        val UNCATEGORIZED = CategoryId("uncategorized")
    }
}

/**
 * A spending category. A value object, not an enum: users will define their own. Categories are
 * archived, never hard-deleted, once an expense references them — see ADR-0003.
 */
data class Category(
    val id: CategoryId,
    val name: String,
    val isArchived: Boolean = false,
)
