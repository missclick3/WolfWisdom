package ru.missclick.wolfwisdom.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.missclick.wolfwisdom.R

@Composable
internal fun rememberQuoteFlipSound(): (Long) -> Unit {
    val context = LocalContext.current.applicationContext
    val isPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sound = remember(context, isPreview) {
        if (isPreview) null else QuoteFlipSound(context)
    }

    DisposableEffect(sound) {
        onDispose { sound?.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { sound?.stop() }

    return remember(sound, lifecycleOwner) {
        { transitionId ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                sound?.play(transitionId)
            }
        }
    }
}

private class QuoteFlipSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build())
        .build()
    @Volatile private var isLoaded = false
    @Volatile private var isReleased = false
    private val soundId: Int
    private var streamId = 0
    private var lastTransitionId: Long? = null

    init {
        pool.setOnLoadCompleteListener { _, _, status ->
            isLoaded = status == 0 && !isReleased
        }
        soundId = pool.load(context, R.raw.card_flip_mystic_wind, 1)
    }

    fun play(transitionId: Long) {
        if (lastTransitionId == transitionId) return
        lastTransitionId = transitionId
        if (!isLoaded || isReleased) return
        stop()
        streamId = pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun stop() {
        if (!isReleased && streamId != 0) pool.stop(streamId)
        streamId = 0
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        isLoaded = false
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}
