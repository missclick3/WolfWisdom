package ru.missclick.wolfwisdom.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import ru.missclick.wolfwisdom.presentation.about.AboutRoot
import ru.missclick.wolfwisdom.presentation.about.LegalDocumentRoot
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
import ru.missclick.wolfwisdom.widget.WidgetPlacementHost
import ru.missclick.wolfwisdom.widget.WidgetPlacementViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WolfNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavigationAnalytics(navController)
    val uriHandler = LocalUriHandler.current
    val widgetPlacement: WidgetPlacementViewModel = viewModel()
    QuoteShareHost(modifier) { shareQuote ->
        NavHost(
            navController = navController,
            startDestination = Screen.Main,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            composable<Screen.Main> { entry ->
                val mainViewModel = koinViewModel<MainViewModel>()
                MainRoot(
                    viewModel = mainViewModel,
                    onWidgetClick = { widgetPlacement.open(mainViewModel.state.value.quoteId, "main") },
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
                    },
                    onNavigateToAbout = {
                        navController.navigateFrom(entry) {
                            navigate(Screen.About) { launchSingleTop = true }
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
                    onWidgetClick = { widgetPlacement.open(it.id, "saved") },
                    onShareClick = { quote, scene -> shareQuote(quote.quote, scene) }
                )
            }
            composable<Screen.Settings> { entry ->
                SettingsRoot(onBackClick = { navController.popBackStackFrom(entry) })
            }
            composable<Screen.Contacts> { entry ->
                ContactsRoot(onBackClick = { navController.popBackStackFrom(entry) })
            }
            composable<Screen.About> { entry ->
                AboutRoot(
                    onBackClick = { navController.popBackStackFrom(entry) },
                    onDocumentClick = { document ->
                        navController.navigateFrom(entry) {
                            navigate(Screen.LegalDocument(document)) { launchSingleTop = true }
                        }
                    }
                )
            }
            composable<Screen.LegalDocument> { entry ->
                LegalDocumentRoot(
                    document = entry.toRoute<Screen.LegalDocument>().document,
                    onBackClick = { navController.popBackStackFrom(entry) }
                )
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
                ErrorRoot(onRetry = handler::retry, source = source)
            }
        }
    }
    WidgetPlacementHost(widgetPlacement)
}
