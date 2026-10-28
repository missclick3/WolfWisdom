package ru.missclick.wolfwisdom.analytics

import kotlinx.coroutines.CancellationException

/** A failed optional SDK stays disabled for this process, without changing consent. */
internal class AnalyticsCollectionController(
    private val prepare: () -> Unit,
    private val initialize: () -> Unit,
    private val setDataSendingEnabled: (Boolean) -> Unit,
    private val resumeSession: () -> Unit,
    private val pauseSession: () -> Unit,
    private val onFailure: (Exception) -> Unit
) {
    private var prepared = false
    private var initializationAttempted = false
    private var failed = false
    private var enabled = false

    @Synchronized
    fun setEnabled(value: Boolean) {
        if (!value) enabled = false
        if (failed) return
        safely {
            if (!prepared) {
                prepare()
                prepared = true
            }
            if (!value) {
                if (initializationAttempted) {
                    setDataSendingEnabled(false)
                    pauseSession()
                }
            } else {
                if (!initializationAttempted) {
                    // Activation may throw after partially initializing the SDK.
                    initializationAttempted = true
                    initialize()
                }
                setDataSendingEnabled(true)
                enabled = true
                resumeSession()
            }
        }
    }

    @Synchronized
    fun onResume() = runWhenEnabled(resumeSession)

    @Synchronized
    fun onPause() = runWhenEnabled(pauseSession)

    @Synchronized
    fun runWhenEnabled(block: () -> Unit) {
        if (enabled) safely(block)
    }

    private fun safely(block: () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            enabled = false
            failed = true
            throw cancelled
        } catch (error: Exception) {
            enabled = false
            failed = true
            onFailure(error)
            if (initializationAttempted) {
                // Each cleanup is independent: a transmission failure must not skip pause.
                cleanup { setDataSendingEnabled(false) }
                cleanup(pauseSession)
            }
        }
    }

    private fun cleanup(block: () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(error)
        }
    }
}
