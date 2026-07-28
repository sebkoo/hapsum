package io.github.sebkoo.hapsum.core.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Mirrors the domain `Receipt` — immutable evidence, never edited after capture (ADR-0003).
 * The seven `parsed*` columns land in schema v3 (row 17) and are nullable in value/confidence
 * pairs: each pair is present or NULL as a unit, because a parsed field only exists together
 * with its confidence. Rows written before v3 (and OCR failures) keep them NULL — "the parser
 * found nothing", not zeros. `parseConfidence` persists the domain's derived aggregate so a
 * future unconfirmed-receipts inbox can ORDER BY it in SQL; the domain recomputes it from the
 * per-field values and ignores this column on read. `parsedDate` is an epoch day, the same
 * representation as `ExpenseEntity.date`.
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val imageRef: String,
    val ocrText: String,
    val parseConfidence: Float,
    val parsedMerchant: String? = null,
    val parsedMerchantConfidence: String? = null,
    val parsedDate: Long? = null,
    val parsedDateConfidence: String? = null,
    val parsedTotalMinorUnits: Long? = null,
    val parsedTotalCurrency: String? = null,
    val parsedTotalConfidence: String? = null,
)
