package ru.missclick.wolfwisdom.presentation.saved

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics

class SavedAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "saved") {
    fun onAction(action: SavedAction) {
        when (action) {
            SavedAction.OpenSearch -> event("search_open_clicked")
            SavedAction.CloseSearch -> event("search_close_clicked")
            is SavedAction.RemoveQuote -> event("quote_remove_clicked")
            else -> Unit // Never transmit search input or log each keystroke.
        }
    }

    fun shareClicked() = event("quote_share_clicked")
    fun widgetClicked() = event("quote_widget_clicked")
    fun searchClearClicked() = event("search_clear_clicked")
}
