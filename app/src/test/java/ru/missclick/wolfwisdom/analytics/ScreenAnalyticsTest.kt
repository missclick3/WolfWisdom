package ru.missclick.wolfwisdom.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.missclick.wolfwisdom.navigation.LoadErrorSource
import ru.missclick.wolfwisdom.presentation.error.ErrorAnalytics
import ru.missclick.wolfwisdom.presentation.main.MainAction
import ru.missclick.wolfwisdom.presentation.main.MainAnalytics
import ru.missclick.wolfwisdom.presentation.main.MainState
import ru.missclick.wolfwisdom.presentation.saved.SavedAction
import ru.missclick.wolfwisdom.presentation.saved.SavedAnalytics
import ru.missclick.wolfwisdom.widget.WidgetPlacementAnalytics

class ScreenAnalyticsTest {
    private data class Event(val name: String, val parameters: Map<String, Any>)

    private class RecordingTracker : AnalyticsTracker {
        val events = mutableListOf<Event>()
        override fun track(name: String, parameters: Map<String, Any>) {
            events += Event(name, parameters)
        }
    }

    @Test
    fun mainCallbacks_internalReloadAndAnimation_doNotEmitUserActions() {
        val tracker = RecordingTracker()
        val analytics = MainAnalytics(tracker)

        listOf(MainAction.Reload, MainAction.DismissOptions, MainAction.QuoteAnimationFinished(1))
            .forEach { analytics.onAction(it, MainState()) }

        assertTrue(tracker.events.isEmpty())
    }

    @Test
    fun saveClick_savedQuote_recordsRemovalIntentWithoutQuoteContent() {
        val tracker = RecordingTracker()
        MainAnalytics(tracker).onAction(MainAction.ToggleQuoteSaved,
            MainState(quote = "Private test content", quoteId = 123, isQuoteSaved = true))

        assertEquals(listOf(Event("quote_save_clicked", mapOf("screen" to "main", "action" to "remove"))), tracker.events)
    }

    @Test
    fun savedSearch_typingAndReload_doNotTransmitSearchText() {
        val tracker = RecordingTracker()
        val analytics = SavedAnalytics(tracker)

        analytics.onAction(SavedAction.SearchQueryChanged("personal search"))
        analytics.onAction(SavedAction.Reload)

        assertTrue(tracker.events.isEmpty())
    }

    @Test
    fun errorScreen_visitAndRetry_preserveFailingScreenContext() {
        val tracker = RecordingTracker()
        val analytics = ErrorAnalytics(tracker, LoadErrorSource.SAVED)

        analytics.opened("internal")
        analytics.retryClicked()

        assertEquals(listOf(
            Event("screen_opened", mapOf("screen" to "error", "error_source" to "saved", "source" to "internal")),
            Event("error_retry_clicked", mapOf("screen" to "error", "error_source" to "saved"))
        ), tracker.events)
    }

    @Test
    fun widgetCreateClick_recordsIntentWithoutClaimingSuccessfulPlacement() {
        val tracker = RecordingTracker()

        WidgetPlacementAnalytics(tracker).createClicked()

        assertEquals(listOf(Event("widget_create_clicked", mapOf("screen" to "widget_placement", "surface" to "placement"))), tracker.events)
    }
}
