package io.github.sebkoo.hapsum.core.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, ReceiptEntity::class, LineItemEntity::class],
    version = 4,
    // Explicit, not relying on the default: migration discipline starts at schema v1 (ADR-0003).
    exportSchema = true,
)
abstract class HapsumDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao(): CategoryDao

    abstract fun receiptDao(): ReceiptDao
}
