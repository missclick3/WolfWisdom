package ru.missclick.wolfwisdom.data.local

import android.content.Context
import android.content.SharedPreferences

data class FeedbackSettings(
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

class FeedbackPreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("feedback_settings", Context.MODE_PRIVATE)

    val settings: FeedbackSettings
        get() = FeedbackSettings(
            soundEffectsEnabled = preferences.getBoolean("sound_effects_enabled", true),
            vibrationEnabled = preferences.getBoolean("vibration_enabled", true)
        )

    fun setSoundEffectsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("sound_effects_enabled", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    /** Returns a function that unregisters the observer. */
    fun observe(onChange: (FeedbackSettings) -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            onChange(settings)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onChange(settings)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
