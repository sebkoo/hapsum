package io.github.sebkoo.hapsum.core.model

/**
 * The MVP default category vocabulary, seeded idempotently at every app start (`:core:data`)
 * and the only ids the rule-based categorizer (`:core:ai`) may suggest — one source of truth,
 * so an engine suggestion can never reference a category the database doesn't hold (the
 * `expenses.categoryId` FK stays unambushable by construction; a test in `:core:ai` pins it).
 * User-defined categories are a roadmap item; this list grows only in lockstep with the
 * engine's rule table.
 */
object DefaultCategories {
    val UNCATEGORIZED = Category(CategoryId.UNCATEGORIZED, "Uncategorized")
    val GROCERIES = Category(CategoryId("groceries"), "Groceries")
    val DINING = Category(CategoryId("dining"), "Dining")
    val TRANSPORT = Category(CategoryId("transport"), "Transport")
    val UTILITIES = Category(CategoryId("utilities"), "Utilities")
    val SHOPPING = Category(CategoryId("shopping"), "Shopping")

    /** Stable seed order, the reserved fallback first. */
    val all: List<Category> =
        listOf(UNCATEGORIZED, GROCERIES, DINING, TRANSPORT, UTILITIES, SHOPPING)
}
