package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

/** The Figma wolf brand mark at its original 48dp size by default. */
@Composable
fun WolfLogoBrand(
    modifier: Modifier = Modifier,
    size: Dp = WolfSpacing.Space48,
    contentDescription: String? = stringResource(R.string.app_name)
) {
    Image(
        painter = painterResource(R.drawable.brand_moonlit_wolf),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF050C14)
@Composable
private fun WolfLogoBrandPreview() {
    WolfWisdomTheme {
        WolfLogoBrand()
    }
}
