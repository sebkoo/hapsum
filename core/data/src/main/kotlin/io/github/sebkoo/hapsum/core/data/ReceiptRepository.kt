package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId

interface ReceiptRepository {
    /** Immutable evidence, written once at capture (ADR-0003) — never an update. */
    suspend fun save(receipt: Receipt)

    suspend fun getById(id: ReceiptId): Receipt?
}

class ReceiptRepositoryImpl(
    private val dao: ReceiptDao,
) : ReceiptRepository {
    override suspend fun save(receipt: Receipt) {
        dao.insertWithLineItems(receipt.toEntity(), receipt.toLineItemEntities())
    }

    override suspend fun getById(id: ReceiptId): Receipt? = dao.getByIdWithLineItems(id.value)?.toDomain()
}
