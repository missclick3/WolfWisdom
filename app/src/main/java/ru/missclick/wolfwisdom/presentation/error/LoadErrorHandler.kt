package ru.missclick.wolfwisdom.presentation.error

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import ru.missclick.wolfwisdom.domain.util.Error

data class LoadErrorState(
    val error: Error? = null,
    val isRetrying: Boolean = false
)

/** Owned by the loading ViewModel; retry events stay out of navigation arguments. */
class LoadErrorHandler<Event : Any> {
    private val _state = MutableStateFlow(LoadErrorState())
    val state = _state.asStateFlow()
    private var retryEvent: Event? = null
    private var retryAttempt = 0L
    private val retryChannel = Channel<Event>(Channel.CONFLATED)
    val retryEvents = retryChannel.receiveAsFlow()

    fun onFailure(error: Error, retryEvent: Event) {
        this.retryEvent = retryEvent
        _state.value = LoadErrorState(error = error)
    }

    fun onSuccess() {
        retryEvent = null
        _state.value = LoadErrorState()
    }

    /** Emits the failed operation's update event and waits until that attempt has a result. */
    suspend fun retry() {
        val event = retryEvent ?: return
        val current = _state.value
        if (current.isRetrying || !_state.compareAndSet(current, current.copy(isRetrying = true))) return
        val attempt = ++retryAttempt
        try {
            retryChannel.send(event)
            state.first { !it.isRetrying }
        } finally {
            if (attempt == retryAttempt) _state.update { it.copy(isRetrying = false) }
        }
    }
}
