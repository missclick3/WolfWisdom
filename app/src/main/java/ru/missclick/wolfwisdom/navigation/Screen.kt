package ru.missclick.wolfwisdom.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Main : Screen
}
