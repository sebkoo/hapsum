package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.ReceiptId
import io.github.sebkoo.hapsum.core.testing.ReceiptFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptRepositoryImplTest {
    private val dao = mockk<ReceiptDao>()
    private val repository = ReceiptRepositoryImpl(dao)

    @Test
    fun `save — delegates to dao with the mapped entity and positioned line items`() =
        runTest {
            val receipt = ReceiptFixtures.synthetic()
            coEvery { dao.insertWithLineItems(any(), any()) } returns Unit

            repository.save(receipt)

            coVerify { dao.insertWithLineItems(receipt.toEntity(), receipt.toLineItemEntities()) }
        }

    @Test
    fun `getById — dao returns a receipt with line items — maps to domain, order preserved`() =
        runTest {
            val receipt = ReceiptFixtures.synthetic()
            coEvery { dao.getByIdWithLineItems(receipt.id.value) } returns
                ReceiptWithLineItems(receipt.toEntity(), receipt.toLineItemEntities())

            val result = repository.getById(receipt.id)

            assertEquals(receipt, result)
        }

    @Test
    fun `getById — dao finds nothing — repository returns null`() =
        runTest {
            coEvery { dao.getByIdWithLineItems("missing") } returns null

            val result = repository.getById(ReceiptId("missing"))

            assertNull(result)
        }
}
