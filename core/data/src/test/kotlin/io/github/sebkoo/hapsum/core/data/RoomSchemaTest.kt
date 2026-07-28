package io.github.sebkoo.hapsum.core.data

import android.content.Context
import androidx.room3.Delete
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.ExpenseFixtures
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the schema itself against a real in-memory SQLite database (Robolectric, no
 * emulator) — repository tests elsewhere mock the DAO, so the seed, the archive path, the FK
 * constraint, and the `@Relation` join never actually execute anywhere else (ADR-0003).
 *
 * `sdk = [35]`: the module targets compileSdk 36, but Robolectric 4.15.1 doesn't shadow API 36
 * yet — nothing here exercises SDK-36-specific behavior, so running under the latest Robolectric
 * supports is a correct stand-in, not a weakening of the test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomSchemaTest {
    private lateinit var db: HapsumDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, HapsumDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `categoryDao insertDefaults — fresh database — uncategorized category exists`() =
        runTest {
            val categoryDao = db.categoryDao()

            categoryDao.insertDefaults(
                listOf(CategoryEntity(id = CategoryId.UNCATEGORIZED.value, name = "Uncategorized")),
            )

            val seeded = categoryDao.getById(CategoryId.UNCATEGORIZED.value)
            assertNotNull(seeded)
            assertEquals("Uncategorized", seeded?.name)
            assertEquals(false, seeded?.isArchived)
        }

    @Test
    fun `categoryDao archive — existing category — isArchived round-trips true, no delete method exists`() =
        runTest {
            val categoryDao = db.categoryDao()
            val category = CategoryFixtures.groceries()
            categoryDao.insertDefaults(
                listOf(CategoryEntity(id = category.id.value, name = category.name)),
            )

            categoryDao.archive(category.id.value)

            val archived = categoryDao.getById(category.id.value)
            assertEquals(true, archived?.isArchived)

            val deleteMethods = CategoryDao::class.java.methods.filter { it.isAnnotationPresent(Delete::class.java) }
            assertTrue("CategoryDao must expose no @Delete method — archive-only (ADR-0003)", deleteMethods.isEmpty())
        }

    @Test
    fun `expenseDao insert — categoryId references no row — throws, FK RESTRICT enforced`() =
        runTest {
            val expenseDao = db.expenseDao()
            val expense = ExpenseFixtures.synthetic(categoryId = CategoryId("does-not-exist"))

            var caught: Throwable? = null
            try {
                expenseDao.insert(expense.toEntity())
            } catch (t: Throwable) {
                caught = t
            }

            assertNotNull("expected FK RESTRICT to reject an insert with a nonexistent categoryId", caught)
            val isForeignKeyViolation =
                generateSequence(caught) { it.cause }
                    .any { it.message?.contains("FOREIGN KEY", ignoreCase = true) == true }
            assertTrue("expected a foreign key violation, got: $caught", isForeignKeyViolation)
        }

    @Test
    fun `observeExpensesWithCategory — persisted rows — maps to the domain projection`() =
        runTest {
            val categoryDao = db.categoryDao()
            val expenseDao = db.expenseDao()
            val category = CategoryFixtures.groceries()
            val expense = ExpenseFixtures.synthetic(categoryId = category.id)

            categoryDao.insertDefaults(listOf(CategoryEntity(id = category.id.value, name = category.name)))
            expenseDao.insert(expense.toEntity())

            expenseDao.observeAllWithCategory().test {
                val rows = awaitItem()
                assertEquals(listOf(ExpenseWithCategory(expense, category)), rows.map { it.toDomain() })
                cancelAndIgnoreRemainingEvents()
            }
        }
}
