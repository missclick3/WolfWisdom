package ru.missclick.wolfwisdom.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

enum class WolfMenuAction(
    @StringRes val labelRes: Int,
    @DrawableRes internal val iconRes: Int? = null
) {
    SAVED(R.string.saved),
    ACHIEVEMENTS(R.string.achievements, R.drawable.ic_menu_achievements),
    SUPPORT(R.string.donate, R.drawable.ic_menu_support),
    CONTACTS(R.string.contacts, R.drawable.ic_menu_contacts),
    ABOUT(R.string.about_app, R.drawable.ic_menu_about)
}

/** Anchored to the button's Box. Material keeps the popup alive for its exit animation. */
@Composable
fun WolfOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (WolfMenuAction) -> Unit = {}
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(200.dp),
        shape = RoundedCornerShape(WolfRadii.Small),
        containerColor = WolfColors.Glass,
        tonalElevation = 0.dp,
        shadowElevation = WolfSpacing.Space12,
        border = BorderStroke(WolfLayout.Hairline, WolfColors.GlassBorder),
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        WolfMenuAction.entries.forEachIndexed { index, action ->
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WolfSpacing.Space48)
                        .clickable(role = Role.Button) {
                            onDismissRequest()
                            onItemClick(action)
                        }
                        .padding(
                            start = WolfSpacing.Space16,
                            end = WolfSpacing.Space16,
                            top = WolfSpacing.Space4,
                            bottom = WolfSpacing.Space4
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconModifier = Modifier.size(WolfSpacing.Space24)
                    val iconRes = action.iconRes
                    if (iconRes == null) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = iconModifier,
                            tint = WolfColors.Secondary
                        )
                    } else {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = iconModifier,
                            tint = WolfColors.Secondary
                        )
                    }
                    Spacer(Modifier.width(WolfSpacing.Space12))
                    Text(
                        text = stringResource(action.labelRes),
                        modifier = Modifier.weight(1f),
                        style = WolfTypography.Body,
                        color = WolfColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index < WolfMenuAction.entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = WolfSpacing.Space12),
                        thickness = 0.5.dp,
                        color = WolfColors.Muted
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 320, locale = "ru")
@Composable
private fun WolfOverflowMenuPreview() {
    WolfWisdomTheme {
        WolfWisdomScreen {
            WolfHeader(isOptionsVisible = true)
        }
    }
}
