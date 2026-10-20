package ru.missclick.wolfwisdom.presentation.saved

import androidx.annotation.StringRes

sealed interface SavedEffect {
    data class ShowError(@StringRes val messageRes: Int) : SavedEffect
}
