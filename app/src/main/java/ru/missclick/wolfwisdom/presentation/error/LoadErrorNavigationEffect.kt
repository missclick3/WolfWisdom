package ru.missclick.wolfwisdom.presentation.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Only the foreground destination can open an error screen. */
@Composable
fun LoadErrorNavigationEffect(handler: LoadErrorHandler<*>, onLoadError: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigate by rememberUpdatedState(onLoadError)
    LaunchedEffect(handler, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            handler.state.map { it.error != null }.distinctUntilChanged().collect { hasError ->
                if (hasError) navigate()
            }
        }
    }
}
