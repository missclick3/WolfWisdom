package ru.missclick.wolfwisdom.analytics

import android.app.Application
import android.content.Context

/** Process-wide choice shared by the main screen, settings and widget entry points. */
object AnalyticsConsent {
    lateinit var controller: AnalyticsConsentController
        private set

    fun initialize(application: Application) {
        val preferences = application.getSharedPreferences("analytics_consent", Context.MODE_PRIVATE)
        val initial = when (preferences.getString("choice", null)) {
            "granted" -> AnalyticsConsentChoice.GRANTED
            "declined" -> AnalyticsConsentChoice.DECLINED
            else -> AnalyticsConsentChoice.UNKNOWN
        }
        controller = AnalyticsConsentController(
            initialChoice = initial,
            saveChoice = { choice ->
                preferences.edit().putString("choice", if (choice == AnalyticsConsentChoice.GRANTED) "granted" else "declined").apply()
            },
            setCollectionEnabled = { AnalyticsRuntime.setEnabled(application, it) },
            tracker = AnalyticsRuntime.createTracker()
        )
    }
}
