package ru.missclick.wolfwisdom.presentation.main

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics

class MainAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "main") {
    fun onAction(action: MainAction, state: MainState) {
        when (action) {
            MainAction.ToggleOptions -> event(if (state.isOptionsVisible) "menu_close_clicked" else "menu_open_clicked")
            MainAction.NavigateToSaved -> menuItem("saved")
            MainAction.NavigateToContacts -> menuItem("contacts")
            MainAction.NavigateToSettings -> menuItem("settings")
            MainAction.NavigateToAbout -> menuItem("about")
            MainAction.SupportClick -> menuItem("support")
            MainAction.ToggleQuoteSaved -> event("quote_save_clicked", "action" to if (state.isQuoteSaved) "remove" else "save")
            MainAction.ShareClick -> event("quote_share_clicked")
            MainAction.WidgetClick -> event("quote_widget_clicked")
            MainAction.SwipeUp -> event("quote_swiped", "direction" to "next")
            MainAction.SwipeDown -> event("quote_swiped", "direction" to "previous")
            else -> Unit // Reloads, animation callbacks and dismissals are not button clicks.
        }
    }

    private fun menuItem(item: String) = event("menu_item_clicked", "item" to item)
}
