package ru.missclick.wolfwisdom.presentation.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository

class SavedViewModel(private val quoteRepository: QuoteRepository) : ViewModel() {
    private val _state = MutableStateFlow(SavedState())
    val state = _state.asStateFlow()
    private val effectChannel = Channel<SavedEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            quoteRepository.observeSavedQuotes()
                .catch {
                    _state.update { it.copy(isLoading = false) }
                    effectChannel.send(SavedEffect.ShowError(R.string.saved_load_error))
                }
                .collect { quotes ->
                    _state.update {
                        it.copy(
                            quotes = quotes,
                            isLoading = false,
                            isSearchVisible = it.isSearchVisible && quotes.isNotEmpty(),
                            searchQuery = if (quotes.isEmpty()) "" else it.searchQuery
                        )
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
                quoteRepository.setSaved(quoteId, false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                effectChannel.send(SavedEffect.ShowError(R.string.saved_remove_error))
            } finally {
                _state.update { it.copy(removingQuoteIds = it.removingQuoteIds - quoteId) }
            }
        }
    }
}
