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
 * `lineItemId` is nullable and un-FK'd — no `LineItemEntity` table exists yet.
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
    ],
    indices = [Index("categoryId"), Index("receiptId")],
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
