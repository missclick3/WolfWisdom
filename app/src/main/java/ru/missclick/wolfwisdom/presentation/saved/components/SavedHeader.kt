package ru.missclick.wolfwisdom.presentation.saved.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.WolfIconButton
import ru.missclick.wolfwisdom.presentation.saved.SavedAction
import ru.missclick.wolfwisdom.presentation.saved.SavedState
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography

@Composable
internal fun SavedHeader(
    state: SavedState,
    onAction: (SavedAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(
            horizontal = WolfSpacing.Space12,
            vertical = WolfSpacing.Space16
        ),
        horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WolfIconButton(
            onClick = onBackClick,
            contentDescription = stringResource(R.string.navigate_back),
            containerColor = WolfColors.SavedGlass,
            size = 44.dp
        ) {
            Icon(painterResource(R.drawable.ic_saved_back), contentDescription = null)
        }
        if (state.isSearchVisible) {
            SavedSearchField(
                query = state.searchQuery,
                onQueryChange = { onAction(SavedAction.SearchQueryChanged(it)) },
                onCloseClick = { onAction(SavedAction.CloseSearch) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = stringResource(R.string.saved),
                modifier = Modifier.weight(1f),
                style = WolfTypography.Title,
                color = WolfColors.Text,
                textAlign = TextAlign.Center
            )
            if (state.quotes.isNotEmpty()) {
                WolfIconButton(
                    onClick = { onAction(SavedAction.OpenSearch) },
                    contentDescription = stringResource(R.string.open_saved_search),
                    containerColor = WolfColors.SavedGlass,
                    size = 44.dp
                ) {
                    Icon(painterResource(R.drawable.ic_saved_search), contentDescription = null)
                }
            } else {
                Spacer(Modifier.size(WolfSpacing.Space48))
            }
        }
    }
}

@Composable
private fun SavedSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val hint = stringResource(R.string.search)
    val textStyle = WolfTypography.Quote.copy(fontSize = 16.sp, lineHeight = 22.sp)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        modifier = modifier.height(WolfSpacing.Space48),
        shape = RoundedCornerShape(WolfRadii.Pill),
        color = WolfColors.SavedGlass,
        border = BorderStroke(WolfLayout.Hairline, WolfColors.GlassBorder),
        shadowElevation = WolfSpacing.Space12
    ) {
        Row(
            modifier = Modifier.padding(start = WolfSpacing.Space12),
            horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_saved_search),
                contentDescription = null,
                tint = WolfColors.IconMoon,
                modifier = Modifier.size(WolfSpacing.Space24)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester)
                    .semantics { contentDescription = hint },
                textStyle = textStyle.copy(color = WolfColors.Text),
                singleLine = true,
                cursorBrush = SolidColor(WolfColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }),
                decorationBox = { field ->
                    Box {
                        if (query.isEmpty()) {
                            Text(text = hint, style = textStyle, color = WolfColors.Secondary)
                        }
                        field()
                    }
                }
            )
            WolfIconButton(
                onClick = {
                    keyboard?.hide()
                    onCloseClick()
                },
                contentDescription = stringResource(R.string.close_saved_search),
                containerColor = WolfColors.SavedGlass,
                size = 44.dp
            ) {
                Icon(painterResource(R.drawable.ic_saved_close), contentDescription = null)
            }
        }
    }
}
