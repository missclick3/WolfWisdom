package ru.missclick.wolfwisdom.presentation.settings

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics

class SettingsAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "settings") {
    fun onAction(action: SettingsAction, previousLanguage: String) {
        when (action) {
            is SettingsAction.SoundEffectsChanged -> event("sound_toggle_clicked", "enabled" to action.enabled)
            is SettingsAction.VibrationChanged -> event("vibration_toggle_clicked", "enabled" to action.enabled)
            is SettingsAction.LanguageSelected -> event("language_option_clicked",
                "previous_language" to previousLanguage, "language" to action.language.tag)
            SettingsAction.BackPressed -> Unit
        }
    }
}
