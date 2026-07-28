package io.github.sebkoo.hapsum.core.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun observeAllWithCategory(): Flow<List<ExpenseWithCategoryRow>>
}
