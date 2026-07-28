package io.github.sebkoo.hapsum.core.data

import android.content.Context
import androidx.room3.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Data-layer Hilt bindings — the composition root's job, now discharged by the framework
 * instead of `HapsumDataContainer` (ADR-0005). Room types never cross this module's boundary —
 * consumers see repository interfaces only.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides
    @Singleton
    fun provideHapsumDatabase(
        @ApplicationContext context: Context,
    ): HapsumDatabase =
        Room
            .databaseBuilder(context, HapsumDatabase::class.java, "hapsum.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideExpenseRepository(database: HapsumDatabase): ExpenseRepository =
        ExpenseRepositoryImpl(database.expenseDao())

    @Provides
    fun provideCategoryRepository(database: HapsumDatabase): CategoryRepository =
        CategoryRepositoryImpl(database.categoryDao())

    @Provides
    fun provideReceiptRepository(database: HapsumDatabase): ReceiptRepository =
        ReceiptRepositoryImpl(database.receiptDao())
}
