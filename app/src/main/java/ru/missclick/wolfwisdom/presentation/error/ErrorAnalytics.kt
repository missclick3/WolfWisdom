package ru.missclick.wolfwisdom.presentation.error

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics
import ru.missclick.wolfwisdom.navigation.LoadErrorSource

class ErrorAnalytics(tracker: AnalyticsTracker, source: LoadErrorSource) : BaseScreenAnalytics(tracker, "error") {
    override val context: Map<String, Any> = mapOf("error_source" to source.name.lowercase(java.util.Locale.ROOT))
    fun retryClicked() = event("error_retry_clicked")
}
