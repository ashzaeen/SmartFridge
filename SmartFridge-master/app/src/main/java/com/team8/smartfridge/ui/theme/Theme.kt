package com.team8.smartfridge.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Cream = Color(0xFFF2F0E8)   // the one surface tone for every container slot

private val GardenTerminalColorScheme = lightColorScheme(
    primary                  = Brand,
    secondary                = BrandLight,
    tertiary                 = BrandAccent,
    background               = BgSecondary,
    surface                  = Cream,
    surfaceVariant           = Cream,
    surfaceBright            = Cream,
    surfaceDim               = Cream,
    surfaceContainer         = Cream,
    surfaceContainerLow      = Cream,
    surfaceContainerHigh     = Cream,
    surfaceContainerHighest  = Cream,
    onPrimary                = Color(0xFFF0F7E8),
    onSecondary              = White,
    onTertiary               = TextPrimary,
    onBackground             = TextPrimary,
    onSurface                = TextPrimary,
    onSurfaceVariant         = TextSecondary,
    outline                  = Divider,
    outlineVariant           = Color(0xFFE0DDD0)
)

@Composable
fun SmartFridgeTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Cream.toArgb()
            // Light background → dark status bar icons
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = GardenTerminalColorScheme,
        typography = Typography,
        content = content
    )
}
