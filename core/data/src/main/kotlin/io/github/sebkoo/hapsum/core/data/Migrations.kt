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

/**
 * Adds the OCR parse output (row 17): seven nullable parsed-header columns on `receipts` —
 * plain `ADD COLUMN`s, no table recreate this time — plus the `line_items` table the row-14
 * entity KDoc deferred to exactly this row. Pre-v3 receipt rows keep NULL parsed fields,
 * which the domain reads as "the parser never ran", not as zeros. `line_items`' `CREATE
 * TABLE` is copied verbatim from the generated `schemas/.../3.json`, same discipline as
 * [MIGRATION_1_2].
 */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedMerchant` TEXT")
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedMerchantConfidence` TEXT")
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedDate` INTEGER")
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedDateConfidence` TEXT")
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedTotalMinorUnits` INTEGER")
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedTotalCurrency` TEXT")
            connection.execSQL("ALTER TABLE `receipts` ADD COLUMN `parsedTotalConfidence` TEXT")

            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `line_items` (`id` TEXT NOT NULL, `receiptId` TEXT NOT NULL, " +
                    "`position` INTEGER NOT NULL, `description` TEXT NOT NULL, " +
                    "`amountMinorUnits` INTEGER NOT NULL, `currencyIsoCode` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`receiptId`) REFERENCES `receipts`(`id`) " +
                    "ON UPDATE RESTRICT ON DELETE RESTRICT )",
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_line_items_receiptId` ON `line_items` (`receiptId`)")
        }
    }

/**
 * RESTRICTs `expenses.lineItemId` to `line_items.id` (ADR-0003's confirm-row amendment): the
 * column existed since schema v1 but the `line_items` table it points at didn't exist until v3
 * (row 17), so this is the first schema where the FK can be added. SQLite still has no `ALTER
 * TABLE ADD FOREIGN KEY`, so `expenses` is recreated wholesale — the same table-recreate pattern
 * as [MIGRATION_1_2]. No backfill needed this time: every non-null `lineItemId` already written
 * by capture points at a real `line_items` row, so the recreate cannot orphan anything.
 * `createSql` is copied verbatim from the generated `schemas/.../4.json`, the same discipline as
 * every migration before it.
 */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE `expenses_new` (`id` TEXT NOT NULL, `amountMinorUnits` INTEGER NOT NULL, " +
                    "`currencyIsoCode` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                    "`receiptId` TEXT NOT NULL, `lineItemId` TEXT, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT , " +
                    "FOREIGN KEY(`receiptId`) REFERENCES `receipts`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT , " +
                    "FOREIGN KEY(`lineItemId`) REFERENCES `line_items`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT )",
            )
            val columns = "id, amountMinorUnits, currencyIsoCode, categoryId, date, receiptId, lineItemId"
            connection.execSQL(
                "INSERT INTO `expenses_new` ($columns) SELECT $columns FROM `expenses`",
            )
            connection.execSQL("DROP TABLE `expenses`")
            connection.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_receiptId` ON `expenses` (`receiptId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_lineItemId` ON `expenses` (`lineItemId`)")
        }
    }
