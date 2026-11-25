package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioColorScheme = darkColorScheme(
    primary = SaffronAmber,
    onPrimary = MidnightSlate,
    primaryContainer = Color(0xFF3A280A),
    onPrimaryContainer = WarmGoldText,
    secondary = RoyalGold,
    onSecondary = MidnightSlate,
    secondaryContainer = Color(0xFF2B2414),
    onSecondaryContainer = WarmGoldText,
    tertiary = DivineBlueAccent,
    onTertiary = MidnightSlate,
    background = MidnightSlate,
    onBackground = IvoryText,
    surface = StudioSurface,
    onSurface = IvoryText,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = MutedSlateText,
    outline = StudioBorder,
    error = CrimsonAccent,
    onError = IvoryText
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun SamvadStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
