package ru.missclick.wolfwisdom.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

private val QuoteActionButtonSize = 44.dp
internal val QuoteActionWidth = 72.dp
private val QuoteActionCaption = WolfTypography.Caption.copy(
    fontSize = 10.sp,
    lineHeight = 14.sp
)

/** Save state is owned by the caller; each action invokes its supplied callback. */
@Composable
fun QuoteActionButtons(
    modifier: Modifier = Modifier,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onWidgetClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.width(QuoteActionWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        // 48dp touch targets add 4dp around the 44dp visuals, retaining a 12dp visual gap.
        verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space8)
    ) {
        WolfIconButton(
            onClick = onSaveClick,
            enabled = enabled,
            contentDescription = stringResource(
                if (isSaved) R.string.remove_quote_from_saved else R.string.save_quote
            ),
            modifier = Modifier.semantics { selected = isSaved },
            size = QuoteActionButtonSize,
            containerColor = if (isSaved) WolfColors.Pressed else WolfColors.Glass
        ) {
            if (isSaved) {
                Icon(
                    painter = painterResource(R.drawable.ic_quote_saved),
                    contentDescription = null
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null
                )
            }
        }
        QuoteActionWithLabel(
            iconRes = R.drawable.ic_quote_share,
            labelRes = R.string.share,
            onClick = onShareClick,
            enabled = enabled
        )
        QuoteActionWithLabel(
            iconRes = R.drawable.ic_quote_widget,
            labelRes = R.string.set_widget,
            onClick = onWidgetClick,
            enabled = enabled
        )
    }
}

@Composable
private fun QuoteActionWithLabel(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val label = stringResource(labelRes)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space4)
    ) {
        WolfIconButton(
            onClick = onClick,
            enabled = enabled,
            contentDescription = label,
            size = QuoteActionButtonSize
        ) {
            Icon(painter = painterResource(iconRes), contentDescription = null)
        }
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { },
            style = QuoteActionCaption,
            color = WolfColors.Text,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Preview(name = "Russian", widthDp = 104, heightDp = 240, locale = "ru")
@Preview(name = "English", widthDp = 104, heightDp = 240, locale = "en")
@Composable
private fun QuoteActionButtonsPreview() {
    WolfWisdomTheme {
        var isSaved by remember { mutableStateOf(true) }
        WolfWisdomScreen {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WolfSpacing.Space16),
                contentAlignment = Alignment.Center
            ) {
                QuoteActionButtons(
                    isSaved = isSaved,
                    onSaveClick = { isSaved = !isSaved },
                    onShareClick = {},
                    onWidgetClick = {}
                )
            }
        }
    }
}
