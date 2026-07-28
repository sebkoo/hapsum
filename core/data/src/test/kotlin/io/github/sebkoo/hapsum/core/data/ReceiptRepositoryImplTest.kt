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
    fun `save — delegates to dao insert with the mapped entity`() =
        runTest {
            val receipt = ReceiptFixtures.synthetic()
            coEvery { dao.insert(any()) } returns Unit

            repository.save(receipt)

            coVerify { dao.insert(receipt.toEntity()) }
        }

    @Test
    fun `getById — dao returns an entity — maps to domain with empty line items`() =
        runTest {
            val receipt = ReceiptFixtures.synthetic()
            coEvery { dao.getById(receipt.id.value) } returns receipt.toEntity()

            val result = repository.getById(receipt.id)

            assertEquals(receipt.copy(lineItems = emptyList()), result)
        }

    @Test
    fun `getById — dao finds nothing — repository returns null`() =
        runTest {
            coEvery { dao.getById("missing") } returns null

            val result = repository.getById(ReceiptId("missing"))

            assertNull(result)
        }
}
