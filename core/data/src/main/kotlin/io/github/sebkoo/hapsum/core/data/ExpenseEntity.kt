package io.github.sebkoo.hapsum.core.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * `categoryId` is RESTRICTed to an existing [CategoryEntity] row, archived or not — the
 * database refuses to let a referenced category disappear (ADR-0003), on top of [CategoryDao]
 * not exposing a hard delete at all. `receiptId` is NOT NULL and RESTRICTed to an existing
 * [ReceiptEntity] row (schema v2, ADR-0003): every expense originates from a receipt in MVP.
 * `lineItemId` is nullable — an expense can represent a receipt's total instead of one specific
 * line — but when set it is RESTRICTed to an existing [LineItemEntity] row (schema v4): no
 * engine or screen may write an expense pointing at a line item that doesn't exist.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LineItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["lineItemId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("receiptId"), Index("lineItemId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amountMinorUnits: Long,
    val currencyIsoCode: String,
    val categoryId: String,
    val date: Long,
    val receiptId: String,
    val lineItemId: String?,
)
