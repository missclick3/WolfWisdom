package ru.missclick.wolfwisdom.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationActionsTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController

    @Before
    fun setUp() {
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = Screen.Main) {
                composable<Screen.Main> { Text("Home") }
                composable<Screen.Saved> { Text("Saved") }
                composable<Screen.Settings> { Text("Settings") }
                composable<Screen.Contacts> { Text("Contacts") }
                composable<Screen.Error> { Text("Error") }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun repeatedBackFromEverySecondaryScreenKeepsHome() {
        for (screen in listOf(Screen.Saved, Screen.Settings, Screen.Contacts)) {
            compose.runOnIdle {
                when (screen) {
                    Screen.Saved -> controller.navigate(Screen.Saved)
                    Screen.Settings -> controller.navigate(Screen.Settings)
                    Screen.Contacts -> controller.navigate(Screen.Contacts)
                    else -> error("Unexpected screen")
                }
            }
            compose.waitForIdle()
            compose.runOnIdle {
                val outgoing = controller.currentBackStackEntry!!
                // Both events arrive before Compose removes the outgoing screen.
                repeat(10) { controller.popBackStackFrom(outgoing) }
                assertTrue(controller.currentDestination!!.hasRoute<Screen.Main>())
            }
            compose.waitForIdle()
        }
    }

    @Test
    fun rapidDifferentMenuActionsOnlyOpenFirstDestination() {
        compose.runOnIdle {
            val home = controller.currentBackStackEntry!!
            controller.navigateFrom(home) { navigate(Screen.Settings) }
            controller.navigateFrom(home) { navigate(Screen.Contacts) }
            controller.navigateFrom(home) { navigate(Screen.Saved) }
            assertTrue(controller.currentDestination!!.hasRoute<Screen.Settings>())
            controller.popBackStackFrom(controller.currentBackStackEntry!!)
            assertTrue(controller.currentDestination!!.hasRoute<Screen.Main>())
            assertNull(controller.previousBackStackEntry)
        }
    }

    @Test
    fun lateErrorRecoveryCannotPopHomeOrANewDestination() {
        compose.runOnIdle { controller.navigate(Screen.Error(LoadErrorSource.MAIN)) }
        compose.waitForIdle()
        val errorEntry = compose.runOnIdle { controller.currentBackStackEntry!! }
        compose.runOnIdle { controller.popBackStackFrom(errorEntry) }
        compose.waitForIdle()
        compose.runOnIdle {
            controller.popBackStackFrom(errorEntry)
            assertTrue(controller.currentDestination!!.hasRoute<Screen.Main>())
            controller.navigate(Screen.Contacts)
            controller.popBackStackFrom(errorEntry)
            assertTrue(controller.currentDestination!!.hasRoute<Screen.Contacts>())
        }
    }

    @Test
    fun rootCannotBePoppedByBackCallback() {
        compose.runOnIdle {
            repeat(10) { controller.popBackStackFrom(controller.currentBackStackEntry!!) }
            assertNotNull(controller.currentBackStackEntry)
            assertTrue(controller.currentDestination!!.hasRoute<Screen.Main>())
        }
    }
}
