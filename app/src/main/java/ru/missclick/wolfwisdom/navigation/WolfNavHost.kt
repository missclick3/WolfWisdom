package ru.missclick.wolfwisdom.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.missclick.wolfwisdom.presentation.main.MainRoot
import ru.missclick.wolfwisdom.presentation.saved.SavedRoot
import ru.missclick.wolfwisdom.presentation.saved.SavedScreen

@Composable
fun WolfNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main,
        modifier = modifier
    ) {
        composable<Screen.Main> {
            MainRoot(
                onNavigateToSaved = {
                    navController.navigate(Screen.Saved) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Screen.Saved> {
            SavedRoot()
        }
    }
}
