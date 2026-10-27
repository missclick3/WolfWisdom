package ru.missclick.wolfwisdom.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.domain.util.Result
import ru.missclick.wolfwisdom.domain.util.onFailure
import ru.missclick.wolfwisdom.domain.util.onSuccess
import ru.missclick.wolfwisdom.presentation.components.QuoteCardTransition
import ru.missclick.wolfwisdom.presentation.components.QuoteFlipDirection
import ru.missclick.wolfwisdom.presentation.error.LoadErrorHandler

class MainViewModel(private val quoteRepository: QuoteRepository) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()
    private val effectChannel = Channel<MainEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var quoteGeneration = 0L
    val errorHandler = LoadErrorHandler<MainAction>()
    private var observationJob: Job? = null

    init {
        viewModelScope.launch { errorHandler.retryEvents.collect(::onAction) }
        observeQuotes()
    }

    private fun observeQuotes() {
        observationJob?.cancel()
        _state.update { it.copy(isLoading = true, hasLoadError = false) }
        observationJob = viewModelScope.launch {
            quoteRepository.observeCurrentQuote().collect { result ->
                val quote = when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> {
                        _state.update { it.copy(isLoading = false, hasLoadError = true) }
                        errorHandler.onFailure(result.error, MainAction.Reload)
                        return@collect
                    }
                }
                _state.update {
                    if (it.quoteId == quote.id) it.copy(isQuoteOnWidget = quote.isOnWidget) else it
                }
                if (_state.value.isNavigating || _state.value.isSavingQuote ||
                    _state.value.quoteTransition != null) return@collect
                val generation = quoteGeneration
                val canGoBack = canGoBack() ?: return@collect
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
                            isQuoteOnWidget = quote.isOnWidget,
                            canGoBack = canGoBack,
                            isLoading = false,
                            hasLoadError = false
                        )
                    }
                }
                val current = _state.value
                if (generation == quoteGeneration && !current.isNavigating &&
                    !current.isSavingQuote && current.quoteTransition == null) {
                    errorHandler.onSuccess()
                }
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            MainAction.Reload -> {
                ++quoteGeneration
                _state.update { it.copy(quoteTransition = null) }
                observeQuotes()
            }
            // These actions are handled by MainRoot.
            MainAction.NavigateToSaved,
            MainAction.NavigateToAchievements,
            MainAction.SupportClick,
            MainAction.NavigateToContacts,
            MainAction.NavigateToAbout,
            MainAction.NavigateToSettings,
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
        if (current.showLoading || current.isSavingQuote || current.isNavigating || current.quoteTransition != null) return

        val isSaved = !current.isQuoteSaved
        ++quoteGeneration
        _state.update { it.copy(isSavingQuote = true) }
        viewModelScope.launch {
            try {
                quoteRepository.setSaved(quoteId, isSaved)
                    .onSuccess {
                        _state.update {
                            if (it.quoteId == quoteId) it.copy(isQuoteSaved = isSaved) else it
                        }
                    }
                    .onFailure {
                        effectChannel.send(MainEffect.ShowError(
                            if (isSaved) R.string.saved_add_error else R.string.saved_remove_error
                        ))
                    }
            } finally {
                _state.update { it.copy(isSavingQuote = false) }
            }
        }
    }

    private fun navigate(direction: QuoteFlipDirection) {
        val current = _state.value
        if (current.isLoading || current.quote.isEmpty() || current.isNavigating || current.isSavingQuote ||
            current.quoteTransition != null) return

        val transition = QuoteCardTransition(
            id = ++quoteGeneration,
            direction = direction,
            outgoingQuote = current.quote,
            outgoingIsSaved = current.isQuoteSaved,
            outgoingIsOnWidget = current.isQuoteOnWidget,
            isBoundary = direction == QuoteFlipDirection.Down && !current.canGoBack
        )
        if (transition.isBoundary) {
            _state.update { it.copy(quoteTransition = transition) }
            return
        }

        _state.update { it.copy(isNavigating = true) }
        viewModelScope.launch {
            try {
                val result = when (direction) {
                    QuoteFlipDirection.Up -> quoteRepository.nextQuote()
                    QuoteFlipDirection.Down -> quoteRepository.previousQuote()
                }
                result.onSuccess { quote ->
                    val canGoBack = canGoBack() ?: return@onSuccess
                    _state.update {
                        it.copy(
                            quoteId = quote.id,
                            quote = quote.quote,
                            isQuoteSaved = quote.isSaved,
                            isQuoteOnWidget = quote.isOnWidget,
                            canGoBack = canGoBack,
                            isNavigating = false,
                            isLoading = false,
                            hasLoadError = false,
                            quoteTransition = transition
                        )
                    }
                    errorHandler.onSuccess()
                }.onFailure { error ->
                    _state.update { it.copy(isLoading = false, hasLoadError = true) }
                    errorHandler.onFailure(
                        error,
                        if (direction == QuoteFlipDirection.Up) MainAction.SwipeUp else MainAction.SwipeDown
                    )
                }
            } finally {
                _state.update { it.copy(isNavigating = false) }
            }
        }
    }

    private suspend fun canGoBack(): Boolean? = when (val result = quoteRepository.hasPreviousQuote()) {
        is Result.Success -> result.data
        is Result.Failure -> {
            _state.update { it.copy(isLoading = false, hasLoadError = true) }
            errorHandler.onFailure(result.error, MainAction.Reload)
            null
        }
    }
}
