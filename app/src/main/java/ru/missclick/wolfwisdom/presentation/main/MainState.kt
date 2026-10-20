package ru.missclick.wolfwisdom.presentation.main

import ru.missclick.wolfwisdom.presentation.components.QuoteCardTransition

data class MainState(
    val isOptionsVisible: Boolean = false,
    val quote: String = "",
    val quoteId: Long? = null,
    val isQuoteSaved: Boolean = false,
    val isSavingQuote: Boolean = false,
    val canGoBack: Boolean = false,
    val isNavigating: Boolean = false,
    val quoteTransition: QuoteCardTransition? = null,
    val paramOne: String = "default",
    val paramTwo: List<String> = emptyList(),
)
