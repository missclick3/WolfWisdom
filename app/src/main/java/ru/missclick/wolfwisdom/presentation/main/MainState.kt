package ru.missclick.wolfwisdom.presentation.main

data class MainState(
    val isOptionsVisible: Boolean = false,
    val quote: String = "",
    val isQuoteSaved: Boolean = false,
    val paramOne: String = "default",
    val paramTwo: List<String> = emptyList(),
)
