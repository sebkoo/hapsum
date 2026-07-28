package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Expense
import io.github.sebkoo.hapsum.core.model.ExpenseId
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.Receipt
import io.github.sebkoo.hapsum.core.model.ReceiptId
import java.time.LocalDate

internal fun Expense.toEntity(): ExpenseEntity =
    ExpenseEntity(
        id = id.value,
        amountMinorUnits = amount.minorUnits,
        currencyIsoCode = amount.currency.isoCode,
        categoryId = categoryId.value,
        date = date.toEpochDay(),
        receiptId = receiptId.value,
        lineItemId = lineItemId?.value,
    )

internal fun ExpenseEntity.toDomain(): Expense =
    Expense(
        id = ExpenseId(id),
        amount = Money(amountMinorUnits, CurrencyCode.of(currencyIsoCode)),
        categoryId = CategoryId(categoryId),
        date = LocalDate.ofEpochDay(date),
        receiptId = ReceiptId(receiptId),
        lineItemId = lineItemId?.let(::LineItemId),
    )

internal fun CategoryEntity.toDomain(): Category =
    Category(
        id = CategoryId(id),
        name = name,
        isArchived = isArchived,
    )

internal fun ExpenseWithCategoryRow.toDomain(): ExpenseWithCategory =
    ExpenseWithCategory(
        expense = expense.toDomain(),
        category = category.toDomain(),
    )

internal fun Receipt.toEntity(): ReceiptEntity =
    ReceiptEntity(
        id = id.value,
        imageRef = imageRef,
        ocrText = ocrText,
        parseConfidence = parseConfidence,
    )

/** Line items never round-trip here — no `LineItemEntity` table exists yet (row 17). */
internal fun ReceiptEntity.toDomain(): Receipt =
    Receipt(
        id = ReceiptId(id),
        imageRef = imageRef,
        ocrText = ocrText,
        parseConfidence = parseConfidence,
        lineItems = emptyList(),
    )
