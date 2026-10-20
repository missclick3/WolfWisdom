package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

/** Shared decorative divider for quotes and saved cards. */
@Composable
fun MountainDivider(
    modifier: Modifier = Modifier,
    color: Color = WolfColors.Muted
) {
    Row(
        modifier = modifier.width(128.dp).height(WolfSpacing.Space20),
        horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.6.dp,
            color = color
        )
        Icon(
            painter = painterResource(R.drawable.ic_quote_mountain),
            contentDescription = null,
            modifier = Modifier.size(width = 28.dp, height = 20.dp),
            tint = color
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.6.dp,
            color = color
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF142637)
@Composable
private fun MountainDividerPreview() {
    WolfWisdomTheme {
        MountainDivider()
    }
}
