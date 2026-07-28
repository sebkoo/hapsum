package io.github.sebkoo.hapsum.core.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One parsed row of a receipt — the table `ReceiptEntity`'s row-14 KDoc deferred to the OCR
 * parser (row 17). FK RESTRICT both ways: a line item can neither precede nor outlive its
 * receipt (ADR-0003's no-orphans discipline, same as `expenses.receiptId`). `position`
 * preserves top-to-bottom receipt order — SQLite rowid order is not a contract. Money columns
 * mirror `ExpenseEntity`'s minor-units-plus-ISO-code pair (ADR-0002).
 */
@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onUpdate = ForeignKey.RESTRICT,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("receiptId")],
)
data class LineItemEntity(
    @PrimaryKey val id: String,
    val receiptId: String,
    val position: Int,
    val description: String,
    val amountMinorUnits: Long,
    val currencyIsoCode: String,
)
