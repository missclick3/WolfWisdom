package ru.missclick.wolfwisdom.presentation.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.domain.util.onFailure
import ru.missclick.wolfwisdom.domain.util.onSuccess

class SavedViewModel(private val quoteRepository: QuoteRepository) : ViewModel() {
    private val _state = MutableStateFlow(SavedState())
    val state = _state.asStateFlow()
    private val effectChannel = Channel<SavedEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            quoteRepository.observeSavedQuotes().collect { result ->
                result.onSuccess { quotes ->
                    _state.update {
                        it.copy(
                            quotes = quotes,
                            isLoading = false,
                            isSearchVisible = it.isSearchVisible && quotes.isNotEmpty(),
                            searchQuery = if (quotes.isEmpty()) "" else it.searchQuery
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isLoading = false) }
                    effectChannel.send(SavedEffect.ShowError(R.string.saved_load_error))
                }
            }
        }
    }

    fun onAction(action: SavedAction) {
        when (action) {
            SavedAction.OpenSearch -> _state.update {
                it.copy(isSearchVisible = it.quotes.isNotEmpty())
            }
            SavedAction.CloseSearch -> _state.update {
                it.copy(isSearchVisible = false, searchQuery = "")
            }
            is SavedAction.SearchQueryChanged -> _state.update {
                it.copy(searchQuery = action.query)
            }
            is SavedAction.RemoveQuote -> removeQuote(action.quoteId)
        }
    }

    private fun removeQuote(quoteId: Long) {
        val current = _state.value
        if (quoteId in current.removingQuoteIds || current.quotes.none { it.id == quoteId }) return
        _state.update { it.copy(removingQuoteIds = it.removingQuoteIds + quoteId) }
        viewModelScope.launch {
            try {
                quoteRepository.setSaved(quoteId, false).onFailure {
                    effectChannel.send(SavedEffect.ShowError(R.string.saved_remove_error))
                }
            } finally {
                _state.update { it.copy(removingQuoteIds = it.removingQuoteIds - quoteId) }
            }
        }
    }
}
