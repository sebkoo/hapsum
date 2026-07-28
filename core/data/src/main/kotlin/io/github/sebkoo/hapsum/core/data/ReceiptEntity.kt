package io.github.sebkoo.hapsum.core.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Mirrors the domain `Receipt`'s scalar fields — immutable evidence, never edited after capture
 * (ADR-0003). Line items are not persisted here: no `LineItemEntity` table exists yet, deferred
 * until the OCR parser (row 16) needs one.
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val imageRef: String,
    val ocrText: String,
    val parseConfidence: Float,
)
