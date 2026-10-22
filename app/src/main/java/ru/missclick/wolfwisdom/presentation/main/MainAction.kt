package ru.missclick.wolfwisdom.presentation.main

sealed interface MainAction {
    data object BackPressed : MainAction
    data object NavigateToSaved : MainAction
    data object NavigateToAchievements : MainAction
    data object SupportClick : MainAction
    data object NavigateToContacts : MainAction
    data object NavigateToAbout : MainAction
    data object ShareClick : MainAction
    data object WidgetClick : MainAction
    data object ToggleOptions : MainAction
    data object DismissOptions : MainAction
    data object ToggleQuoteSaved : MainAction
    data object SwipeUp : MainAction
    data object SwipeDown : MainAction
    data class QuoteAnimationFinished(val transitionId: Long) : MainAction
}
