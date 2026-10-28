package ru.missclick.wolfwisdom.analytics

import android.app.Application
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import ru.missclick.wolfwisdom.BuildConfig

class AppMetricaAnalyticsTracker : AnalyticsTracker {
    override fun track(name: String, parameters: Map<String, Any>) {
        AnalyticsRuntime.reportEvent(name, parameters)
    }

    companion object {
        fun initialize(application: Application) {
            val config = AppMetricaConfig.newConfigBuilder(BuildConfig.METRIC_KEY)
                .withLocationTracking(false)
                .withAdvIdentifiersTracking(false)
                .withRevenueAutoTrackingEnabled(false)
                .withDataSendingEnabled(false)
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
