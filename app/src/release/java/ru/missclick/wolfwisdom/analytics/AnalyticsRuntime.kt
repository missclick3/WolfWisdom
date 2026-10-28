package ru.missclick.wolfwisdom.analytics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.appmetrica.analytics.AppMetrica
import java.lang.ref.WeakReference
import ru.missclick.wolfwisdom.logging.AppLog

object AnalyticsRuntime {
    @Volatile private var collection: AnalyticsCollectionController? = null
    @Volatile private var resumedActivity = WeakReference<Activity>(null)

    /** No SDK calls before the first grant, including widget-only process starts. */
    @Synchronized
    fun setEnabled(application: Application, value: Boolean) {
        val controller = collection ?: AnalyticsCollectionController(
            prepare = {
                application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityResumed(activity: Activity) {
                        resumedActivity = WeakReference(activity)
                        collection?.onResume()
                    }
                    override fun onActivityPaused(activity: Activity) {
                        try {
                            collection?.onPause()
                        } finally {
                            if (resumedActivity.get() === activity) resumedActivity.clear()
                        }
                    }
                    override fun onActivityDestroyed(activity: Activity) {
                        if (resumedActivity.get() === activity) resumedActivity.clear()
                    }
                    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
                    override fun onActivityStarted(activity: Activity) = Unit
                    override fun onActivityStopped(activity: Activity) = Unit
                    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
                })
            },
            initialize = {
                AppMetricaAnalyticsTracker.initialize(application)
                // Always preserve Android's crash handler, even if reporting fails.
                val previous = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                    try {
                        collection?.runWhenEnabled { AppMetrica.reportUnhandledException(error) }
                    } finally {
                        previous?.uncaughtException(thread, error)
                    }
                }
            },
            setDataSendingEnabled = AppMetrica::setDataSendingEnabled,
            resumeSession = { resumedActivity.get()?.let(AppMetrica::resumeSession) },
            pauseSession = { resumedActivity.get()?.let(AppMetrica::pauseSession) },
            onFailure = { AppLog.w("AppAnalytics", "Analytics disabled after SDK failure", it) }
        ).also { collection = it }
        controller.setEnabled(value)
    }

    internal fun reportEvent(name: String, parameters: Map<String, Any>) {
        collection?.runWhenEnabled { AppMetrica.reportEvent(name, parameters) }
    }

    fun createTracker(): AnalyticsTracker = AppMetricaAnalyticsTracker()
}
