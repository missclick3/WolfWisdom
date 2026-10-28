package ru.missclick.wolfwisdom

import ru.missclick.wolfwisdom.analytics.ProvideAnalytics
import ru.missclick.wolfwisdom.presentation.consent.AnalyticsConsentHost

import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import ru.missclick.wolfwisdom.navigation.WolfNavHost
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

class MainActivity : AppCompatActivity() {
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
                    ProvideAnalytics { WolfNavHost() }
                    AnalyticsConsentHost()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WolfWisdomTheme {
        WolfWisdomScreen {}
    }
}
