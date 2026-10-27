package ru.missclick.wolfwisdom.analytics

/** Provider-independent transport. Screen classes own event names and parameters. */
fun interface AnalyticsTracker {
    fun track(name: String, parameters: Map<String, Any>)
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(name: String, parameters: Map<String, Any>) = Unit
}

interface ScreenAnalytics {
    val screen: String
    fun opened(source: String)
    fun backClicked()
}

abstract class BaseScreenAnalytics(
    protected val tracker: AnalyticsTracker,
    final override val screen: String
) : ScreenAnalytics {
    protected open val context: Map<String, Any> get() = emptyMap()

    protected fun event(name: String, vararg parameters: Pair<String, Any>) {
        tracker.track(name, context + mapOf("screen" to screen) + parameters.toMap())
    }

    final override fun opened(source: String) = event("screen_opened", "source" to source)
    final override fun backClicked() = event("back_clicked")
}
