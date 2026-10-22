package ru.missclick.wolfwisdom.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun currentDeviceConfiguration(): DeviceConfiguration {
    val configuration = LocalConfiguration.current
    return DeviceConfiguration.fromWindowSize(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
}

enum class DeviceConfiguration {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE;

    val isMobile: Boolean
        get() = this == MOBILE_PORTRAIT || this == MOBILE_LANDSCAPE

    val isWideScreen: Boolean
        get() = this == TABLET_LANDSCAPE

    companion object {
        private val TabletMinSize = 600.dp

        /**
         * Chooses a layout for the available window, including multi-window mode.
         * Windows with a shorter side of at least 600 dp use tablet layouts.
         * Square windows use portrait layouts; larger windows also use tablet layouts.
         */
        fun fromWindowSize(width: Dp, height: Dp): DeviceConfiguration {
            val isTablet = minOf(width, height) >= TabletMinSize
            val isLandscape = width > height
            return when {
                isTablet && isLandscape -> TABLET_LANDSCAPE
                isTablet -> TABLET_PORTRAIT
                isLandscape -> MOBILE_LANDSCAPE
                else -> MOBILE_PORTRAIT
            }
        }
    }
}
