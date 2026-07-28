package io.github.sebkoo.hapsum.core.data

import android.content.Context
import androidx.room3.Room

/**
 * Data-layer composition root: builds the database and the repositories over it. Room types
 * never cross this module's boundary — consumers see repository interfaces only.
 */
class HapsumDataContainer(
    context: Context,
) {
    private val database = createHapsumDatabase(context)

    val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl(database.expenseDao())
    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(database.categoryDao())
}

internal fun createHapsumDatabase(context: Context): HapsumDatabase =
    Room
        .databaseBuilder(context, HapsumDatabase::class.java, "hapsum.db")
        .addMigrations(MIGRATION_1_2)
        .build()
