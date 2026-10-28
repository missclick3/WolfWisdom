package ru.missclick.wolfwisdom.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import ru.missclick.wolfwisdom.analytics.LocalAnalyticsTracker
import ru.missclick.wolfwisdom.analytics.screenVisitSource
import ru.missclick.wolfwisdom.presentation.contacts.ContactsAnalytics
import ru.missclick.wolfwisdom.presentation.about.AboutAnalytics
import ru.missclick.wolfwisdom.presentation.about.AboutDocumentAnalytics
import ru.missclick.wolfwisdom.presentation.error.ErrorAnalytics
import ru.missclick.wolfwisdom.presentation.main.MainAnalytics
import ru.missclick.wolfwisdom.presentation.saved.SavedAnalytics
import ru.missclick.wolfwisdom.presentation.settings.SettingsAnalytics

@Composable
internal fun NavigationAnalytics(navController: NavHostController) {
    val tracker = LocalAnalyticsTracker.current
    var lastEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    // Navigation changes, not Activity lifecycle events, start a screen visit.
    LaunchedEffect(navController, tracker) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val analytics = when {
                entry.destination.hasRoute<Screen.Main>() -> MainAnalytics(tracker)
                entry.destination.hasRoute<Screen.Saved>() -> SavedAnalytics(tracker)
                entry.destination.hasRoute<Screen.Settings>() -> SettingsAnalytics(tracker)
                entry.destination.hasRoute<Screen.Contacts>() -> ContactsAnalytics(tracker)
                entry.destination.hasRoute<Screen.About>() -> AboutAnalytics(tracker)
                entry.destination.hasRoute<Screen.LegalDocument>() -> AboutDocumentAnalytics(tracker, entry.toRoute<Screen.LegalDocument>().document)
                entry.destination.hasRoute<Screen.Error>() -> ErrorAnalytics(tracker, entry.toRoute<Screen.Error>().source)
                else -> return@collect
            }
            val source = screenVisitSource(lastEntryId, entry.id,
                navController.previousBackStackEntry?.id, analytics.screen)
            if (source != null) {
                analytics.opened(source)
                lastEntryId = entry.id
            }
        }
    }
}
