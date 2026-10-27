package ru.missclick.wolfwisdom.analytics

import android.app.Application
import ru.missclick.wolfwisdom.logging.AppLog

/** The debug APK has no AppMetrica dependency, services or startup code. */
object AnalyticsRuntime {
    fun initialize(@Suppress("UNUSED_PARAMETER") application: Application) = Unit

    fun createTracker(): AnalyticsTracker = LoggingAnalyticsTracker()
}

class LoggingAnalyticsTracker : AnalyticsTracker {
    override fun track(name: String, parameters: Map<String, Any>) {
        AppLog.d("AppAnalytics", "$name $parameters")
    }
}
