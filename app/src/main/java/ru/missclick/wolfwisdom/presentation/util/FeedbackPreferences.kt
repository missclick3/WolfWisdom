package ru.missclick.wolfwisdom.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ru.missclick.wolfwisdom.data.local.FeedbackPreferences
import ru.missclick.wolfwisdom.data.local.FeedbackSettings

@Composable
internal fun rememberFeedbackPreferences(): FeedbackPreferences {
    val context = LocalContext.current.applicationContext
    return remember(context) { FeedbackPreferences(context) }
}

@Composable
internal fun rememberFeedbackSettings(preferences: FeedbackPreferences): State<FeedbackSettings> {
    val state = remember(preferences) { mutableStateOf(preferences.settings) }
    DisposableEffect(preferences) {
        val unsubscribe = preferences.observe { state.value = it }
        onDispose { unsubscribe() }
    }
    return state
}
