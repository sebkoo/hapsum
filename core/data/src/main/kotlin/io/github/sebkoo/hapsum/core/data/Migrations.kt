package io.github.sebkoo.hapsum.core.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds the `receipts` table and RESTRICTs `expenses.receiptId` to it (ADR-0003). SQLite has no
 * `ALTER TABLE ADD FOREIGN KEY`, so `expenses` is recreated wholesale — the standard Room
 * table-recreate pattern. Every v1 `expenses.receiptId` was a free-floating string with no
 * backing row; before the FK exists, this migration backfills one placeholder [ReceiptEntity]
 * per distinct `receiptId` already referenced, so no pre-existing expense is dropped or
 * orphaned. `createSql` here is copied verbatim from the generated `schemas/.../2.json` — the
 * whole point of exporting it is to validate against exactly this string.
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `receipts` (`id` TEXT NOT NULL, `imageRef` TEXT NOT NULL, " +
                    "`ocrText` TEXT NOT NULL, `parseConfidence` REAL NOT NULL, PRIMARY KEY(`id`))",
            )

            // Backfill: every distinct receiptId already on an expense gets a placeholder
            // receipt row, so the FK below never orphans pre-existing data.
            connection.execSQL(
                "INSERT INTO `receipts` (id, imageRef, ocrText, parseConfidence) " +
                    "SELECT DISTINCT receiptId, '', '', 0.0 FROM `expenses`",
            )

            connection.execSQL(
                "CREATE TABLE `expenses_new` (`id` TEXT NOT NULL, `amountMinorUnits` INTEGER NOT NULL, " +
                    "`currencyIsoCode` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                    "`receiptId` TEXT NOT NULL, `lineItemId` TEXT, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT , " +
                    "FOREIGN KEY(`receiptId`) REFERENCES `receipts`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT )",
            )
            val columns = "id, amountMinorUnits, currencyIsoCode, categoryId, date, receiptId, lineItemId"
            connection.execSQL(
                "INSERT INTO `expenses_new` ($columns) SELECT $columns FROM `expenses`",
            )
            connection.execSQL("DROP TABLE `expenses`")
            connection.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_receiptId` ON `expenses` (`receiptId`)")
        }
    }
