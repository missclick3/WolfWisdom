package ru.missclick.wolfwisdom.presentation.error

data class ErrorState(
    val paramOne: String = "default",
    val paramTwo: List<String> = emptyList(),
)