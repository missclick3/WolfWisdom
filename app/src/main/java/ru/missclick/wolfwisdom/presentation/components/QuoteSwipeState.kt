package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Shared by the screen's gesture area and the card's graphics layer. */
@Stable
class QuoteSwipeState internal constructor() {
    internal val rotation = Animatable(0f)
    internal var dragAngle by mutableFloatStateOf(0f)
    internal var isDragging by mutableStateOf(false)
    internal var isAwaitingNavigation by mutableStateOf(false)
    internal var isSettling by mutableStateOf(false)

    internal val angle: Float
        get() = if (isDragging || isAwaitingNavigation) dragAngle else rotation.value

    internal suspend fun returnToRest() {
        isSettling = true
        try {
            rotation.snapTo(angle)
            isDragging = false
            isAwaitingNavigation = false
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } finally {
            isSettling = false
        }
    }
}

@Composable
fun rememberQuoteSwipeState(): QuoteSwipeState = remember { QuoteSwipeState() }

/** A release must cross the distance threshold; a short fast flick cannot commit. */
@Composable
fun Modifier.quoteSwipeGestures(
    state: QuoteSwipeState,
    enabled: Boolean,
    canGoBack: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
): Modifier {
    val scope = rememberCoroutineScope()
    val latestUp by rememberUpdatedState(onSwipeUp)
    val latestDown by rememberUpdatedState(onSwipeDown)
    val canStart = enabled && !state.isSettling && !state.isAwaitingNavigation

    return pointerInput(canStart, canGoBack) {
        if (!canStart) return@pointerInput
        val commitDistance = (size.height * 0.25f).coerceIn(140.dp.toPx(), 200.dp.toPx())
        var dragDistance = 0f

        fun settleBack() {
            if (!state.isDragging || state.isSettling) return
            state.isSettling = true
            scope.launch { state.returnToRest() }
        }

        detectVerticalDragGestures(
            onDragStart = {
                dragDistance = 0f
                state.dragAngle = 0f
                state.isDragging = true
            },
            onVerticalDrag = { change, amount ->
                change.consume()
                dragDistance += amount
                val direction = if (dragDistance < 0f) QuoteFlipDirection.Up else QuoteFlipDirection.Down
                val maxAngle = if (dragDistance > 0f && !canGoBack) 12f else 75f
                val progress = (abs(dragDistance) / commitDistance).coerceIn(0f, 1f)
                state.dragAngle = direction.rotationSign * progress * maxAngle
            },
            onDragCancel = { settleBack() },
            onDragEnd = {
                if (abs(dragDistance) >= commitDistance) {
                    // Hold the exact dragged pose while Room resolves the destination.
                    state.isAwaitingNavigation = true
                    state.isDragging = false
                    if (dragDistance < 0f) latestUp() else latestDown()
                } else {
                    settleBack()
                }
            }
        )
    }
}
