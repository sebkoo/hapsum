package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.DefaultCategories
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryRepositoryImplTest {
    private val dao = mockk<CategoryDao>()
    private val repository = CategoryRepositoryImpl(dao)

    @Test
    fun `seedDefaults — inserts the full default vocabulary, uncategorized first`() =
        runTest {
            val seeded = slot<List<CategoryEntity>>()
            coEvery { dao.insertDefaults(capture(seeded)) } returns Unit

            repository.seedDefaults()

            coVerify { dao.insertDefaults(any()) }
            assertEquals(DefaultCategories.all.map { it.id.value }, seeded.captured.map(CategoryEntity::id))
            assertEquals(CategoryId.UNCATEGORIZED.value, seeded.captured.first().id)
        }

    @Test
    fun `archive — delegates to dao archive by id`() =
        runTest {
            coEvery { dao.archive(any()) } returns Unit

            repository.archive(CategoryId("groceries"))

            coVerify { dao.archive("groceries") }
        }
}
