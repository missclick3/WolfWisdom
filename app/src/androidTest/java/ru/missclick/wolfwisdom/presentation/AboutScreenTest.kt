package ru.missclick.wolfwisdom.presentation

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.about.AboutDocument
import ru.missclick.wolfwisdom.presentation.about.AboutRoot
import ru.missclick.wolfwisdom.presentation.about.AboutScreen
import ru.missclick.wolfwisdom.presentation.about.LegalDocumentScreen
import ru.missclick.wolfwisdom.presentation.about.PRIVACY_POLICY_URL
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@RunWith(Parameterized::class)
class AboutScreenTest(private val width: Int, private val height: Int, private val fontScale: Float) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val labels = listOf("Лицензии", "Политика конфиденциальности", "Условия использования")

    @Test
    fun documentButtonsOpenPrivacyInBrowserAndOtherDocumentsInApp() {
        val opened = mutableListOf<AboutDocument>()
        val openedUrls = mutableListOf<String>()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) { openedUrls += uri }
        }
        var backs = 0
        compose.setContent {
            Window {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    AboutRoot({ backs++ }, { opened += it })
                }
            }
        }
        screenshot("about")
        labels.forEach { label ->
            compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
        }
        compose.runOnIdle {
            assertEquals(listOf(AboutDocument.LICENSES, AboutDocument.TERMS), opened)
            assertEquals(listOf(PRIVACY_POLICY_URL), openedUrls)
        }
        compose.onNodeWithText("О приложении").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun localDocumentsLoadOfflineScrollAndReturnToAbout() {
        var document by mutableStateOf<AboutDocument?>(null)
        compose.setContent {
            Window {
                val selected = document
                if (selected == null) AboutScreen({}, { document = it })
                else LegalDocumentScreen(selected) { document = null }
            }
        }
        labels.filterIndexed { index, _ -> AboutDocument.entries[index] != AboutDocument.PRIVACY_POLICY }
            .forEachIndexed { index, label ->
            compose.onNodeWithText(label).performScrollTo().performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Редакция от 28 октября 2026 года").fetchSemanticsNodes().isNotEmpty()
            }
            screenshot("document-$index")
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Написать разработчику"))
            compose.onNodeWithText("Написать разработчику").assertIsDisplayed()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.onNodeWithText("О приложении").assertIsDisplayed()
        }
    }

    @Composable
    private fun Window(content: @Composable () -> Unit) {
        val configuration = Configuration(LocalConfiguration.current).apply {
            screenWidthDp = width
            screenHeightDp = height
            smallestScreenWidthDp = minOf(width, height)
            setLocale(Locale.forLanguageTag("ru"))
            orientation = if (width > height) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        val referenceContext = remember { context.createConfigurationContext(configuration) }
        CompositionLocalProvider(LocalConfiguration provides configuration, LocalContext provides referenceContext,
            LocalResources provides referenceContext.resources, LocalDensity provides Density(1f, fontScale)) {
            WolfWisdomTheme { Box(Modifier.requiredSize(width.dp, height.dp).testTag("about-surface")) { content() } }
        }
    }

    private fun screenshot(name: String) {
        val image = compose.onNodeWithTag("about-surface").captureToImage().asAndroidBitmap()
        val directory = File(context.cacheDir, "about-qa").apply { mkdirs() }
        File(directory, "$name-$width-$height-$fontScale.png").outputStream().use {
            assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}x{1} font {2}")
        fun sizes() = listOf(arrayOf<Any>(390, 844, 1f), arrayOf<Any>(844, 390, 1f), arrayOf<Any>(834, 1112, 1f),
            arrayOf<Any>(1112, 834, 1f), arrayOf<Any>(390, 844, 2f), arrayOf<Any>(640, 320, 2f))
    }
}
