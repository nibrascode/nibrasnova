package com.nibras.nova.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaTurquoise,
    onPrimary = NovaDeepNavy,
    primaryContainer = NovaElevatedNavy,
    onPrimaryContainer = NovaTurquoise,
    secondary = NovaCyanSecondary,
    onSecondary = NovaDeepNavy,
    secondaryContainer = NovaElevatedNavy,
    onSecondaryContainer = NovaSoftSilver,
    tertiary = NovaIncognitoAccent,
    background = NovaDeepNavy,
    onBackground = NovaSoftSilver,
    surface = NovaGraphiteBlack,
    onSurface = NovaSoftSilver,
    surfaceVariant = NovaElevatedNavy,
    onSurfaceVariant = NovaMutedSilver,
    outline = NovaCardBorderDark,
    error = NovaDangerRed,
    onError = Color.White
)

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaDeepTurquoiseLightMode,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = NovaDeepNavy,
    secondary = NovaCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = NovaLightElevatedSurface,
    onSecondaryContainer = NovaLightTextPrimary,
    tertiary = NovaIncognitoAccent,
    background = NovaLightBackground,
    onBackground = NovaLightTextPrimary,
    surface = NovaLightSurface,
    onSurface = NovaLightTextPrimary,
    surfaceVariant = NovaLightElevatedSurface,
    onSurfaceVariant = NovaLightTextSecondary,
    outline = NovaLightBorder,
    error = NovaDangerRed,
    onError = Color.White
)

@Composable
fun NovaTheme(
    darkTheme: Boolean = true,
    highContrastTurquoise: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) NovaDarkColorScheme else NovaLightColorScheme
    val colorScheme = if (!highContrastTurquoise && darkTheme) {
        baseScheme.copy(primary = NovaCyanSecondary)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NovaTypography,
        shapes = NovaShapes,
        content = content
    )
}
