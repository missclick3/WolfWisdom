package ru.missclick.wolfwisdom.presentation.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ErrorViewModel : ViewModel() {
    private val _state = MutableStateFlow(ErrorState())
    val state = _state.asStateFlow()
    private val retryChannel = Channel<Unit>(Channel.BUFFERED)
    val retryRequests = retryChannel.receiveAsFlow()

    fun onAction(action: ErrorAction) {
        when (action) {
            ErrorAction.Retry -> {
                if (_state.value.isRetrying) return
                _state.update { it.copy(isRetrying = true) }
                viewModelScope.launch { retryChannel.send(Unit) }
            }
        }
    }

    fun onRetryFinished() {
        _state.update { it.copy(isRetrying = false) }
    }
}
