package ru.missclick.wolfwisdom.presentation

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.missclick.wolfwisdom.MainActivity
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.data.local.FeedbackPreferences
import ru.missclick.wolfwisdom.data.local.FeedbackSettings
import ru.missclick.wolfwisdom.presentation.components.QuoteActionButtons
import ru.missclick.wolfwisdom.presentation.settings.SettingsRoot
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

class FeedbackSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var preferences: FeedbackPreferences
    private lateinit var original: FeedbackSettings

    @Before
    fun setUp() {
        preferences = FeedbackPreferences(compose.activity)
        original = preferences.settings
        preferences.setSoundEffectsEnabled(true)
        preferences.setVibrationEnabled(true)
    }

    @After
    fun restorePreferences() {
        preferences.setSoundEffectsEnabled(original.soundEffectsEnabled)
        preferences.setVibrationEnabled(original.vibrationEnabled)
    }

    @Test
    fun togglesPersistIndependentlyThroughRecreation() {
        val soundLabel = compose.activity.getString(R.string.settings_sound)
        val vibrationLabel = compose.activity.getString(R.string.settings_vibration)
        showSettings()
        compose.onNodeWithText(soundLabel).assertIsOn().performClick().assertIsOff()
        assertFalse(preferences.settings.soundEffectsEnabled)
        assertTrue(preferences.settings.vibrationEnabled)
        compose.onNodeWithText(vibrationLabel).performClick().assertIsOff()

        compose.activityRule.scenario.recreate()
        showSettings()
        compose.onNodeWithText(soundLabel).assertIsOff().performClick().assertIsOn()
        assertTrue(FeedbackPreferences(compose.activity).settings.soundEffectsEnabled)
        assertFalse(preferences.settings.vibrationEnabled)
        compose.onNodeWithText(vibrationLabel).performScrollTo().assertIsOff().performClick().assertIsOn()
        assertTrue(preferences.settings.vibrationEnabled)
    }

    @Test
    fun actionButtonsReadVibrationPreferenceOnEveryClick() {
        var hapticCalls = 0
        var actionCalls = 0
        val feedbackView = object : View(compose.activity) {
            override fun performHapticFeedback(feedbackConstant: Int): Boolean {
                hapticCalls++
                return true
            }
        }
        val actionLabels = listOf(R.string.save_quote, R.string.share, R.string.set_widget)
            .map { compose.activity.getString(it) }
        compose.runOnUiThread {
            (compose.activity.window.decorView as ViewGroup).addView(
                feedbackView, ViewGroup.LayoutParams(0, 0)
            )
            compose.activity.setContent {
                WolfWisdomTheme {
                    CompositionLocalProvider(LocalView provides feedbackView) {
                        QuoteActionButtons(
                            isSaved = false,
                            onSaveClick = { actionCalls++ },
                            onShareClick = { actionCalls++ },
                            onWidgetClick = { actionCalls++ }
                        )
                    }
                }
            }
        }
        for (vibrationEnabled in listOf(true, false, true)) {
            // Sound and vibration must remain independent.
            preferences.setSoundEffectsEnabled(false)
            preferences.setVibrationEnabled(vibrationEnabled)
            val before = hapticCalls
            actionLabels.forEach { compose.onNodeWithContentDescription(it).performClick() }
            compose.runOnIdle {
                assertEquals(before + if (vibrationEnabled) 3 else 0, hapticCalls)
            }
        }
        compose.runOnIdle { assertEquals(9, actionCalls) }
    }

    private fun showSettings() {
        compose.runOnUiThread {
            compose.activity.setContent { WolfWisdomTheme { SettingsRoot() } }
        }
        compose.waitForIdle()
    }
}
