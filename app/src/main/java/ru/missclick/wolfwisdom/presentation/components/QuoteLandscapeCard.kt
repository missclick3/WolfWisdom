package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import kotlin.math.roundToInt

@Composable
internal fun QuoteLandscapeCard(
    quote: String,
    artwork: ImageBitmap,
    sceneIndex: Int,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    fixedHeight: Dp? = null,
    actions: @Composable BoxScope.() -> Unit = {}
) {
    val scene = Math.floorMod(sceneIndex, WolfLayout.SavedCardHeights.size)
    Surface(
        modifier = modifier.fillMaxWidth()
            .then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier),
        shape = RoundedCornerShape(WolfRadii.Medium * scale),
        color = WolfColors.Background,
        border = BorderStroke(WolfLayout.Hairline, WolfColors.Border)
    ) {
        BoxWithConstraints(
            modifier = Modifier.heightIn(min = WolfLayout.SavedCardHeights[scene] * scale),
            contentAlignment = Alignment.CenterStart
        ) {
            // Figma supplies the four landscapes as one vertical image strip.
            Canvas(Modifier.matchParentSize()) {
                val sceneHeight = artwork.height / 4
                drawImage(
                    image = artwork,
                    srcOffset = IntOffset(0, scene * sceneHeight),
                    srcSize = IntSize(artwork.width, sceneHeight),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            }
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                0f to Color.Transparent,
                0.4f to Color(0xFF04090E).copy(alpha = 0.15f),
                1f to Color(0xFF04090E).copy(alpha = 0.84f)
            )))
            Column(
                modifier = Modifier.fillMaxWidth().padding(
                    start = maxWidth * (136f / 378f),
                    end = 64.dp * scale,
                    top = WolfSpacing.Space24 * scale,
                    bottom = WolfSpacing.Space16 * scale
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp * scale),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quote,
                    style = WolfTypography.Quote.copy(
                        fontSize = WolfTypography.Quote.fontSize * scale,
                        lineHeight = WolfTypography.Quote.lineHeight * scale
                    ),
                    color = WolfColors.Text,
                    // Reserve room for the author and divider before measuring a saved preview.
                    modifier = Modifier.fillMaxWidth()
                        .then(if (fixedHeight != null) Modifier.weight(1f, fill = false) else Modifier),
                    maxLines = if (fixedHeight != null) 4 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.quote_author),
                    style = WolfTypography.Label.copy(
                        fontSize = WolfTypography.Label.fontSize * scale,
                        lineHeight = WolfTypography.Label.lineHeight * scale
                    ),
                    color = WolfColors.Muted,
                    textAlign = TextAlign.Center
                )
                MountainDivider(
                    Modifier.padding(end = WolfSpacing.Space48 * scale)
                        .widthIn(max = 128.dp * scale)
                        .fillMaxWidth(),
                    scale = scale
                )
            }
            actions()
        }
    }
}
