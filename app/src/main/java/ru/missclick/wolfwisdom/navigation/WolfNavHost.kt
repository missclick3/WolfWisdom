package ru.missclick.wolfwisdom.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.missclick.wolfwisdom.BuildConfig
import ru.missclick.wolfwisdom.presentation.contacts.ContactsRoot
import ru.missclick.wolfwisdom.presentation.main.MainRoot
import ru.missclick.wolfwisdom.presentation.saved.SavedRoot
import ru.missclick.wolfwisdom.presentation.settings.SettingsRoot
import ru.missclick.wolfwisdom.presentation.sharing.QuoteShareHost
import ru.missclick.wolfwisdom.presentation.error.ErrorRoot
import ru.missclick.wolfwisdom.presentation.error.LoadErrorHandler
import ru.missclick.wolfwisdom.presentation.main.MainViewModel
import ru.missclick.wolfwisdom.presentation.saved.SavedViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WolfNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val uriHandler = LocalUriHandler.current
    QuoteShareHost(modifier) { shareQuote ->
        NavHost(
            navController = navController,
            startDestination = Screen.Main
        ) {
            composable<Screen.Main> { entry ->
                MainRoot(
                    onLoadError = {
                        navController.navigateFrom(entry) {
                            navigate(Screen.Error(LoadErrorSource.MAIN)) { launchSingleTop = true }
                        }
                    },
                    onShareClick = { shareQuote(it, null) },
                    onSupportClick = {
                        uriHandler.openUri(BuildConfig.SUPPORT_LINK)
                    },
                    onNavigateToSaved = {
                        navController.navigateFrom(entry) {
                            navigate(Screen.Saved) { launchSingleTop = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigateFrom(entry) {
                            navigate(Screen.Settings) { launchSingleTop = true }
                        }
                    },
                    onNavigateToContacts = {
                        navController.navigateFrom(entry) {
                            navigate(Screen.Contacts) { launchSingleTop = true }
                        }
                    }
                )
            }
            composable<Screen.Saved> { entry ->
                SavedRoot(
                    onLoadError = {
                        navController.navigateFrom(entry) {
                            navigate(Screen.Error(LoadErrorSource.SAVED)) { launchSingleTop = true }
                        }
                    },
                    onBackClick = { navController.popBackStackFrom(entry) },
                    onShareClick = { quote, scene -> shareQuote(quote.quote, scene) }
                )
            }
            composable<Screen.Settings> { entry ->
                SettingsRoot(onBackClick = { navController.popBackStackFrom(entry) })
            }
            composable<Screen.Contacts> { entry ->
                ContactsRoot(onBackClick = { navController.popBackStackFrom(entry) })
            }
            composable<Screen.Error> { entry ->
                val source = entry.toRoute<Screen.Error>().source
                val owner = remember(entry, source) {
                    when (source) {
                        LoadErrorSource.MAIN -> navController.getBackStackEntry<Screen.Main>()
                        LoadErrorSource.SAVED -> navController.getBackStackEntry<Screen.Saved>()
                    }
                }
                val handler: LoadErrorHandler<*> = when (source) {
                    LoadErrorSource.MAIN -> koinViewModel<MainViewModel>(viewModelStoreOwner = owner).errorHandler
                    LoadErrorSource.SAVED -> koinViewModel<SavedViewModel>(viewModelStoreOwner = owner).errorHandler
                }
                val error by handler.state.collectAsStateWithLifecycle()
                LaunchedEffect(error.error) {
                    if (error.error == null) navController.popBackStackFrom(entry)
                }
                val activity = LocalActivity.current
                BackHandler {
                    navController.navigateFrom(entry) {
                        when (source) {
                            LoadErrorSource.MAIN -> activity?.finish()
                            LoadErrorSource.SAVED -> popBackStack<Screen.Saved>(inclusive = true)
                        }
                    }
                }
                ErrorRoot(onRetry = handler::retry)
            }
        }
    }
}
