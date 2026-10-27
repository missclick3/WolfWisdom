package ru.missclick.wolfwisdom.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.missclick.wolfwisdom.R

enum class AppLanguage(
    val tag: String,
    @StringRes val label: Int,
    @DrawableRes val flag: Int
) {
    ENGLISH("en", R.string.language_english, R.drawable.ic_flag_english),
    RUSSIAN("ru", R.string.language_russian, R.drawable.ic_flag_russian);

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
