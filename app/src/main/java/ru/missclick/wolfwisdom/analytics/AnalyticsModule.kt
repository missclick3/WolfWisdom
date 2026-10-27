package ru.missclick.wolfwisdom.analytics

import org.koin.dsl.module

val analyticsModule = module {
    single<AnalyticsTracker> { AppMetricaAnalyticsTracker() }
}
