package ru.missclick.wolfwisdom.analytics

import android.app.Application

object AnalyticsRuntime {
    fun initialize(application: Application) = AppMetricaAnalyticsTracker.initialize(application)

    fun createTracker(): AnalyticsTracker = AppMetricaAnalyticsTracker()
}
