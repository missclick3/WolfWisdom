package ru.missclick.wolfwisdom.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.missclick.wolfwisdom.presentation.main.MainRoot
import ru.missclick.wolfwisdom.presentation.saved.SavedRoot
import ru.missclick.wolfwisdom.presentation.sharing.QuoteShareHost

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
            composable<Screen.Main> {
                MainRoot(
                    onShareClick = { shareQuote(it, null) },
                    onSupportClick = {
                        uriHandler.openUri("https://pay.cloudtips.ru/p/be52ff98")
                    },
                    onNavigateToSaved = {
                        navController.navigate(Screen.Saved) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<Screen.Saved> {
                SavedRoot(
                    onBackClick = { navController.popBackStack() },
                    onShareClick = { quote, scene -> shareQuote(quote.quote, scene) }
                )
            }
        }
    }
}
