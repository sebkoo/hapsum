package io.github.sebkoo.hapsum.core.data

import androidx.room3.Dao
import androidx.room3.Insert

/**
 * Minimal by design: capture (row 15) is what needs a full write/read surface and a
 * repository over it. This exists only so [ReceiptEntity] is reachable at all from Kotlin —
 * without it, every FK-dependent test would drop to raw SQL instead of the DAO.
 */
@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: ReceiptEntity)
}
