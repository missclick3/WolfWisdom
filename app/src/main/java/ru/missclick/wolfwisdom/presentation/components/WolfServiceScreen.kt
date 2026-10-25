package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.*

internal data class WolfGlassBackdrop(
    val painter: Painter,
    val size: IntSize,
    val origin: Offset,
    val contrast: Brush
)

/** Shared background, safe insets and centered header for service screens. */
@Composable
internal fun WolfServiceScreen(
    title: String,
    onBackClick: () -> Unit,
    device: DeviceConfiguration = currentDeviceConfiguration(),
    content: @Composable (WolfGlassBackdrop) -> Unit
) {
    val tablet = !device.isMobile
    val landscape = device == DeviceConfiguration.MOBILE_LANDSCAPE ||
        device == DeviceConfiguration.TABLET_LANDSCAPE
    val background = painterResource(when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> R.drawable.moonlit_wolf_background
        DeviceConfiguration.MOBILE_LANDSCAPE -> R.drawable.moonlit_wolf_phone_landscape_background
        DeviceConfiguration.TABLET_PORTRAIT -> R.drawable.moonlit_wolf_tablet_portrait_background
        DeviceConfiguration.TABLET_LANDSCAPE -> R.drawable.moonlit_wolf_landscape_background
    })
    val contrast = Brush.verticalGradient(listOf(
        Color(0xFF000408).copy(alpha = 0.28f),
        Color(0xFF000408).copy(alpha = 0.02f)
    ))
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }

    WolfWisdomScreen {
        Box(Modifier.fillMaxSize().onGloballyPositioned {
            screenSize = it.size
            screenOrigin = it.positionInRoot()
        }.background(contrast).safeDrawingPadding()) {
            Box(Modifier.fillMaxSize().padding(top = 60.dp)) {
                content(WolfGlassBackdrop(background, screenSize, screenOrigin, contrast))
            }
            Box(Modifier.fillMaxWidth().height(60.dp)) {
                Text(
                    title,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 72.dp),
                    color = WolfColors.Text,
                    style = WolfTypography.Title.copy(
                        fontSize = if (tablet) 28.sp else 22.sp,
                        lineHeight = if (tablet) 39.sp else 31.sp
                    ),
                    textAlign = TextAlign.Center
                )
                WolfIconButton(
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(start = if (landscape) 22.dp else 14.dp),
                    size = 44.dp,
                    containerColor = WolfColors.SavedGlass
                ) {
                    Icon(painterResource(R.drawable.ic_saved_back), null)
                }
            }
        }
    }
}

/** Reuses the screen image beneath the glass while keeping foreground content sharp. */
@Composable
internal fun WolfGlassCard(
    backdrop: WolfGlassBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(WolfRadii.Medium)
    var position by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned {
        position = it.positionInRoot() - backdrop.origin
    }.dropShadow(shape, Shadow(
        radius = WolfEffects.ShadowRadius,
        color = WolfEffects.ShadowColor,
        offset = DpOffset(WolfEffects.ShadowOffsetX, WolfEffects.ShadowOffsetY)
    )).clip(shape).border(WolfLayout.Hairline, WolfColors.GlassBorder, shape)) {
        // Repaint the matching part of the screen behind the glass. Only this
        // background layer is blurred; foreground content stays sharp.
        Canvas(Modifier.matchParentSize().blur(10.dp)) {
            if (backdrop.size.width > 0 && backdrop.size.height > 0) {
                val scale = maxOf(backdrop.size.width / backdrop.painter.intrinsicSize.width,
                    backdrop.size.height / backdrop.painter.intrinsicSize.height)
                val imageSize = backdrop.painter.intrinsicSize * scale
                translate(
                    (backdrop.size.width - imageSize.width) / 2 - position.x,
                    (backdrop.size.height - imageSize.height) / 2 - position.y
                ) {
                    with(backdrop.painter) { draw(imageSize) }
                }
                translate(-position.x, -position.y) {
                    drawRect(backdrop.contrast, size = Size(backdrop.size.width.toFloat(), backdrop.size.height.toFloat()))
                }
            }
        }
        Box(Modifier.matchParentSize().background(WolfColors.SavedGlass))
        content()
    }
}
