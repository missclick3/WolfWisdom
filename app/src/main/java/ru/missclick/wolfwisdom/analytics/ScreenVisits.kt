package ru.missclick.wolfwisdom.analytics

/** A retained navigation entry denotes the same visit across recomposition/recreation. */
internal fun screenVisitSource(
    lastEntryId: String?,
    entryId: String,
    previousBackStackEntryId: String?,
    screen: String
): String? = when {
    lastEntryId == entryId -> null
    lastEntryId == null -> if (screen == "main") "app_launch" else "internal"
    previousBackStackEntryId != lastEntryId -> "back"
    screen in setOf("saved", "settings", "contacts", "about") -> "menu"
    else -> "internal"
}
