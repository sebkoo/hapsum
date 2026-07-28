package io.github.sebkoo.hapsum.core.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction

/** Backs [ReceiptRepository] — capture (row 16) is its first user; row 17 adds line items. */
@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: ReceiptEntity)

    @Insert
    suspend fun insertLineItems(lineItems: List<LineItemEntity>)

    /** Receipt and line items land atomically — a receipt with half its rows is never observable. */
    @Transaction
    suspend fun insertWithLineItems(
        receipt: ReceiptEntity,
        lineItems: List<LineItemEntity>,
    ) {
        insert(receipt)
        insertLineItems(lineItems)
    }

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: String): ReceiptEntity?

    @Query("SELECT * FROM line_items WHERE receiptId = :receiptId ORDER BY position")
    suspend fun getLineItems(receiptId: String): List<LineItemEntity>

    /** Read counterpart of [insertWithLineItems]: one transaction, one consistent snapshot. */
    @Transaction
    suspend fun getByIdWithLineItems(id: String): ReceiptWithLineItems? {
        val receipt = getById(id) ?: return null
        return ReceiptWithLineItems(receipt, getLineItems(id))
    }
}
