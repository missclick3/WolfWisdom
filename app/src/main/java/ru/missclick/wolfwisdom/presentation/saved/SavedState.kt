package ru.missclick.wolfwisdom.presentation.saved

data class SavedState(
    val paramOne: String = "default",
    val paramTwo: List<String> = emptyList(),
)