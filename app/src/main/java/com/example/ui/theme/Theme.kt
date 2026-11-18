package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryRedLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = PrimaryDarkRed,
    secondary = SecondaryBlueLight,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = SecondaryBlueLight,
    tertiary = AccentPurpleLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight,
    error = DeleteRedLight,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = PrimaryRedDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = SecondaryBlueDark,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = Color(0xFFBBDEFB),
    tertiary = AccentPurpleDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    error = DeleteRedDark,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun PdfGalleryTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
