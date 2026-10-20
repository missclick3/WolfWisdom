package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

private const val MenuIconAnimationDurationMillis = 240

/** Decorative icon; its containing button owns the click action and accessible label. */
@Composable
fun WolfMenuToggleIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    // A shared progress keeps both glyphs synchronized and supports mid-animation reversal.
    val progress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = MenuIconAnimationDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "Wolf menu icon"
    )

    Box(modifier = modifier.size(WolfSpacing.Space24)) {
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = 1f - progress
                    rotationZ = 90f * progress
                    scaleX = 1f - 0.2f * progress
                    scaleY = scaleX
                }
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = progress
                    rotationZ = -90f * (1f - progress)
                    scaleX = 0.8f + 0.2f * progress
                    scaleY = scaleX
                }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050C14)
@Composable
private fun WolfMenuToggleIconPreview() {
    WolfWisdomTheme {
        var isExpanded by remember { mutableStateOf(false) }
        WolfIconButton(
            onClick = { isExpanded = !isExpanded },
            contentDescription = stringResource(
                if (isExpanded) R.string.close_options else R.string.more_options
            )
        ) {
            WolfMenuToggleIcon(isExpanded = isExpanded)
        }
    }
}
