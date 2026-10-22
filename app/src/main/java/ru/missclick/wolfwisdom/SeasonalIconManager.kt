package ru.missclick.wolfwisdom

import android.app.AlarmManager
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.time.LocalDate
import java.time.ZoneId

object SeasonalIconManager {
    private var startedActivities = 0
    private var waitingForBackground = false

    fun initialize(application: Application) {
        val handler = Handler(Looper.getMainLooper())
        val updateInBackground = Runnable {
            if (startedActivities == 0) {
                waitingForBackground = false
                update(application)
            }
        }
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                waitingForBackground = true
                handler.removeCallbacks(updateInBackground)
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities == 0) {
                    // Allow activity recreation and transitions to finish before changing aliases.
                    handler.postDelayed(updateInBackground, 1_000L)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun update(context: Context) {
        val today = LocalDate.now()
        // Disabling the alias used to launch a visible task can close it, even with
        // DONT_KILL_APP. Receivers must respect the same foreground guard as onResume.
        if (startedActivities == 0 && !waitingForBackground) {
            updateIcon(context, SeasonalIcon.forDate(today))
        }

        // Inexact local-midnight check: no special exact-alarm permission required.
        // Recalculate each time to respect daylight saving and time-zone changes.
        val nextMidnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, SeasonalIconReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC, nextMidnight, pendingIntent
        )
    }

    private fun updateIcon(context: Context, selected: SeasonalIcon) {
        val manager = context.packageManager
        // Enable the new alias first so there is always a launchable entry on older Android.
        val changes = SeasonalIcon.entries.sortedBy { it != selected }.mapNotNull { icon ->
            val component = ComponentName(context.packageName, "${context.packageName}.${icon.aliasName}")
            val enabled = icon == selected
            val current = manager.getComponentEnabledSetting(component)
            val currentlyEnabled = current == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (current == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == SeasonalIcon.DEFAULT)
            if (enabled == currentlyEnabled) null else component to if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        }
        if (changes.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.setComponentEnabledSettings(changes.map { (component, state) ->
                    PackageManager.ComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                })
            } else {
                changes.forEach { (component, state) ->
                    manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}
