package ru.missclick.wolfwisdom.ui.theme

import androidx.compose.ui.graphics.Color

// Figma: WW · Primitives.
object WolfPalette {
    val Night950 = Color(0xFF050C14)
    val Night900 = Color(0xFF0A1623)
    val Night800 = Color(0xFF142637)
    val Moon100 = Color(0xFFF0F3FA)
    val Moon300 = Color(0xFFB9C9DD)
    val Moon500 = Color(0xFF87A2BF)
    val Ice300 = Color(0xFFC9DFFF)
    val Ice500 = Color(0xFF719BC1)
}

// Figma: WW · Moonlight. Semantic colors remain available outside Material roles.
object WolfColors {
    val Background = WolfPalette.Night950
    val Surface = WolfPalette.Night800
    val Text = WolfPalette.Moon100
    val Secondary = WolfPalette.Moon300
    val Muted = WolfPalette.Moon500
    val Accent = WolfPalette.Ice300
    val Border = WolfPalette.Ice500
    val Glass = Surface.copy(alpha = 0.91f)
    val Pressed = Accent.copy(alpha = 0.16f)
    val GlassBorder = Color(red = 0.33f, green = 0.41f, blue = 0.50f, alpha = 0.55f)
    val IconMoon = Color(red = 0.87f, green = 0.91f, blue = 1f, alpha = 1f)
}
