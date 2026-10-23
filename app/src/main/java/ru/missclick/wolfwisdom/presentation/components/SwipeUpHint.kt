package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme
import kotlin.time.Duration.Companion.milliseconds

private const val JumpIntervalMillis = 12_000L
private const val JumpRiseMillis = 180
private const val JumpReturnMillis = 300

/** Visual guidance for the quote feed's swipe gesture. */
@Composable
fun SwipeUpHint(modifier: Modifier = Modifier, isVisible: Boolean = true) {
    val arrowOffset = remember { Animatable(0f) }
    val jumpHeight = with(LocalDensity.current) { 6.dp.toPx() }

    LaunchedEffect(jumpHeight, isVisible) {
        arrowOffset.snapTo(0f)
        if (!isVisible) return@LaunchedEffect
        delay(JumpIntervalMillis.milliseconds)
        while (isActive) {
            arrowOffset.animateTo(
                targetValue = -jumpHeight,
                animationSpec = tween(JumpRiseMillis, easing = FastOutSlowInEasing)
            )
            arrowOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(JumpReturnMillis, easing = FastOutSlowInEasing)
            )
            delay((JumpIntervalMillis - JumpRiseMillis - JumpReturnMillis).milliseconds)
        }
    }

    Column(
        modifier = modifier
            .graphicsLayer { alpha = if (isVisible) 1f else 0f }
            .then(if (isVisible) Modifier.semantics(mergeDescendants = true) {}
                else Modifier.clearAndSetSemantics {}),
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
                modifier = Modifier
                    .size(WolfSpacing.Space24)
                    .graphicsLayer { translationY = arrowOffset.value },
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
