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
    onMenuItemClick: (WolfMenuAction) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Space16),
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
                style = WolfTypography.Title,
                color = WolfColors.Text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = WolfTypography.Body,
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
        Spacer(Modifier.width(Space16))
    }
}
