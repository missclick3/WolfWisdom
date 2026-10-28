package ru.missclick.wolfwisdom.presentation.about

import ru.missclick.wolfwisdom.analytics.AnalyticsTracker
import ru.missclick.wolfwisdom.analytics.BaseScreenAnalytics

@androidx.annotation.Keep
@kotlinx.serialization.Serializable
enum class AboutDocument(val screen: String) {
    LICENSES("licenses"), PRIVACY_POLICY("privacy_policy"), TERMS("terms")
}

class AboutAnalytics(tracker: AnalyticsTracker) : BaseScreenAnalytics(tracker, "about") {
    fun documentClicked(document: AboutDocument) = event("about_document_clicked", "document" to document.screen)
}

class AboutDocumentAnalytics(tracker: AnalyticsTracker, document: AboutDocument) : BaseScreenAnalytics(tracker, document.screen)
