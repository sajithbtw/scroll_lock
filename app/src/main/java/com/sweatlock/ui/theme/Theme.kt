package com.sweatlock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFFF2F2F7)
val Card = Color(0xFFFFFFFF)
val Accent = Color(0xFF0A84FF)
val Good = Color(0xFF34C759)
val Warn = Color(0xFFFF9F0A)
val Ink = Color(0xFF1C1C1E)
val Muted = Color(0xFF6E6E73)

@Composable
fun SweatLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Accent, background = Bg, surface = Card, onSurface = Ink),
        content = content
    )
}
