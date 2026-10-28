package ru.missclick.wolfwisdom.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsConsentControllerTest {
    private class Harness(initial: AnalyticsConsentChoice = AnalyticsConsentChoice.UNKNOWN) {
        var persisted = initial
        var collecting = false
        val events = mutableListOf<String>()
        val controller = AnalyticsConsentController(initial,
            saveChoice = { persisted = it },
            setCollectionEnabled = { collecting = it },
            tracker = AnalyticsTracker { name, _ -> events += name })
    }

    @Test
    fun track_beforeDecision_dropsEventsWithoutStartingCollection() {
        val h = Harness()
        h.controller.track("screen_opened", emptyMap())
        assertFalse(h.collecting)
        assertTrue(h.events.isEmpty())
        assertEquals(AnalyticsConsentChoice.UNKNOWN, h.controller.choice.value)
    }

    @Test
    fun choose_grant_doesNotReplayEarlierEvents() {
        val h = Harness()
        h.controller.track("before_consent", emptyMap())
        h.controller.choose(true)
        h.controller.track("after_consent", emptyMap())
        assertTrue(h.collecting)
        assertEquals(listOf("after_consent"), h.events)
        assertEquals(AnalyticsConsentChoice.GRANTED, h.persisted)
    }

    @Test
    fun choose_decline_persistsDecisionAndKeepsAllEventsBlocked() {
        val h = Harness()
        h.controller.choose(false)
        h.controller.track("screen_opened", emptyMap())
        val restored = Harness(h.persisted)
        assertEquals(AnalyticsConsentChoice.DECLINED, restored.controller.choice.value)
        assertFalse(restored.collecting)
        assertTrue(h.events.isEmpty())
    }

    @Test
    fun choose_revokeThenGrant_dropsEventsFromDisabledInterval() {
        val h = Harness(AnalyticsConsentChoice.GRANTED)
        h.controller.track("allowed", emptyMap())
        h.controller.choose(false)
        assertFalse(h.collecting)
        h.controller.track("revoked", emptyMap())
        h.controller.choose(true)
        h.controller.track("allowed_again", emptyMap())
        assertEquals(listOf("allowed", "allowed_again"), h.events)
    }

    @Test
    fun initialize_savedGrant_enablesCollectionWithoutAnotherPrompt() {
        val h = Harness(AnalyticsConsentChoice.GRANTED)
        assertTrue(h.collecting)
        assertEquals(AnalyticsConsentChoice.GRANTED, h.controller.choice.value)
    }

    @Test
    fun choose_revocation_closesEventGateBeforeUpdatingBackend() {
        val events = mutableListOf<String>()
        lateinit var controller: AnalyticsConsentController
        var initialized = false
        controller = AnalyticsConsentController(AnalyticsConsentChoice.GRANTED, {},
            setCollectionEnabled = { enabled ->
                if (initialized && !enabled) controller.track("during_revocation", emptyMap())
            }, tracker = AnalyticsTracker { name, _ -> events += name })
        initialized = true
        controller.choose(false)
        assertTrue(events.isEmpty())
    }
}
