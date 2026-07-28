package io.github.sebkoo.hapsum.feature.ledger

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.sebkoo.hapsum.core.data.ExpenseWithCategory
import io.github.sebkoo.hapsum.core.designsystem.HapsumTheme
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.ExpenseFixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The first Compose UI test — Robolectric-backed so it runs under the same JVM gate as
 * everything else (no emulator), same trade as RoomSchemaTest: sdk = [35] because Robolectric
 * does not shadow API 36 yet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LedgerContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `ledger content — fixture rows — renders category name and amount`() {
        val category = CategoryFixtures.groceries()
        val row =
            ExpenseWithCategory(
                expense = ExpenseFixtures.synthetic(categoryId = category.id),
                category = category,
            )

        compose.setContent {
            HapsumTheme {
                LedgerContent(state = LedgerUiState(isLoading = false, rows = listOf(row)))
            }
        }

        compose.onNodeWithText("Groceries").assertIsDisplayed()
        compose.onNodeWithText("$2.50").assertIsDisplayed()
    }

    @Test
    fun `ledger content — no rows — renders the empty state`() {
        compose.setContent {
            HapsumTheme {
                LedgerContent(state = LedgerUiState(isLoading = false, rows = emptyList()))
            }
        }

        compose.onNodeWithText("No expenses yet").assertIsDisplayed()
    }
}
