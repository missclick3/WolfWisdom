package ru.missclick.wolfwisdom.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.presentation.main.MainAction
import ru.missclick.wolfwisdom.presentation.main.MainScreen
import ru.missclick.wolfwisdom.presentation.main.MainState
import ru.missclick.wolfwisdom.presentation.saved.SavedAction
import ru.missclick.wolfwisdom.presentation.saved.SavedScreen
import ru.missclick.wolfwisdom.presentation.saved.SavedState
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@RunWith(Parameterized::class)
class AdaptiveScreensTest(private val width: Int, private val height: Int) {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val referenceResources = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
    ).resources
    private val quotes = listOf(
        Quote(1, "Волк не тот, кто волк, а тот, кто волк.", true),
        Quote(2, "Тишина — это тоже ответ.", true),
        Quote(3, "Не бойся идти один. Бойся жить не своей жизнью.", true),
        Quote(4, "Сила — в спокойствии, а не в шуме.", true)
    )

    @Test
    fun mainActionsRemainAccessibleInEveryLayout() {
        var saves = 0
        var shares = 0
        compose.setContent {
            ReferenceWindow {
                MainScreen(
                    state = MainState(quote = quotes.first().quote),
                    onAction = { if (it == MainAction.ToggleQuoteSaved) saves++ },
                    onShareClick = { shares++ }
                )
            }
        }
        screenshot("main")
        compose.onNodeWithContentDescription(referenceResources.getString(R.string.save_quote)).performClick()
        compose.onNodeWithContentDescription(referenceResources.getString(R.string.share)).performClick()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(1, shares)
        }
    }

    @Test
    fun draggingFromTheCardFlipsInBothDirections() {
        var backwards by mutableStateOf(false)
        val actions = mutableListOf<MainAction>()
        compose.setContent {
            ReferenceWindow {
                key(backwards) {
                    MainScreen(
                        state = MainState(quote = quotes.first().quote, canGoBack = true),
                        onAction = { actions += it }
                    )
                }
            }
        }
        for (isBackwards in listOf(false, true)) {
            compose.runOnIdle { backwards = isBackwards }
            val screen = compose.onNodeWithTag("screen")
            val screenBounds = screen.fetchSemanticsNode().boundsInRoot
            val cardBounds = compose.onNode(
                SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)
            ).fetchSemanticsNode().boundsInRoot
            val start = cardBounds.center - screenBounds.topLeft
            val distance = minOf(240f, if (isBackwards) height - start.y - 8f else start.y - 8f)
            screen.performTouchInput {
                swipe(
                    start = start,
                    end = start + Offset(0f, if (isBackwards) distance else -distance),
                    durationMillis = 600
                )
            }
            compose.runOnIdle {
                assertEquals(
                    if (isBackwards) listOf(MainAction.SwipeUp, MainAction.SwipeDown)
                    else listOf(MainAction.SwipeUp),
                    actions
                )
            }
        }
    }

    @Test
    fun savedQuotesUseOnePortraitPhoneColumnAndTwoColumnsOtherwise() {
        var removedId: Long? = null
        compose.setContent {
            ReferenceWindow {
                SavedScreen(
                    state = SavedState(quotes = quotes, isLoading = false),
                    onAction = { if (it is SavedAction.RemoveQuote) removedId = it.quoteId }
                )
            }
        }
        val first = compose.onNodeWithText(quotes[0].quote).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText(quotes[1].quote).fetchSemanticsNode().boundsInRoot
        if (minOf(width, height) < 600 && width <= height) {
            assertEquals(first.left, second.left, 1f)
            assertTrue(second.top > first.top)
        } else {
            assertTrue(second.left > first.right)
        }
        screenshot("saved")
        compose.onAllNodesWithContentDescription(referenceResources.getString(R.string.remove_quote_from_saved))
            .onFirst().performClick()
        compose.runOnIdle { assertEquals(1L, removedId) }
    }

    @Composable
    private fun ReferenceWindow(content: @Composable () -> Unit) {
        val configuration = Configuration(LocalConfiguration.current).apply {
            screenWidthDp = width
            screenHeightDp = height
            smallestScreenWidthDp = minOf(width, height)
            setLocale(Locale.forLanguageTag("ru"))
            orientation = if (width > height) Configuration.ORIENTATION_LANDSCAPE
                else Configuration.ORIENTATION_PORTRAIT
        }
        val referenceContext = remember(width, height) { context.createConfigurationContext(configuration) }
        CompositionLocalProvider(
            LocalConfiguration provides configuration,
            LocalContext provides referenceContext,
            LocalResources provides referenceContext.resources,
            LocalDensity provides Density(1f)
        ) {
            WolfWisdomTheme {
                Box(Modifier.requiredSize(width.dp, height.dp).testTag("screen")) { content() }
            }
        }
    }

    private fun screenshot(screen: String) {
        val bitmap = compose.onNodeWithTag("screen").captureToImage().asAndroidBitmap()
        val directory = File(context.cacheDir, "layout-qa").apply { mkdirs() }
        File(directory, "$screen-$width-$height.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}")
        fun sizes() = listOf(arrayOf(390, 844), arrayOf(844, 390), arrayOf(834, 1112), arrayOf(1112, 834))
    }
}
