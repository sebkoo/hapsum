package io.github.sebkoo.hapsum.feature.capture

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.sebkoo.hapsum.core.designsystem.HapsumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers only the permission-gate state: `CameraXViewfinder` needs a real `SurfaceRequest`
 * backed by an actual camera session, which Robolectric cannot provide — the granted/preview
 * branch is exercised on-device, not here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `capture content — permission not granted — renders the rationale`() {
        compose.setContent {
            HapsumTheme {
                CaptureContent(state = CaptureUiState(hasCameraPermission = false), onIntent = {})
            }
        }

        compose.onNodeWithText("Camera access is needed to photograph receipts").assertIsDisplayed()
    }

    @Test
    fun `capture content — permission not granted — request button invokes the callback`() {
        var requested = false

        compose.setContent {
            HapsumTheme {
                CaptureContent(
                    state = CaptureUiState(hasCameraPermission = false),
                    onIntent = {},
                    onRequestPermission = { requested = true },
                )
            }
        }

        compose.onNodeWithText("Grant camera access").performClick()

        assert(requested)
    }

    @Test
    fun `capture content — permission granted, no surface request yet — renders loading`() {
        compose.setContent {
            HapsumTheme {
                CaptureContent(state = CaptureUiState(hasCameraPermission = true), onIntent = {})
            }
        }

        compose
            .onAllNodesWithText("Camera access is needed to photograph receipts")
            .assertCountEquals(0)
    }
}
