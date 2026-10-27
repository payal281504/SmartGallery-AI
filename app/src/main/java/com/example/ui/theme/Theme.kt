package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AuraPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = AuraSecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = AuraTertiaryDark,
    onTertiary = Color.Black,
    background = AuraDarkBackground,
    onBackground = AuraDarkOnSurface,
    surface = AuraDarkSurface,
    onSurface = AuraDarkOnSurface,
    surfaceVariant = AuraDarkSurfaceVariant,
    onSurfaceVariant = AuraDarkOnSurfaceVariant,
    surfaceContainer = AuraDarkSurfaceContainer,
    error = AuraDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AuraPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = AuraSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = AuraTertiaryLight,
    onTertiary = Color.White,
    background = AuraLightBackground,
    onBackground = AuraLightOnSurface,
    surface = AuraLightSurface,
    onSurface = AuraLightOnSurface,
    surfaceVariant = AuraLightSurfaceVariant,
    onSurfaceVariant = AuraLightOnSurfaceVariant,
    surfaceContainer = AuraLightSurfaceContainer,
    error = AuraDanger,
    onError = Color.White
)

@Composable
fun AuraGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted rich palette for cohesive gallery aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for existing template callers
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    AuraGalleryTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
