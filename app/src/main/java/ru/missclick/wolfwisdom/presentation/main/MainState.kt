package ru.missclick.wolfwisdom.presentation.main

data class MainState(
    val paramOne: String = "default",
    val paramTwo: List<String> = emptyList(),
)