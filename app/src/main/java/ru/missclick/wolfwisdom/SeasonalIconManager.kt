package ru.missclick.wolfwisdom

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

object SeasonalIconManager {
    fun update(context: Context) {
        val today = LocalDate.now()
        val selected = SeasonalIcon.forDate(today)
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
}
