package ru.missclick.wolfwisdom.logging

import android.util.Log
import ru.missclick.wolfwisdom.BuildConfig

/** Release constants allow R8 to remove these calls and their log messages. */
object AppLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.LOGGING_ENABLED) Log.d(tag, message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        if (BuildConfig.LOGGING_ENABLED) Log.w(tag, message, error)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (BuildConfig.LOGGING_ENABLED) Log.e(tag, message, error)
    }
}
