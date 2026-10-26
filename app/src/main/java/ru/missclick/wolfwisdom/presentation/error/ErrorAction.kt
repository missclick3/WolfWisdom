package ru.missclick.wolfwisdom.presentation.error

sealed interface ErrorAction {
    data object Retry : ErrorAction
}
