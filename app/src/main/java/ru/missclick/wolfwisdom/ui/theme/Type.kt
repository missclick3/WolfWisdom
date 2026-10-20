package ru.missclick.wolfwisdom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.missclick.wolfwisdom.R

val Inter = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.Normal),
    Font(R.font.inter_variable, weight = FontWeight.Medium)
)
val Tinos = FontFamily(Font(R.font.tinos_regular, weight = FontWeight.Normal))
val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_variable, weight = FontWeight.Medium)
)

// Raw Figma variables. WW/Title overrides its 20sp size variable with 22sp.
object WolfTypeScale {
    val DisplaySize = 36.sp
    val DisplayLineHeight = 42.sp
    val QuoteSize = 18.sp
    val QuoteLineHeight = 21.sp
    val TitleSize = 20.sp
    val TitleLineHeight = 26.sp
    val BodySize = 15.sp
    val BodyLineHeight = 22.sp
    val LabelSize = 13.sp
    val LabelLineHeight = 18.sp
    val CaptionSize = 11.sp
    val CaptionLineHeight = 16.sp
}

// Exact WW styles, including the serif quote role absent from Material.
object WolfTypography {
    val Display = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.Medium,
        fontSize = WolfTypeScale.DisplaySize,
        lineHeight = WolfTypeScale.DisplayLineHeight,
        letterSpacing = 0.sp
    )
    val Quote = TextStyle(
        fontFamily = Tinos,
        fontWeight = FontWeight.Normal,
        fontSize = WolfTypeScale.QuoteSize,
        lineHeight = WolfTypeScale.QuoteLineHeight,
        letterSpacing = 0.sp
    )
    val HeroQuote = Quote.copy(fontSize = 32.sp, lineHeight = 36.sp)
    val Title = Quote.copy(fontSize = 22.sp, lineHeight = WolfTypeScale.TitleLineHeight)
    val Body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = WolfTypeScale.BodySize,
        lineHeight = WolfTypeScale.BodyLineHeight,
        letterSpacing = 0.sp
    )
    val Label = Body.copy(
        fontWeight = FontWeight.Medium,
        fontSize = WolfTypeScale.LabelSize,
        lineHeight = WolfTypeScale.LabelLineHeight
    )
    val Caption = Body.copy(
        fontSize = WolfTypeScale.CaptionSize,
        lineHeight = WolfTypeScale.CaptionLineHeight
    )
}

// Material roles reuse the foundation styles instead of Roboto defaults.
val Typography = Typography(
    displayLarge = WolfTypography.Display,
    displayMedium = WolfTypography.Display,
    displaySmall = WolfTypography.Display,
    headlineLarge = WolfTypography.Title,
    headlineMedium = WolfTypography.Title,
    headlineSmall = WolfTypography.Title,
    titleLarge = WolfTypography.Title,
    titleMedium = WolfTypography.Title,
    titleSmall = WolfTypography.Title,
    bodyLarge = WolfTypography.Body,
    bodyMedium = WolfTypography.Body,
    bodySmall = WolfTypography.Caption,
    labelLarge = WolfTypography.Label,
    labelMedium = WolfTypography.Label,
    labelSmall = WolfTypography.Caption
)
