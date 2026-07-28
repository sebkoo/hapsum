package io.github.sebkoo.hapsum.core.testing

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId

object CategoryFixtures {
    fun groceries(): Category = Category(CategoryId("fixture-groceries"), "Groceries")
}
