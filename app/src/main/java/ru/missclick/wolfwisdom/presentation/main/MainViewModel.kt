package ru.missclick.wolfwisdom.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.presentation.components.QuoteCardTransition
import ru.missclick.wolfwisdom.presentation.components.QuoteFlipDirection

class MainViewModel(private val quoteRepository: QuoteRepository) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()
    private val effectChannel = Channel<MainEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var quoteGeneration = 0L

    init {
        viewModelScope.launch {
            quoteRepository.observeCurrentQuote().collect { quote ->
                if (_state.value.isNavigating || _state.value.isSavingQuote ||
                    _state.value.quoteTransition != null) return@collect
                val generation = quoteGeneration
                val canGoBack = quoteRepository.hasPreviousQuote()
                _state.update {
                    // A delayed observation must not replace a newer navigation or save result.
                    if (generation != quoteGeneration || it.isNavigating || it.isSavingQuote ||
                        it.quoteTransition != null) {
                        it
                    } else {
                        it.copy(
                            quoteId = quote.id,
                            quote = quote.quote,
                            isQuoteSaved = quote.isSaved,
                            canGoBack = canGoBack
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            // These actions are handled by MainRoot.
            MainAction.NavigateToSaved,
            MainAction.NavigateToAchievements,
            MainAction.SupportClick,
            MainAction.NavigateToContacts,
            MainAction.NavigateToAbout,
            MainAction.ShareClick,
            MainAction.WidgetClick -> Unit
            MainAction.BackPressed -> viewModelScope.launch {
                effectChannel.send(MainEffect.ShowCloseSnackbar)
            }
            MainAction.ToggleOptions -> _state.update {
                it.copy(isOptionsVisible = !it.isOptionsVisible)
            }
            MainAction.DismissOptions -> _state.update {
                it.copy(isOptionsVisible = false)
            }
            MainAction.ToggleQuoteSaved -> toggleQuoteSaved()
            MainAction.SwipeUp -> navigate(QuoteFlipDirection.Up)
            MainAction.SwipeDown -> navigate(QuoteFlipDirection.Down)
            is MainAction.QuoteAnimationFinished -> _state.update {
                if (it.quoteTransition?.id == action.transitionId) it.copy(quoteTransition = null) else it
            }
        }
    }

    private fun toggleQuoteSaved() {
        val current = _state.value
        val quoteId = current.quoteId ?: return
        if (current.isSavingQuote || current.isNavigating || current.quoteTransition != null) return

        val isSaved = !current.isQuoteSaved
        ++quoteGeneration
        _state.update { it.copy(isSavingQuote = true) }
        viewModelScope.launch {
            try {
                quoteRepository.setSaved(quoteId, isSaved)
                _state.update {
                    if (it.quoteId == quoteId) it.copy(isQuoteSaved = isSaved) else it
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("MainViewModel", "Could not update the quote's saved state", error)
            } finally {
                _state.update { it.copy(isSavingQuote = false) }
            }
        }
    }

    private fun navigate(direction: QuoteFlipDirection) {
        val current = _state.value
        if (current.quote.isEmpty() || current.isNavigating || current.isSavingQuote ||
            current.quoteTransition != null) return

        val transition = QuoteCardTransition(
            id = ++quoteGeneration,
            direction = direction,
            outgoingQuote = current.quote,
            outgoingIsSaved = current.isQuoteSaved,
            isBoundary = direction == QuoteFlipDirection.Down && !current.canGoBack
        )
        if (transition.isBoundary) {
            _state.update { it.copy(quoteTransition = transition) }
            return
        }

        _state.update { it.copy(isNavigating = true) }
        viewModelScope.launch {
            try {
                val quote = when (direction) {
                    QuoteFlipDirection.Up -> quoteRepository.nextQuote()
                    QuoteFlipDirection.Down -> quoteRepository.previousQuote()
                }
                val canGoBack = quoteRepository.hasPreviousQuote()
                _state.update {
                    it.copy(
                        quoteId = quote.id,
                        quote = quote.quote,
                        isQuoteSaved = quote.isSaved,
                        canGoBack = canGoBack,
                        isNavigating = false,
                        quoteTransition = transition
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("MainViewModel", "Could not navigate to a quote", error)
                _state.update {
                    it.copy(isNavigating = false, quoteTransition = transition.copy(isBoundary = true))
                }
            }
        }
    }

}
