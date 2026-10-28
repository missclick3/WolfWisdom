package ru.missclick.wolfwisdom.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AnalyticsConsentChoice { UNKNOWN, DECLINED, GRANTED }

/** Drops events while disabled; nothing is buffered for a later grant. */
class AnalyticsConsentController(
    initialChoice: AnalyticsConsentChoice,
    private val saveChoice: (AnalyticsConsentChoice) -> Unit,
    private val setCollectionEnabled: (Boolean) -> Unit,
    private val tracker: AnalyticsTracker
) : AnalyticsTracker {
    private val mutableChoice = MutableStateFlow(initialChoice)
    val choice = mutableChoice.asStateFlow()

    init {
        setCollectionEnabled(initialChoice == AnalyticsConsentChoice.GRANTED)
    }

    @Synchronized
    fun choose(enabled: Boolean) {
        val next = if (enabled) AnalyticsConsentChoice.GRANTED else AnalyticsConsentChoice.DECLINED
        if (mutableChoice.value == next) return
        // Close the gate before changing the SDK or publishing the new UI state.
        if (!enabled) mutableChoice.value = next
        saveChoice(next)
        setCollectionEnabled(enabled)
        mutableChoice.value = next
    }

    @Synchronized
    override fun track(name: String, parameters: Map<String, Any>) {
        if (mutableChoice.value == AnalyticsConsentChoice.GRANTED) tracker.track(name, parameters)
    }
}
