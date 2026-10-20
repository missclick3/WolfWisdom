package ru.missclick.wolfwisdom.presentation.saved

import ru.missclick.wolfwisdom.domain.model.Quote

data class SavedState(
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = true,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val removingQuoteIds: Set<Long> = emptySet()
) {
    val visibleQuotes: List<Quote>
        get() = if (isSearchVisible && searchQuery.isNotBlank()) {
            quotes.filter { it.quote.contains(searchQuery.trim(), ignoreCase = true) }
        } else {
            quotes
        }
}
