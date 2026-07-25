package com.ahsanrehmat.pulseplan.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = PulseGreen,
    primaryContainer = PulseGreen,
    onPrimaryContainer = Ink,
    secondary = PulseGreenDark,
    background = Canvas,
    onBackground = Ink,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Ink,
    outline = Line,
)

private val DarkColors = darkColorScheme(
    primary = PulseGreen,
    onPrimary = Ink,
    primaryContainer = PulseGreenDark,
    background = Ink,
    onBackground = Canvas,
    surface = InkSoft,
    onSurface = Canvas,
    outline = Muted,
)

@Composable
fun PulsePlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Ink.toArgb()
            window.navigationBarColor = Ink.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = PulseTypography,
        content = content,
    )
}

