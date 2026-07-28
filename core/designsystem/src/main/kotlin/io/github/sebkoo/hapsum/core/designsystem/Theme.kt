package io.github.sebkoo.hapsum.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun HapsumTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) HapsumDarkColorScheme else HapsumLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HapsumTypography,
        content = content,
    )
}
