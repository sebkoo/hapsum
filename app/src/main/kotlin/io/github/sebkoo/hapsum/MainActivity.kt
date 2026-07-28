package io.github.sebkoo.hapsum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.sebkoo.hapsum.core.designsystem.HapsumTheme
import io.github.sebkoo.hapsum.ui.PlaceholderScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HapsumTheme {
                PlaceholderScreen()
            }
        }
    }
}
