package ru.missclick.wolfwisdom

import java.time.LocalDate

enum class SeasonalIcon(val aliasName: String) {
    DEFAULT("DefaultLauncher"),
    SPRING("SpringLauncher"),
    SUMMER("SummerLauncher"),
    AUTUMN("AutumnLauncher"),
    WINTER("WinterLauncher"),
    NEW_YEAR("NewYearLauncher");

    companion object {
        fun forDate(date: LocalDate): SeasonalIcon = when {
            (date.monthValue == 12 && date.dayOfMonth >= 24) ||
                (date.monthValue == 1 && date.dayOfMonth <= 8) -> NEW_YEAR
            date.monthValue in 3..5 -> SPRING
            date.monthValue in 6..8 -> SUMMER
            date.monthValue in 9..11 -> AUTUMN
            else -> WINTER
        }
    }
}
