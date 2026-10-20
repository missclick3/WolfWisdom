package ru.missclick.wolfwisdom.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository

class MainViewModel(private val quoteRepository: QuoteRepository) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            quoteRepository.observeCurrentQuote().collect { quote ->
                _state.update {
                    it.copy(quote = quote.quote, isQuoteSaved = quote.isSaved)
                }
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            MainAction.ToggleOptions -> _state.update {
                it.copy(isOptionsVisible = !it.isOptionsVisible)
            }
            MainAction.DismissOptions -> _state.update {
                it.copy(isOptionsVisible = false)
            }
            // Saving and gesture handling will be implemented in a later iteration.
            MainAction.ToggleQuoteSaved,
            MainAction.SwipeUp,
            MainAction.SwipeDown -> Unit
        }
    }

}
