package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration

@Composable
fun WolfWisdomScreen(content: @Composable () -> Unit) {
    val background = when (currentDeviceConfiguration()) {
        DeviceConfiguration.MOBILE_PORTRAIT -> R.drawable.moonlit_wolf_background
        DeviceConfiguration.MOBILE_LANDSCAPE -> R.drawable.moonlit_wolf_phone_landscape_background
        DeviceConfiguration.TABLET_PORTRAIT -> R.drawable.moonlit_wolf_tablet_portrait_background
        DeviceConfiguration.TABLET_LANDSCAPE -> R.drawable.moonlit_wolf_landscape_background
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}
