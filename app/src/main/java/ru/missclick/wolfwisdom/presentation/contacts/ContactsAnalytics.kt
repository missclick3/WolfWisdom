package ru.missclick.wolfwisdom.presentation.contacts

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics

class ContactsAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "contacts") {
    fun onAction(action: ContactsAction) {
        val target = when (action) {
            ContactsAction.OpenBoosty -> "boosty"
            ContactsAction.OpenChannel -> "telegram_channel"
            ContactsAction.OpenTelegram -> "telegram_personal"
            ContactsAction.SendEmail -> "email"
            ContactsAction.BackPressed -> return
        }
        event("contact_clicked", "target" to target)
    }
}
