package io.github.sebkoo.hapsum.core.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

/** Backs [ReceiptRepository] — capture (row 16) is its first user. */
@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: String): ReceiptEntity?
}
