package io.github.sebkoo.hapsum.feature.confirm

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.sebkoo.hapsum.core.designsystem.HapsumTheme
import io.github.sebkoo.hapsum.core.model.CategoryId
import io.github.sebkoo.hapsum.core.model.CurrencyCode
import io.github.sebkoo.hapsum.core.model.LineItem
import io.github.sebkoo.hapsum.core.model.LineItemId
import io.github.sebkoo.hapsum.core.model.Money
import io.github.sebkoo.hapsum.core.model.ParseConfidence
import io.github.sebkoo.hapsum.core.model.ParsedField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConfirmContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val usd = CurrencyCode.of("USD")

    private fun loadedState(): ConfirmUiState =
        ConfirmUiState(
            isLoading = false,
            merchant = ParsedField("SYNTH MART", ParseConfidence.LOW),
            currency = usd,
            amountText = "5.50",
            amount = Money(5_50, usd),
            dateText = "2026-01-01",
            date = LocalDate.of(2026, 1, 1),
            categoryId = CategoryId("groceries"),
            lineItems = listOf(LineItem(LineItemId("li-1"), "Milk", Money(2_50, usd))),
        )

    @Test
    fun `confirm content — loading — renders a progress indicator, not the form`() {
        compose.setContent {
            HapsumTheme {
                ConfirmContent(state = ConfirmUiState(isLoading = true), onIntent = {})
            }
        }

        compose.onAllNodesWithText("Save").assertCountEquals(0)
    }

    @Test
    fun `confirm content — load failed — renders the failure message`() {
        compose.setContent {
            HapsumTheme {
                ConfirmContent(
                    state = ConfirmUiState(isLoading = false, error = ConfirmError.LoadFailed),
                    onIntent = {},
                )
            }
        }

        compose.onNodeWithText("Couldn't load this receipt").assertIsDisplayed()
    }

    @Test
    fun `confirm content — receipt loaded — renders merchant, amount, date, and line items`() {
        compose.setContent {
            HapsumTheme {
                ConfirmContent(state = loadedState(), onIntent = {})
            }
        }

        compose.onNodeWithText("SYNTH MART").assertIsDisplayed()
        compose.onNodeWithText("5.50").assertIsDisplayed()
        compose.onNodeWithText("2026-01-01").assertIsDisplayed()
        compose.onNodeWithText("Milk").assertIsDisplayed()
        compose.onNodeWithText("$2.50").assertIsDisplayed()
    }

    @Test
    fun `confirm content — mismatch flagged — renders the non-blocking hint, save stays enabled`() {
        compose.setContent {
            HapsumTheme {
                ConfirmContent(state = loadedState().copy(lineItemsMismatch = true), onIntent = {})
            }
        }

        compose.onNodeWithText("Line items don't add up to the total").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun `confirm content — amount missing — save button is disabled`() {
        compose.setContent {
            HapsumTheme {
                ConfirmContent(state = loadedState().copy(amount = null, amountText = ""), onIntent = {})
            }
        }

        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `confirm content — save clicked — sends SaveClicked`() {
        var lastIntent: ConfirmUiIntent? = null

        compose.setContent {
            HapsumTheme {
                ConfirmContent(state = loadedState(), onIntent = { intent -> lastIntent = intent })
            }
        }

        compose.onNodeWithText("Save").performClick()

        assertEquals(ConfirmUiIntent.SaveClicked, lastIntent)
    }

    @Test
    fun `confirm content — category chip clicked — sends CategorySelected`() {
        var lastIntent: ConfirmUiIntent? = null

        compose.setContent {
            HapsumTheme {
                ConfirmContent(state = loadedState(), onIntent = { intent -> lastIntent = intent })
            }
        }

        compose.onNodeWithText("Dining").performClick()

        assertEquals(ConfirmUiIntent.CategorySelected(CategoryId("dining")), lastIntent)
    }

    @Test
    fun `confirm content — low-confidence amount — renders the low-confidence hint`() {
        compose.setContent {
            HapsumTheme {
                ConfirmContent(
                    state = loadedState().copy(amountConfidence = ParseConfidence.LOW),
                    onIntent = {},
                )
            }
        }

        compose.onNodeWithText("Low confidence — please double-check").assertIsDisplayed()
    }
}
