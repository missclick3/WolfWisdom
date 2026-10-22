package ru.missclick.wolfwisdom

import android.os.Bundle
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.navigation.WolfNavHost
import ru.missclick.wolfwisdom.ui.theme.WolfPalette
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        SeasonalIconManager.update(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            WolfWisdomTheme {
                WolfWisdomScreen {
                    WolfNavHost()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hello $name!",
            style = WolfTypography.Title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = WolfPalette.Ice300
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WolfWisdomTheme {
        WolfWisdomScreen {}
    }
}
