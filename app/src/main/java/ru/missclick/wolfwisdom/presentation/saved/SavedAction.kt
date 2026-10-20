package ru.missclick.wolfwisdom.presentation.saved

sealed interface SavedAction {
    data object OpenSearch : SavedAction
    data object CloseSearch : SavedAction
    data class SearchQueryChanged(val query: String) : SavedAction
    data class RemoveQuote(val quoteId: Long) : SavedAction
}
