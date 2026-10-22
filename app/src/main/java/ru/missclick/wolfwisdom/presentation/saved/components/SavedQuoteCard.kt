package ru.missclick.wolfwisdom.presentation.saved.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.presentation.components.QuoteLandscapeCard
import ru.missclick.wolfwisdom.presentation.components.WolfIconButton
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing

@Composable
internal fun SavedQuoteCard(
    quote: Quote,
    artwork: ImageBitmap,
    sceneIndex: Int,
    isRemoving: Boolean,
    onRemoveClick: () -> Unit,
    onShareClick: () -> Unit,
    onWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuoteLandscapeCard(
        quote = quote.quote,
        artwork = artwork,
        sceneIndex = sceneIndex,
        modifier = modifier
    ) {
        WolfIconButton(
            onClick = onRemoveClick,
            contentDescription = stringResource(R.string.remove_quote_from_saved),
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = WolfSpacing.Space24, end = WolfSpacing.Space8)
                .semantics { selected = true },
            enabled = !isRemoving,
            containerColor = WolfColors.Pressed
        ) {
            Icon(painterResource(R.drawable.ic_quote_saved), contentDescription = null)
        }
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(
                end = WolfSpacing.Space8,
                bottom = WolfSpacing.Space4
            ),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            WolfIconButton(
                onClick = onShareClick,
                contentDescription = stringResource(R.string.share),
                containerColor = WolfColors.SavedGlass
            ) {
                Icon(painterResource(R.drawable.ic_quote_share), contentDescription = null)
            }
            WolfIconButton(
                onClick = onWidgetClick,
                contentDescription = stringResource(R.string.set_widget),
                containerColor = WolfColors.SavedGlass
            ) {
                Icon(painterResource(R.drawable.ic_quote_widget), contentDescription = null)
            }
        }
    }
}
