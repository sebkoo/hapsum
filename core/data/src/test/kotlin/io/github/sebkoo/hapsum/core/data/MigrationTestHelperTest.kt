package io.github.sebkoo.hapsum.core.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the v1→v2 migration (ADR-0003): the schema JSON in `schemas/` is what
 * [androidx.room3.testing.MigrationTestHelper] validates against, and this test is exactly why
 * exporting it from schema v1 was worth doing (ADR-0003) — the first real migration to run.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTestHelperTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseFile = instrumentation.targetContext.getDatabasePath("migration-test.db")

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = databaseFile,
            driver = AndroidSQLiteDriver(),
            databaseClass = HapsumDatabase::class,
        )

    @Test
    fun `migrate 1 to 2 — v1 expense referencing no receipt — survives via a backfilled placeholder`() =
        runTest {
            val v1 = helper.createDatabase(1)
            v1.execSQL(
                "INSERT INTO categories (id, name, isArchived) VALUES ('groceries', 'Groceries', 0)",
            )
            v1.execSQL(
                "INSERT INTO expenses (id, amountMinorUnits, currencyIsoCode, categoryId, date, " +
                    "receiptId, lineItemId) VALUES ('e1', 250, 'USD', 'groceries', 20089, 'r1', NULL)",
            )
            v1.close()

            val v2 = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

            v2.prepare("SELECT id FROM expenses WHERE id = 'e1'").use { statement ->
                assertTrue("expected the v1 expense row to survive the migration", statement.step())
            }
            v2.prepare("SELECT imageRef, ocrText, parseConfidence FROM receipts WHERE id = 'r1'").use { statement ->
                assertTrue("expected a backfilled placeholder receipt for the orphaned receiptId", statement.step())
                assertEquals("", statement.getText(0))
                assertEquals(0.0, statement.getDouble(2), 0.0)
            }
            v2.close()
        }

    @Test
    fun `migrate 1 to 2 — nonexistent category — FK RESTRICT still enforced after migration`() =
        runTest {
            val v1 = helper.createDatabase(1)
            v1.close()

            val v2 = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))
            // MigrationTestHelper hands back a raw connection outside Room's own open path,
            // which is what enables enforcement in production (already proven for categoryId
            // by RoomSchemaTest) — enable it explicitly to check the recreated table's FK
            // clause is well-formed, not to re-prove Room's own enforcement wiring.
            v2.execSQL("PRAGMA foreign_keys = ON")

            var caught: Throwable? = null
            try {
                v2.execSQL(
                    "INSERT INTO expenses (id, amountMinorUnits, currencyIsoCode, categoryId, date, " +
                        "receiptId, lineItemId) VALUES ('e2', 100, 'USD', 'does-not-exist', 20089, " +
                        "'also-missing', NULL)",
                )
            } catch (t: Throwable) {
                caught = t
            }

            assertTrue("expected the categoryId FK to still reject an unknown category", caught != null)
            v2.close()
        }

    @Test
    fun `migrate 2 to 3 — v2 receipt — survives with null parsed fields and an empty line_items table`() =
        runTest {
            val v2 = helper.createDatabase(2)
            v2.execSQL(
                "INSERT INTO receipts (id, imageRef, ocrText, parseConfidence) " +
                    "VALUES ('r1', 'receipts/r1.jpg', 'TOTAL 5.00', 0.5)",
            )
            v2.close()

            val v3 = helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3))

            v3.prepare("SELECT parsedMerchant, parsedTotalMinorUnits FROM receipts WHERE id = 'r1'").use { statement ->
                assertTrue("expected the v2 receipt row to survive the migration", statement.step())
                assertTrue("expected parsedMerchant NULL on a pre-v3 row", statement.isNull(0))
                assertTrue("expected parsedTotalMinorUnits NULL on a pre-v3 row", statement.isNull(1))
            }
            v3.prepare("SELECT COUNT(*) FROM line_items").use { statement ->
                assertTrue(statement.step())
                assertEquals(0L, statement.getLong(0))
            }
            v3.close()
        }
}
