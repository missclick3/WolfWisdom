package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import ru.missclick.wolfwisdom.R
import kotlin.math.abs
import kotlin.math.roundToInt

enum class QuoteFlipDirection(val rotationSign: Float) {
    Up(1f),
    Down(-1f)
}

data class QuoteCardTransition(
    val id: Long,
    val direction: QuoteFlipDirection,
    val outgoingQuote: String,
    val outgoingIsSaved: Boolean,
    val isBoundary: Boolean = false
)

/** Makes one continuous 180-degree turn, revealing the next quote on the other face. */
@Composable
fun FlippingQuoteCard(
    quote: String,
    isSaved: Boolean,
    swipeState: QuoteSwipeState,
    transition: QuoteCardTransition?,
    enabled: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onAnimationFinished: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showIncoming by remember(transition?.id) {
        derivedStateOf { abs(swipeState.rotation.value) >= 90f }
    }
    val latestSwipeUp by rememberUpdatedState(onSwipeUp)
    val latestSwipeDown by rememberUpdatedState(onSwipeDown)
    val latestFinished by rememberUpdatedState(onAnimationFinished)
    val canInteract = enabled && transition == null && !swipeState.isDragging &&
        !swipeState.isAwaitingNavigation && !swipeState.isSettling
    val nextLabel = stringResource(R.string.next_quote)
    val previousLabel = stringResource(R.string.previous_quote)

    LaunchedEffect(transition?.id, enabled) {
        val flip = transition
        if (flip == null) {
            if (enabled && swipeState.isAwaitingNavigation) {
                // A failed/cancelled navigation must not leave the card held in mid-air.
                swipeState.returnToRest()
            } else if (!swipeState.isDragging && !swipeState.isAwaitingNavigation) {
                swipeState.rotation.snapTo(0f)
            }
            return@LaunchedEffect
        }
        val fromDrag = swipeState.isAwaitingNavigation
        swipeState.rotation.snapTo(swipeState.angle)
        swipeState.isDragging = false
        swipeState.isAwaitingNavigation = false
        val sign = flip.direction.rotationSign
        if (flip.isBoundary) {
            if (!fromDrag) {
                swipeState.rotation.animateTo(sign * 12f, tween(140, easing = FastOutSlowInEasing))
            }
            swipeState.returnToRest()
        } else {
            val remaining = (180f - abs(swipeState.rotation.value)).coerceAtLeast(0f)
            val duration = (650f * remaining / 180f).roundToInt().coerceIn(400, 650)
            swipeState.rotation.animateTo(sign * 180f, tween(duration, easing = FastOutSlowInEasing))
        }
        latestFinished(flip.id)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                customActions = if (canInteract) listOf(
                    CustomAccessibilityAction(nextLabel) { latestSwipeUp(); true },
                    CustomAccessibilityAction(previousLabel) { latestSwipeDown(); true }
                ) else emptyList()
            }
            .graphicsLayer {
                rotationX = swipeState.angle
                // Keep enough perspective to distinguish lifting the bottom from the top.
                cameraDistance = 16f * density
            }
    ) {
        val showOutgoing = transition != null && !showIncoming
        QuoteComponent(
            modifier = Modifier.graphicsLayer {
                // Counter-rotate the other face so the new quote reads upright.
                rotationX = when {
                    swipeState.angle > 90f -> -180f
                    swipeState.angle < -90f -> 180f
                    else -> 0f
                }
            },
            quote = if (showOutgoing) transition.outgoingQuote else quote,
            isSaved = if (showOutgoing) transition.outgoingIsSaved else isSaved,
            onSaveClick = { if (canInteract) onSaveClick() },
            onShareClick = { if (canInteract) onShareClick() },
            onWidgetClick = { if (canInteract) onWidgetClick() }
        )
    }
}
