package io.github.sebkoo.hapsum.feature.insights

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.sebkoo.hapsum.core.designsystem.HapsumTheme
import io.github.sebkoo.hapsum.core.model.Category
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.testing.CategoryFixtures
import io.github.sebkoo.hapsum.core.testing.MoneyFixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth

/**
 * Robolectric-backed Compose UI test — the LedgerContentTest pattern. This IS row 23's UI test,
 * running under verify.sh/CI with no emulator: the ladder's Espresso-interop wording resolves
 * here because Hapsum has no View/Compose hybrid boundary to justify a device-only androidTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InsightsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `insights content — fixture summaries — renders month header, category name, and formatted amount`() {
        val groceries = CategoryFixtures.groceries()
        val transport = Category(CategoryId("fixture-transport"), "Transport")
        val summary =
            MonthlySummary(
                month = YearMonth.of(2026, 1),
                total = MoneyFixtures.usd(4_00),
                categories =
                    listOf(
                        CategorySummary(groceries, MoneyFixtures.usd(2_50)),
                        CategorySummary(transport, MoneyFixtures.usd(1_50)),
                    ),
            )

        compose.setContent {
            HapsumTheme {
                InsightsContent(state = InsightsUiState(isLoading = false, summaries = listOf(summary)))
            }
        }

        compose.onNodeWithText("2026-01").assertIsDisplayed()
        compose.onNodeWithText("Groceries").assertIsDisplayed()
        compose.onNodeWithText("Transport").assertIsDisplayed()
        compose.onNodeWithText("$4.00").assertIsDisplayed()
        compose.onNodeWithText("$2.50").assertIsDisplayed()
        compose.onNodeWithText("$1.50").assertIsDisplayed()
    }

    @Test
    fun `insights content — no summaries — renders the empty state`() {
        compose.setContent {
            HapsumTheme {
                InsightsContent(state = InsightsUiState(isLoading = false, summaries = emptyList()))
            }
        }

        compose.onNodeWithText("No spending to summarize yet").assertIsDisplayed()
    }
}
