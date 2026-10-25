package ru.missclick.wolfwisdom.presentation.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.WolfIconButton
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.*

/** Presentation only: both controls show the enabled state from the design. */
@Composable
fun SettingsScreen(onBackClick: () -> Unit = {}) {
    val device = currentDeviceConfiguration()
    val tablet = !device.isMobile
    val landscape = device == DeviceConfiguration.MOBILE_LANDSCAPE ||
        device == DeviceConfiguration.TABLET_LANDSCAPE
    val columnWidth = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 358.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 420.dp
        else -> 520.dp
    }
    val contentTop = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 92.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 80.dp
        DeviceConfiguration.TABLET_PORTRAIT -> 132.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 148.dp
    }
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
            // The body scrolls independently, keeping Back accessible on short windows.
            Column(
                Modifier.fillMaxSize().padding(top = 60.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = WolfSpacing.Space16)
                    .padding(top = contentTop - 60.dp, bottom = WolfSpacing.Space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space12)
            ) {
                SettingsCard(
                    title = stringResource(R.string.settings_sound),
                    description = stringResource(R.string.settings_sound_description),
                    icon = R.drawable.ic_settings_sound,
                    modifier = Modifier.widthIn(max = columnWidth).fillMaxWidth(),
                    minHeight = if (tablet) 116.dp else 100.dp,
                    background = background, screenSize = screenSize,
                    screenOrigin = screenOrigin, contrast = contrast
                )
                SettingsCard(
                    title = stringResource(R.string.settings_vibration),
                    description = stringResource(R.string.settings_vibration_description),
                    icon = R.drawable.ic_settings_vibration,
                    modifier = Modifier.widthIn(max = columnWidth).fillMaxWidth(),
                    minHeight = if (tablet) 116.dp else 100.dp,
                    background = background, screenSize = screenSize,
                    screenOrigin = screenOrigin, contrast = contrast
                )
            }
            Box(Modifier.fillMaxWidth().height(60.dp)) {
                Text(
                    stringResource(R.string.settings),
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

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    minHeight: Dp,
    background: Painter,
    screenSize: IntSize,
    screenOrigin: Offset,
    contrast: Brush,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(WolfRadii.Medium)
    var position by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.heightIn(min = minHeight).onGloballyPositioned {
        position = it.positionInRoot() - screenOrigin
    }.dropShadow(shape, Shadow(
        radius = WolfEffects.ShadowRadius,
        color = WolfEffects.ShadowColor,
        offset = DpOffset(WolfEffects.ShadowOffsetX, WolfEffects.ShadowOffsetY)
    )).clip(shape).border(WolfLayout.Hairline, WolfColors.GlassBorder, shape)) {
        // Repaint the matching part of the screen behind the glass. Only this
        // background layer is blurred; text, icons and the switch stay sharp.
        Canvas(Modifier.matchParentSize().blur(10.dp)) {
            if (screenSize.width > 0 && screenSize.height > 0) {
                val scale = maxOf(screenSize.width / background.intrinsicSize.width,
                    screenSize.height / background.intrinsicSize.height)
                val imageSize = background.intrinsicSize * scale
                translate(
                    (screenSize.width - imageSize.width) / 2 - position.x,
                    (screenSize.height - imageSize.height) / 2 - position.y
                ) {
                    with(background) { draw(imageSize) }
                }
                translate(-position.x, -position.y) {
                    drawRect(contrast, size = Size(screenSize.width.toFloat(), screenSize.height.toFloat()))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight)
                .background(WolfColors.SavedGlass)
                .padding(horizontal = WolfSpacing.Space20, vertical = WolfSpacing.Space16)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)
        ) {
            // Figma's 28dp leading slot contains the original 24dp artwork.
            Box(Modifier.size(28.dp), contentAlignment = Alignment.TopStart) {
                Image(painterResource(icon), null, Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space4)) {
                Text(title, style = WolfTypography.Body, color = WolfColors.Text)
                Text(description, style = WolfTypography.Caption, color = WolfColors.Secondary)
            }
            Image(
                painterResource(R.drawable.ic_settings_toggle_on), null,
                Modifier.size(width = 48.dp, height = 28.dp).semantics {
                    role = Role.Switch
                    toggleableState = ToggleableState.On
                }
            )
        }
    }
}

@Preview(name = "Settings · Phone portrait", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Settings · Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Settings · Tablet portrait", widthDp = 834, heightDp = 1112, locale = "ru")
@Preview(name = "Settings · Tablet landscape", widthDp = 1112, heightDp = 834, locale = "ru")
@Composable
private fun SettingsPreview() {
    WolfWisdomTheme { SettingsScreen() }
}
