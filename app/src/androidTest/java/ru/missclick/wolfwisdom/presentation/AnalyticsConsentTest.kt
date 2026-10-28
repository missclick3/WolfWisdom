package ru.missclick.wolfwisdom.presentation

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.analytics.*
import ru.missclick.wolfwisdom.presentation.consent.AnalyticsConsentDialog
import ru.missclick.wolfwisdom.presentation.consent.AnalyticsConsentHost
import ru.missclick.wolfwisdom.presentation.settings.SettingsAction
import ru.missclick.wolfwisdom.presentation.settings.SettingsScreen
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

class AnalyticsConsentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun allow_enablesCollection_andDialogStaysDismissedAfterRecomposition() {
        var collecting = false
        val controller = AnalyticsConsentController(AnalyticsConsentChoice.UNKNOWN, {},
            { collecting = it }, NoOpAnalyticsTracker)
        var generation by mutableIntStateOf(0)
        compose.setContent { key(generation) { WolfWisdomTheme { AnalyticsConsentHost(controller) } } }
        compose.onNodeWithTag("analytics-consent-allow").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(collecting); generation++ }
        compose.onNodeWithTag("analytics-consent-dialog").assertDoesNotExist()
    }

    @Test
    fun decline_keepsCollectionOff_andPersistsChoice() {
        var saved = AnalyticsConsentChoice.UNKNOWN
        val controller = AnalyticsConsentController(saved, { saved = it }, {}, NoOpAnalyticsTracker)
        compose.setContent { WolfWisdomTheme { AnalyticsConsentHost(controller) } }
        compose.onNodeWithTag("analytics-consent-decline").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(AnalyticsConsentChoice.DECLINED, saved) }
        compose.onNodeWithTag("analytics-consent-dialog").assertDoesNotExist()
    }

    @Test
    fun back_dismissesAsDeclined() {
        val controller = AnalyticsConsentController(AnalyticsConsentChoice.UNKNOWN, {}, {}, NoOpAnalyticsTracker)
        compose.setContent { WolfWisdomTheme { AnalyticsConsentHost(controller) } }
        compose.onNodeWithTag("analytics-consent-dialog").assertIsDisplayed()
        pressBack()
        compose.runOnIdle { assertEquals(AnalyticsConsentChoice.DECLINED, controller.choice.value) }
        compose.onNodeWithTag("analytics-consent-dialog").assertDoesNotExist()
    }

    @Test
    fun dialog_largeText_keepsBothChoicesReachable_andDetailsScroll() {
        compose.setContent {
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { setLocale(java.util.Locale.forLanguageTag("ru")) }
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f), LocalConfiguration provides config) {
                WolfWisdomTheme { AnalyticsConsentDialog {} }
            }
        }
        compose.onNodeWithTag("analytics-consent-allow").assertIsDisplayed()
        compose.onNodeWithTag("analytics-consent-decline").assertIsDisplayed()
        screenshot("consent-large-text")
        compose.onNodeWithTag("analytics-consent-more").performScrollTo().performClick()
        compose.onNodeWithTag("analytics-consent-details").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analytics-consent-decline").assertIsDisplayed()
    }

    @Test
    fun dialog_phone_hasStyledSurfaceAndVisibleChoices() {
        var selected: Boolean? = null
        compose.setContent { WolfWisdomTheme { AnalyticsConsentDialog { selected = it } } }
        compose.onNodeWithTag("analytics-consent-allow").assertIsDisplayed()
        compose.onNodeWithTag("analytics-consent-decline").assertIsDisplayed()
        screenshot("consent-phone")
        compose.onNodeWithTag("analytics-consent-backdrop").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(2f, 2f))
        }
        compose.runOnIdle { assertEquals(false, selected) }
    }

    @Test
    fun settingsToggle_changesControllerChoiceInBothDirections() {
        val controller = AnalyticsConsentController(AnalyticsConsentChoice.DECLINED, {}, {}, NoOpAnalyticsTracker)
        compose.setContent {
            val choice by controller.choice.collectAsState()
            WolfWisdomTheme {
                SettingsScreen(analyticsEnabled = choice == AnalyticsConsentChoice.GRANTED,
                    onAction = { if (it is SettingsAction.AnalyticsChanged) controller.choose(it.enabled) })
            }
        }
        val label = compose.activity.getString(R.string.settings_analytics)
        compose.onNodeWithText(label).performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText(label).performClick().assertIsOff()
        compose.runOnIdle { assertEquals(AnalyticsConsentChoice.DECLINED, controller.choice.value) }
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "consent-qa").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag("analytics-consent-dialog").captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
