package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/** Visual guidance for the quote feed's swipe gesture. */
@Composable
fun SwipeUpHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(WolfSpacing.Space32),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_swipe_up),
                contentDescription = null,
                modifier = Modifier.size(WolfSpacing.Space24),
                tint = WolfColors.IconMoon
            )
        }
        Text(
            text = stringResource(R.string.swipe_for_next),
            modifier = Modifier.widthIn(max = 220.dp),
            style = WolfTypography.Caption.copy(lineHeight = 15.sp),
            color = WolfColors.Hint,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050C14, locale = "ru")
@Preview(name = "English", showBackground = true, backgroundColor = 0xFF050C14, locale = "en")
@Preview(name = "Large font", showBackground = true, backgroundColor = 0xFF050C14,
    locale = "ru", fontScale = 1.5f)
@Composable
private fun SwipeUpHintPreview() {
    WolfWisdomTheme {
        SwipeUpHint()
    }
}
