package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.CategoryId

interface CategoryRepository {
    /** Idempotent — safe to call on every app start. Seeds the reserved [CategoryId.UNCATEGORIZED]. */
    suspend fun seedDefaults()

    suspend fun archive(categoryId: CategoryId)
}

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
) : CategoryRepository {
    override suspend fun seedDefaults() {
        dao.insertDefaults(
            listOf(CategoryEntity(id = CategoryId.UNCATEGORIZED.value, name = "Uncategorized")),
        )
    }

    override suspend fun archive(categoryId: CategoryId) {
        dao.archive(categoryId.value)
    }
}
