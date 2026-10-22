package ru.missclick.wolfwisdom.ui.theme

import androidx.compose.ui.unit.dp

object WolfSpacing {
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
    val Space40 = 40.dp
    val Space48 = 48.dp
    val Space64 = 64.dp
}

object WolfRadii {
    val Small = 12.dp
    val Medium = 20.dp
    val Large = 28.dp
    val Pill = 999.dp
}

object WolfLayout {
    // Reference canvas dimensions; layouts should still adapt to screen size.
    val ReferenceWidth = 390.dp
    val ReferenceHeight = 844.dp
    const val Columns = 4
    val HorizontalMargin = WolfSpacing.Space20
    val Gutter = WolfSpacing.Space12
    val Hairline = 1.dp
    val SavedCardHeight = 176.dp
    val SavedCardHeights = listOf(SavedCardHeight, 156.dp, 156.dp, 164.dp)
}
