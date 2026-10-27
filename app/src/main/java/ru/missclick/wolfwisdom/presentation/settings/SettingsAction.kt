package ru.missclick.wolfwisdom.presentation.settings

sealed interface SettingsAction {
    data object BackPressed : SettingsAction
    data class LanguageSelected(val language: AppLanguage) : SettingsAction
}
