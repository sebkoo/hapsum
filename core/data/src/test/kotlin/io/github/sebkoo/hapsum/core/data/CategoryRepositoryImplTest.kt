package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.CategoryId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CategoryRepositoryImplTest {
    private val dao = mockk<CategoryDao>()
    private val repository = CategoryRepositoryImpl(dao)

    @Test
    fun `seedDefaults — inserts the reserved uncategorized category, ignoring conflicts`() =
        runTest {
            coEvery { dao.insertDefaults(any()) } returns Unit

            repository.seedDefaults()

            coVerify {
                dao.insertDefaults(
                    listOf(CategoryEntity(id = CategoryId.UNCATEGORIZED.value, name = "Uncategorized")),
                )
            }
        }

    @Test
    fun `archive — delegates to dao archive by id`() =
        runTest {
            coEvery { dao.archive(any()) } returns Unit

            repository.archive(CategoryId("groceries"))

            coVerify { dao.archive("groceries") }
        }
}
