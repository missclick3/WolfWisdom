package ru.missclick.wolfwisdom.analytics

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class AnalyticsCollectionControllerTest {
    private class Harness {
        val calls = mutableListOf<String>()
        val failures = mutableListOf<Exception>()
        var failureAt: String? = null
        var failure: Throwable = IllegalStateException("SDK unavailable")
        private fun call(name: String) {
            calls += name
            if (name == failureAt) throw failure
        }
        val runtime = AnalyticsCollectionController(
            prepare = { call("prepare") },
            initialize = { call("initialize") },
            setDataSendingEnabled = { call("sending:$it") },
            resumeSession = { call("resume") },
            pauseSession = { call("pause") },
            onFailure = { failures += it }
        )
        fun event() = runtime.runWhenEnabled { call("event") }
    }

    @Test fun setEnabled_withoutConsent_doesNotCallSdkOrForwardEvents() {
        val h = Harness()
        h.runtime.setEnabled(false)
        h.runtime.onResume()
        h.runtime.onPause()
        h.event()
        assertEquals(listOf("prepare"), h.calls)
    }

    @Test fun setEnabled_afterRevocation_reusesInitializationAndDropsDisabledEvents() {
        val h = Harness()
        h.runtime.setEnabled(true)
        h.event()
        h.runtime.setEnabled(false)
        h.event()
        h.runtime.onResume()
        h.runtime.setEnabled(true)
        h.event()
        assertEquals(listOf("prepare", "initialize", "sending:true", "resume", "event",
            "sending:false", "pause", "sending:true", "resume", "event"), h.calls)
    }

    @Test fun setEnabled_activationFailure_disablesPartialSdkAndDoesNotRetryInProcess() {
        val h = Harness().apply { failureAt = "initialize" }
        h.runtime.setEnabled(true)
        h.event()
        h.runtime.setEnabled(false)
        h.runtime.setEnabled(true)
        assertEquals(listOf("prepare", "initialize", "sending:false", "pause"), h.calls)
        assertEquals(listOf(h.failure), h.failures)
    }

    @Test fun setEnabled_preparationFailure_doesNotAttemptSdkInitialization() {
        val h = Harness().apply { failureAt = "prepare" }
        h.runtime.setEnabled(true)
        h.event()
        assertEquals(listOf("prepare"), h.calls)
        assertEquals(listOf(h.failure), h.failures)
    }

    @Test fun setEnabled_enableFailure_blocksEventsAndAttemptsShutdown() {
        val h = Harness().apply { failureAt = "sending:true" }
        h.runtime.setEnabled(true)
        h.event()
        assertEquals(listOf("prepare", "initialize", "sending:true", "sending:false", "pause"), h.calls)
        assertEquals(listOf(h.failure), h.failures)
    }

    @Test fun onResume_sdkFailure_disablesLaterEventsAndLifecycleCalls() {
        val h = Harness()
        h.runtime.setEnabled(true)
        h.calls.clear()
        h.failureAt = "resume"
        h.runtime.onResume()
        h.runtime.onPause()
        h.runtime.onResume()
        h.event()
        assertEquals(listOf("resume", "sending:false", "pause"), h.calls)
        assertEquals(listOf(h.failure), h.failures)
    }

    @Test fun onPause_sdkFailure_isContainedEvenWhenCleanupAlsoFails() {
        val h = Harness()
        h.runtime.setEnabled(true)
        h.calls.clear()
        h.failureAt = "pause"
        h.runtime.onPause()
        h.event()
        assertEquals(listOf("pause", "sending:false", "pause"), h.calls)
        assertEquals(2, h.failures.size)
    }

    @Test fun setEnabled_disableFailure_closesEventGateAndStillAttemptsPause() {
        val h = Harness()
        h.runtime.setEnabled(true)
        h.calls.clear()
        h.failureAt = "sending:false"
        h.runtime.setEnabled(false)
        h.event()
        assertEquals(listOf("sending:false", "sending:false", "pause"), h.calls)
        assertEquals(2, h.failures.size)
    }

    @Test fun report_sdkFailure_blocksSubsequentEvents() {
        val h = Harness()
        h.runtime.setEnabled(true)
        h.calls.clear()
        h.failureAt = "event"
        h.event()
        h.event()
        assertEquals(listOf("event", "sending:false", "pause"), h.calls)
    }

    @Test fun consent_savedGrant_survivesSdkFailureAndCanStillBeRevoked() {
        val h = Harness().apply { failureAt = "initialize" }
        var persisted = AnalyticsConsentChoice.GRANTED
        val consent = AnalyticsConsentController(persisted,
            saveChoice = { persisted = it }, setCollectionEnabled = h.runtime::setEnabled,
            tracker = AnalyticsTracker { _, _ -> h.event() })
        consent.track("screen_opened", emptyMap())
        assertEquals(AnalyticsConsentChoice.GRANTED, consent.choice.value)
        consent.choose(false)
        assertEquals(AnalyticsConsentChoice.DECLINED, persisted)
        assertEquals(persisted, consent.choice.value)
        assertFalse("event" in h.calls)
    }

    @Test fun setEnabled_cancellation_propagatesWithoutReportingOrdinaryFailure() {
        val h = Harness().apply { failureAt = "initialize"; failure = CancellationException("Cancelled") }
        try {
            h.runtime.setEnabled(true)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(h.failure, actual)
        }
        assertTrue(h.failures.isEmpty())
    }

    @Test fun setEnabled_fatalError_propagatesWithoutReportingOrdinaryFailure() {
        val h = Harness().apply { failureAt = "initialize"; failure = AssertionError("Fatal") }
        try {
            h.runtime.setEnabled(true)
            fail("Fatal errors must propagate")
        } catch (actual: AssertionError) {
            assertSame(h.failure, actual)
        }
        assertTrue(h.failures.isEmpty())
    }
}
