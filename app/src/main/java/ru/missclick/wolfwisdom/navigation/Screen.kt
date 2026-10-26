package ru.missclick.wolfwisdom.navigation

import kotlinx.serialization.Serializable
import androidx.annotation.Keep

@Keep
enum class LoadErrorSource { MAIN, SAVED }

sealed interface Screen {
    @Serializable
    data object Main : Screen

    @Serializable
    data object Saved : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Contacts : Screen

    @Serializable
    data class Error(val source: LoadErrorSource) : Screen
}
