package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.Expense
import io.github.sebkoo.hapsum.core.model.ExpenseId
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
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
        parsedMerchant = merchant?.value,
        parsedMerchantConfidence = merchant?.confidence?.name,
        parsedDate = purchasedAt?.value?.toEpochDay(),
        parsedDateConfidence = purchasedAt?.confidence?.name,
        parsedTotalMinorUnits = total?.value?.minorUnits,
        parsedTotalCurrency = total?.value?.currency?.isoCode,
        parsedTotalConfidence = total?.confidence?.name,
    )

internal fun Receipt.toLineItemEntities(): List<LineItemEntity> =
    lineItems.mapIndexed { index, item ->
        LineItemEntity(
            id = item.id.value,
            receiptId = id.value,
            position = index,
            description = item.description,
            amountMinorUnits = item.amount.minorUnits,
            currencyIsoCode = item.amount.currency.isoCode,
        )
    }

internal fun ReceiptWithLineItems.toDomain(): Receipt =
    Receipt(
        id = ReceiptId(receipt.id),
        imageRef = receipt.imageRef,
        ocrText = receipt.ocrText,
        merchant = parsedField(receipt.parsedMerchant, receipt.parsedMerchantConfidence),
        purchasedAt = parsedField(receipt.parsedDate?.let(LocalDate::ofEpochDay), receipt.parsedDateConfidence),
        total =
            parsedField(
                receipt.parsedTotalMinorUnits?.let { minorUnits ->
                    receipt.parsedTotalCurrency?.let { iso -> Money(minorUnits, CurrencyCode.of(iso)) }
                },
                receipt.parsedTotalConfidence,
            ),
        lineItems =
            lineItems.map { item ->
                LineItem(
                    id = LineItemId(item.id),
                    description = item.description,
                    amount = Money(item.amountMinorUnits, CurrencyCode.of(item.currencyIsoCode)),
                )
            },
    )

/** Value/confidence columns are NULL as a unit (see [ReceiptEntity]) — either both map or neither. */
private fun <T : Any> parsedField(
    value: T?,
    confidence: String?,
): ParsedField<T>? =
    if (value == null || confidence == null) {
        null
    } else {
        ParsedField(value, ParseConfidence.valueOf(confidence))
    }
