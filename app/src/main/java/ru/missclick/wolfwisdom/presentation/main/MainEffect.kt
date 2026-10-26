package ru.missclick.wolfwisdom.presentation.main

import androidx.annotation.StringRes

sealed interface MainEffect {
    data object ShowCloseSnackbar : MainEffect
    data class ShowError(@StringRes val messageRes: Int) : MainEffect
}
