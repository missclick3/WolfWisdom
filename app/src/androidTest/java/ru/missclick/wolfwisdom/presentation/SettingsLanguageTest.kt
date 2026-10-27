package ru.missclick.wolfwisdom.presentation

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.missclick.wolfwisdom.MainActivity
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.settings.SettingsRoot
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

class SettingsLanguageTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun languagePickerUpdatesResourcesAndSurvivesActivityRecreation() {
        val original = AppCompatDelegate.getApplicationLocales()
        try {
            compose.runOnUiThread {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                assertEquals("en", AppCompatDelegate.getApplicationLocales().toLanguageTags())
            }
            awaitLanguage("en")

            for ((tag, label, heading) in listOf(
                Triple("ru", "Русский", "Язык"),
                Triple("en", "English", "Language")
            )) {
                showSettings()
                compose.onNodeWithText(label).performScrollTo().performClick()
                awaitLanguage(tag)
                assertEquals(heading, compose.activity.getString(R.string.settings_language))

                // Recreate the activity while keeping the selected app locale.
                compose.activityRule.scenario.recreate()
                awaitLanguage(tag)
                showSettings()
                compose.onNodeWithText(label).performScrollTo().assertIsSelected()
                compose.onNodeWithText(heading).assertExists()
            }
        } finally {
            compose.runOnUiThread { AppCompatDelegate.setApplicationLocales(original) }
        }
    }

    private fun showSettings() {
        compose.runOnUiThread {
            compose.activity.setContent {
                WolfWisdomTheme { SettingsRoot() }
            }
        }
        compose.waitForIdle()
    }

    private fun awaitLanguage(tag: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.activity.resources.configuration.locales[0].language == tag
        }
        compose.waitForIdle()
    }
}
