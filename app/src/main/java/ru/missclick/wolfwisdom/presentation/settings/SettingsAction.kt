package ru.missclick.wolfwisdom.presentation.settings

sealed interface SettingsAction {
    data object BackPressed : SettingsAction
    data class LanguageSelected(val language: AppLanguage) : SettingsAction
    data class SoundEffectsChanged(val enabled: Boolean) : SettingsAction
    data class VibrationChanged(val enabled: Boolean) : SettingsAction
    data class AnalyticsChanged(val enabled: Boolean) : SettingsAction
}
