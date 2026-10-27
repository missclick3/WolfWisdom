package ru.missclick.wolfwisdom.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import org.koin.compose.koinInject

// Previews and isolated UI tests do not send custom screen events.
val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> { NoOpAnalyticsTracker }

@Composable
fun ProvideAnalytics(content: @Composable () -> Unit) {
    val tracker = koinInject<AnalyticsTracker>()
    CompositionLocalProvider(LocalAnalyticsTracker provides tracker, content = content)
}

@Composable
fun <T : ScreenAnalytics> rememberScreenAnalytics(factory: (AnalyticsTracker) -> T): T {
    val tracker = LocalAnalyticsTracker.current
    return remember(tracker) { factory(tracker) }
}
