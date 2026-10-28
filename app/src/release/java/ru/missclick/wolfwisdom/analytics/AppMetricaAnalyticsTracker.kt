package ru.missclick.wolfwisdom.analytics

import android.app.Application
import ru.missclick.wolfwisdom.logging.AppLog
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import ru.missclick.wolfwisdom.BuildConfig

class AppMetricaAnalyticsTracker : AnalyticsTracker {
    override fun track(name: String, parameters: Map<String, Any>) {
        // Analytics must never interrupt a user action.
        try {
            AppMetrica.reportEvent(name, parameters)
        } catch (error: RuntimeException) {
            AppLog.w("AppAnalytics", "Unable to queue event: $name", error)
        }
    }

    companion object {
        fun initialize(application: Application) {
            val config = AppMetricaConfig.newConfigBuilder(BuildConfig.METRIC_KEY)
                .withLocationTracking(false)
                .withAdvIdentifiersTracking(false)
                .withRevenueAutoTrackingEnabled(false)
                .withDataSendingEnabled(true)
                .withSessionsAutoTrackingEnabled(false)
                .withAppOpenTrackingEnabled(false)
                .withCrashReporting(false)
                .withNativeCrashReporting(false)
                .withAnrMonitoring(false)
                .build()
            AppMetrica.activate(application, config)
        }
    }
}
