package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

/** Shared action-error message. Callers supply localized text; long messages wrap naturally. */
@Composable
fun WolfErrorSnackbar(
    headline: String,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    Surface(
        modifier = modifier.widthIn(max = 480.dp).fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(WolfSpacing.Space16),
        color = WolfColors.ErrorSnackbarSurface,
        border = BorderStroke(1.dp, WolfColors.ErrorSnackbarBorder)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(
                start = WolfSpacing.Space16,
                end = WolfSpacing.Space16,
                top = 15.dp,
                bottom = 18.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space12)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_snackbar_error),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = headline,
                    style = WolfTypography.ErrorSnackbarHeadline,
                    color = WolfColors.ErrorSnackbarText
                )
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = WolfTypography.ErrorSnackbarDetail,
                        color = WolfColors.ErrorSnackbarDetail
                    )
                }
            }
        }
    }
}

@Preview(name = "Compact error", widthDp = 390, locale = "ru")
@Preview(name = "Wide error", widthDp = 844, locale = "en")
@Preview(name = "Large text", widthDp = 390, fontScale = 2f, locale = "ru")
@Composable
private fun WolfErrorSnackbarPreview() {
    WolfWisdomTheme {
        WolfErrorSnackbar(
            headline = stringResource(R.string.saved_remove_error),
            detail = stringResource(R.string.error_snackbar_detail),
            modifier = Modifier.padding(WolfSpacing.Space20)
        )
    }
}
