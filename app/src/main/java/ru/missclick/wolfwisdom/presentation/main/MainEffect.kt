package ru.missclick.wolfwisdom.presentation.main

sealed interface MainEffect {
    data object ShowCloseSnackbar : MainEffect
}
