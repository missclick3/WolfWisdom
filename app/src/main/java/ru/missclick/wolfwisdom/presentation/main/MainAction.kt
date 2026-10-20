package ru.missclick.wolfwisdom.presentation.main

sealed interface MainAction {
    data object ToggleOptions : MainAction
    data object DismissOptions : MainAction
    data object ToggleQuoteSaved : MainAction
    data object SwipeUp : MainAction
    data object SwipeDown : MainAction
}
