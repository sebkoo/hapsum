package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories

interface CategoryRepository {
    /** Idempotent — safe to call on every app start. Seeds [DefaultCategories.all]. */
    suspend fun seedDefaults()

    suspend fun archive(categoryId: CategoryId)
}

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
) : CategoryRepository {
    override suspend fun seedDefaults() {
        dao.insertDefaults(
            DefaultCategories.all.map { category ->
                CategoryEntity(id = category.id.value, name = category.name)
            },
        )
    }

    override suspend fun archive(categoryId: CategoryId) {
        dao.archive(categoryId.value)
    }
}
