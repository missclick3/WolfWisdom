package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@Composable
fun QuoteComponent(
    modifier: Modifier = Modifier,
    quote: String,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onWidgetClick: () -> Unit,
    showActions: Boolean = true,
    minHeight: Dp = 0.dp,
    quoteStyle: TextStyle = WolfTypography.HeroQuote
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WolfColors.SavedGlass, RoundedCornerShape(WolfRadii.Medium))
            .heightIn(min = minHeight)
            .padding(
                horizontal = if (showActions) WolfSpacing.Space16 else WolfSpacing.Space20,
                vertical = WolfSpacing.Space16
            ),
        verticalArrangement = if (showActions) Arrangement.Top else Arrangement.spacedBy(
            WolfSpacing.Space12, Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = quote,
            modifier = Modifier.fillMaxWidth(),
            style = quoteStyle,
            color = WolfColors.Text,
            textAlign = if (showActions) TextAlign.Start else TextAlign.Center
        )
        if (!showActions) {
            Text(
                text = stringResource(R.string.quote_author),
                style = WolfTypography.Label,
                color = WolfColors.Muted,
                textAlign = TextAlign.Center
            )
            MountainDivider()
        } else {
            // Mirror the action rail's width on the left to keep the author and divider
            // centered under the quote, while reserving space for the buttons on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space12),
                verticalAlignment = Alignment.Top
            ) {
                Spacer(Modifier.width(QuoteActionWidth))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.quote_author),
                        modifier = Modifier.padding(vertical = WolfSpacing.Space16),
                        style = WolfTypography.Label,
                        color = WolfColors.Muted,
                        textAlign = TextAlign.Center
                    )
                    MountainDivider()
                }
                QuoteActionButtons(
                    isSaved = isSaved,
                    onSaveClick = onSaveClick,
                    onShareClick = onShareClick,
                    onWidgetClick = onWidgetClick
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050C14, widthDp = 350)
@Preview(name = "Long quote, large font", showBackground = true,
    backgroundColor = 0xFF050C14, widthDp = 280, fontScale = 1.5f)
@Composable
private fun QuoteComponentPreview() {
    WolfWisdomTheme {
        QuoteComponent(
            quote = "Волк не тот, кто волк, а тот, кто волк. Мудрость приходит к тому, кто умеет ждать.",
            isSaved = true,
            onSaveClick = {},
            onShareClick = {},
            onWidgetClick = {}
        )
    }
}
