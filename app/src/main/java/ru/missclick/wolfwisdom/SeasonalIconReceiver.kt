package ru.missclick.wolfwisdom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SeasonalIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SeasonalIconManager.update(context)
    }
}
