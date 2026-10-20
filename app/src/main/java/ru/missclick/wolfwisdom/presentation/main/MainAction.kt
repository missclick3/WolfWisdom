package ru.missclick.wolfwisdom.presentation.main

sealed interface MainAction {
    data object ToggleOptions : MainAction
    data object DismissOptions : MainAction
}
