package ru.missclick.wolfwisdom.widget

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics

class WidgetConfigurationAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "widget_configuration") {
    fun createClicked() = event("widget_create_clicked", "surface" to "configuration")
    fun retryClicked() = event("widget_retry_clicked", "surface" to "configuration")
    fun themeClicked(theme: WidgetTheme) = event("widget_theme_clicked", "theme" to theme.name.lowercase(java.util.Locale.ROOT))
}

class WidgetPlacementAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "widget_placement") {
    fun placementOpened(sourceScreen: String) = event("widget_placement_opened", "source_screen" to sourceScreen)
    fun createClicked() = event("widget_create_clicked", "surface" to "placement")
    fun retryClicked() = event("widget_retry_clicked", "surface" to "placement")
    fun targetClicked() = event("widget_target_clicked")
    fun replaceClicked() = event("widget_replace_clicked")
    fun closeClicked() = event("widget_close_clicked", "surface" to "placement")
}
