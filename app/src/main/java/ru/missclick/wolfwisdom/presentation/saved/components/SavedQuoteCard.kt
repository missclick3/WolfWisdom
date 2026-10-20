package ru.missclick.wolfwisdom.presentation.saved.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.presentation.components.MountainDivider
import ru.missclick.wolfwisdom.presentation.components.WolfIconButton
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import kotlin.math.roundToInt

@Composable
internal fun SavedQuoteCard(
    quote: Quote,
    artwork: ImageBitmap,
    sceneIndex: Int,
    isRemoving: Boolean,
    onRemoveClick: () -> Unit,
    onShareClick: () -> Unit,
    onWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scene = sceneIndex % WolfLayout.SavedCardHeights.size
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WolfRadii.Medium),
        color = WolfColors.Background,
        border = BorderStroke(WolfLayout.Hairline, WolfColors.Border)
    ) {
        BoxWithConstraints(
            modifier = Modifier.heightIn(min = WolfLayout.SavedCardHeights[scene]),
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
                    end = 64.dp,
                    top = WolfSpacing.Space24,
                    bottom = WolfSpacing.Space16
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quote.quote,
                    style = WolfTypography.Quote,
                    color = WolfColors.Text,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.quote_author),
                    style = WolfTypography.Label,
                    color = WolfColors.Muted,
                    textAlign = TextAlign.Center
                )
                MountainDivider(
                    Modifier.padding(end = WolfSpacing.Space48)
                        .widthIn(max = 128.dp)
                        .fillMaxWidth()
                )
            }
            WolfIconButton(
                onClick = onRemoveClick,
                contentDescription = stringResource(R.string.remove_quote_from_saved),
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = WolfSpacing.Space24, end = WolfSpacing.Space8)
                    .semantics { selected = true },
                enabled = !isRemoving,
                containerColor = WolfColors.Pressed
            ) {
                Icon(painterResource(R.drawable.ic_quote_saved), contentDescription = null)
            }
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(
                    end = WolfSpacing.Space8,
                    bottom = WolfSpacing.Space4
                ),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                WolfIconButton(
                    onClick = onShareClick,
                    contentDescription = stringResource(R.string.share),
                    containerColor = WolfColors.SavedGlass
                ) {
                    Icon(painterResource(R.drawable.ic_quote_share), contentDescription = null)
                }
                WolfIconButton(
                    onClick = onWidgetClick,
                    contentDescription = stringResource(R.string.set_widget),
                    containerColor = WolfColors.SavedGlass
                ) {
                    Icon(painterResource(R.drawable.ic_quote_widget), contentDescription = null)
                }
            }
        }
    }
}
