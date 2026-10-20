package ru.missclick.wolfwisdom.presentation.saved.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography

@Composable
internal fun SavedEmptyState(isSearchResult: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WolfRadii.Medium),
        color = WolfColors.SavedGlass,
        border = BorderStroke(WolfLayout.Hairline, WolfColors.GlassBorder),
        shadowElevation = WolfSpacing.Space12
    ) {
        Column(
            modifier = Modifier.heightIn(min = 210.dp).padding(WolfSpacing.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space12)
        ) {
            Icon(
                painter = painterResource(
                    if (isSearchResult) R.drawable.ic_saved_search else R.drawable.ic_saved_bookmark
                ),
                contentDescription = null,
                tint = WolfColors.IconMoon,
                modifier = Modifier.size(WolfSpacing.Space24)
            )
            Text(
                text = stringResource(
                    if (isSearchResult) R.string.search_nothing_found_title else R.string.empty_saved_title
                ),
                style = WolfTypography.Title,
                color = WolfColors.Text,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    if (isSearchResult) R.string.search_nothing_found_desc else R.string.empty_saved_description
                ),
                style = WolfTypography.Body,
                color = WolfColors.Secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
