package ru.missclick.wolfwisdom.presentation.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfEffects
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

/**
 * Figma Action / IconButton: a glass surface, 40dp by default, with a 24dp content slot.
 * Accepts Image, Icon, or other content. Icons inherit the moonlight content color;
 * images retain their original colors. Pass null descriptions to decorative slot
 * content because [contentDescription] labels the button itself.
 *
 * The translucent fill is self-contained. Backdrop blur, when needed, belongs to
 * the background owner; blurring this component would also blur its content.
 */
@Composable
fun WolfIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = WolfSpacing.Space40,
    containerColor: Color = WolfColors.Glass,
    iconSize: Dp = WolfSpacing.Space24,
    hapticFeedbackEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    Surface(
        onClick = {
            if (hapticFeedbackEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .alpha(if (enabled) 1f else 0.38f)
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = WolfEffects.ShadowRadius,
                    spread = WolfEffects.ShadowSpread,
                    color = WolfEffects.ShadowColor,
                    offset = DpOffset(WolfEffects.ShadowOffsetX, WolfEffects.ShadowOffsetY)
                )
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        shape = CircleShape,
        color = containerColor,
        contentColor = WolfColors.IconMoon,
        border = BorderStroke(WolfLayout.Hairline, WolfColors.GlassBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
                propagateMinConstraints = true
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050C14)
@Composable
private fun WolfIconButtonPreview() {
    WolfWisdomTheme {
        Row(
            modifier = Modifier.padding(WolfSpacing.Space24),
            horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)
        ) {
            WolfIconButton(onClick = {}, contentDescription = "More options") {
                Icon(Icons.Default.MoreHoriz, contentDescription = null)
            }
            WolfIconButton(onClick = {}, contentDescription = "Wolf profile") {
                Image(
                    painter = painterResource(R.drawable.brand_moonlit_wolf),
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }
            WolfIconButton(
                onClick = {},
                contentDescription = "More options",
                enabled = false
            ) {
                Icon(Icons.Default.MoreHoriz, contentDescription = null)
            }
        }
    }
}
