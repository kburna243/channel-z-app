package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun GrindhouseTheme(
    content: @Composable () -> Unit
) {
    // Bewusst in der Composition erzeugt statt als Konstante: nur so liest das Farbschema die
    // aktuellen Werte und wird beim Themenwechsel neu gebildet.
    val colorScheme = darkColorScheme(
        primary = AccentIceBlue,
        onPrimary = MidnightCanvas,
        primaryContainer = AccentVibrantOrange,
        onPrimaryContainer = PureWhite,
        secondary = AccentCoral,
        onSecondary = MidnightCanvas,
        tertiary = AccentLavender,
        onTertiary = MidnightCanvas,
        background = MidnightCanvas,
        onBackground = PureWhite,
        surface = SurfaceDark,
        onSurface = PureWhite,
        surfaceVariant = SurfaceCard,
        onSurfaceVariant = TextMuted,
        error = StatusOfflineRed,
        onError = PureWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
