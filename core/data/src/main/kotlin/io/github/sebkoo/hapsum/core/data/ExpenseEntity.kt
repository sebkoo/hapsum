package io.github.sebkoo.hapsum.core.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * `categoryId` is RESTRICTed to an existing [CategoryEntity] row, archived or not — the
 * database refuses to let a referenced category disappear (ADR-0003), on top of [CategoryDao]
 * not exposing a hard delete at all. `receiptId` is NOT NULL: every expense originates from a
 * receipt in MVP (ADR-0003); `lineItemId` is nullable, since an expense may represent the
 * receipt's total rather than one specific parsed line.
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
    ],
    indices = [Index("categoryId")],
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
