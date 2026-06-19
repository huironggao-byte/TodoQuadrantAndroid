package com.example.todoquadrant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B68),
    onPrimary = Color.White,
    secondary = Color(0xFF9E5B2D),
    onSecondary = Color.White,
    tertiary = Color(0xFF6F5AA8),
    onTertiary = Color.White,
    background = Color(0xFFF7F8F5),
    onBackground = Color(0xFF1B1F22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F22),
    surfaceVariant = Color(0xFFE6ECE9),
    onSurfaceVariant = Color(0xFF47524F),
)

@Composable
fun TodoQuadrantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
