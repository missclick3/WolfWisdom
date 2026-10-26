package ru.missclick.wolfwisdom.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/** Outgoing screens stay composed during transitions, but must not navigate again. */
internal fun NavHostController.navigateFrom(
    entry: NavBackStackEntry,
    action: NavHostController.() -> Unit
) {
    if (currentBackStackEntry === entry &&
        entry.lifecycle.currentState == Lifecycle.State.RESUMED
    ) {
        action()
    }
}

/** Keep the root destination even if a Back callback is delivered more than once. */
internal fun NavHostController.popBackStackFrom(entry: NavBackStackEntry) {
    if (currentBackStackEntry === entry && previousBackStackEntry != null) {
        popBackStack()
    }
}
