package ru.missclick.wolfwisdom.analytics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.appmetrica.analytics.AppMetrica
import java.lang.ref.WeakReference

object AnalyticsRuntime {
    @Volatile private var enabled = false
    private var initialized = false
    private var lifecycleRegistered = false
    private var resumedActivity = WeakReference<Activity>(null)

    /** No SDK calls before the first grant, including widget-only process starts. */
    @Synchronized
    fun setEnabled(application: Application, value: Boolean) {
        if (!lifecycleRegistered) {
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    resumedActivity = WeakReference(activity)
                    if (enabled) AppMetrica.resumeSession(activity)
                }
                override fun onActivityPaused(activity: Activity) {
                    if (enabled) AppMetrica.pauseSession(activity)
                    if (resumedActivity.get() === activity) resumedActivity.clear()
                }
                override fun onActivityDestroyed(activity: Activity) {
                    if (resumedActivity.get() === activity) resumedActivity.clear()
                }
                override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            })
            lifecycleRegistered = true
        }
        if (!value) {
            enabled = false
            if (initialized) {
                AppMetrica.setDataSendingEnabled(false)
                resumedActivity.get()?.let(AppMetrica::pauseSession)
            }
            return
        }
        if (!initialized) {
            AppMetricaAnalyticsTracker.initialize(application)
            // Automatic crash reporting cannot be disabled at runtime. Forward
            // crashes ourselves so withdrawing consent also stops collection.
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                try {
                    if (enabled) AppMetrica.reportUnhandledException(error)
                } finally {
                    previous?.uncaughtException(thread, error)
                }
            }
            initialized = true
        }
        AppMetrica.setDataSendingEnabled(true)
        enabled = true
        resumedActivity.get()?.let(AppMetrica::resumeSession)
    }

    fun createTracker(): AnalyticsTracker = AppMetricaAnalyticsTracker()
}
