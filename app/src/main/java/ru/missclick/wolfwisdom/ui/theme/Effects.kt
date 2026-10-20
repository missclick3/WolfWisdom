package ru.missclick.wolfwisdom.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// WW/Glass · moonlit. Backdrop blur and drop-shadow parameters differ from
// Modifier.blur (which blurs content) and Material elevation values.
object WolfEffects {
    const val GlassOpacity = 0.68f
    val BackgroundBlurRadius = 20.dp
    val ShadowOffsetX = 0.dp
    val ShadowOffsetY = 12.dp
    val ShadowRadius = 32.dp
    val ShadowSpread = 0.dp
    val ShadowColor = Color.Black.copy(alpha = 0.36f)
}
