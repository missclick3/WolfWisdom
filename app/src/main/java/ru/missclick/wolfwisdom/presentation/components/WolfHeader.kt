package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing.Space12
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing.Space16
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing.Space4
import ru.missclick.wolfwisdom.ui.theme.WolfTypography

@Composable
fun WolfHeader(
    modifier: Modifier = Modifier,
    isOptionsVisible: Boolean = false,
    onOptionsClick: () -> Unit = {},
    onOptionsDismissRequest: () -> Unit = {},
    onMenuItemClick: (WolfMenuAction) -> Unit = {},
    compact: Boolean = false,
    horizontalPadding: Dp = Space16
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = Space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WolfLogoBrand()
        Spacer(Modifier.width(Space12))
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(Space4)
        ) {
            Text(
                text = stringResource(R.string.app_title),
                style = if (compact) WolfTypography.Title.copy(fontSize = 18.sp, lineHeight = 22.sp)
                    else WolfTypography.Title,
                color = WolfColors.Text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = stringResource(R.string.app_subtitle).let { if (compact) it.uppercase() else it },
                style = if (compact) WolfTypography.Body.copy(
                    fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp
                ) else WolfTypography.Body,
                color = WolfColors.Muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
        Spacer(Modifier.width(Space12))
        Box {
            WolfIconButton(
                onClick = onOptionsClick,
                contentDescription = stringResource(
                    if (isOptionsVisible) R.string.close_options else R.string.more_options
                )
            ) {
                WolfMenuToggleIcon(isExpanded = isOptionsVisible)
            }
            WolfOverflowMenu(
                expanded = isOptionsVisible,
                onDismissRequest = onOptionsDismissRequest,
                onItemClick = onMenuItemClick
            )
        }
        if (!compact) Spacer(Modifier.width(Space16))
    }
}
