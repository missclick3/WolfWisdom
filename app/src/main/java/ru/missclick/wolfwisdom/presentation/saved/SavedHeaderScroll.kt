package ru.missclick.wolfwisdom.presentation.saved

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.ui.theme.WolfTypography

internal data class SavedHeaderDimensions(
    val expandedHeight: Dp,
    val collapsedHeight: Dp,
    val collapseDistance: Dp
)

@Composable
internal fun savedHeaderDimensions(): SavedHeaderDimensions {
    val window = LocalConfiguration.current
    val density = LocalDensity.current
    // Keep the existing 48dp control row, allowing room for larger accessibility text.
    val rowHeight = with(density) { WolfTypography.Title.lineHeight.toDp() }.coerceAtLeast(48.dp)
    val shortWindow = window.screenHeightDp < 480
    val collapsed = maxOf(56.dp, rowHeight + 8.dp)
    val expanded = maxOf(collapsed, rowHeight + if (shortWindow) 16.dp else 32.dp)
    val landscape = window.screenWidthDp > window.screenHeightDp
    val distance = if (landscape) {
        (window.screenHeightDp * 0.12f).dp.coerceIn(40.dp, 80.dp)
    } else {
        (window.screenHeightDp * 0.12f).dp.coerceIn(64.dp, 100.dp)
    }
    return SavedHeaderDimensions(expanded, collapsed, distance)
}

/**
 * Only the first row's measured scroll offset matters. Once that row leaves the
 * viewport we are collapsed, regardless of direction, velocity or later row sizes.
 * No accumulated deltas or separate persisted header state can drift from the grid.
 */
@Composable
internal fun rememberSavedHeaderProgress(
    gridState: LazyGridState,
    columns: Int,
    collapseDistance: Dp
): State<Float> {
    val distancePx = with(LocalDensity.current) { collapseDistance.toPx() }
    return remember(gridState, columns, distancePx) {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex >= columns) 1f
            else (gridState.firstVisibleItemScrollOffset / distancePx).coerceIn(0f, 1f)
        }
    }
}
