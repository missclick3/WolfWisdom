package ru.missclick.wolfwisdom.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.presentation.saved.SavedAction
import ru.missclick.wolfwisdom.presentation.saved.SavedScreen
import ru.missclick.wolfwisdom.presentation.saved.SavedState
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@RunWith(Parameterized::class)
class SavedHeaderScrollTest(private val width: Int, private val height: Int, private val fontScale: Float) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun collapsesOnlyNearTopAndSurvivesRestorationAndResize() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val title = resources.getString(R.string.saved)
        val back = resources.getString(R.string.navigate_back)
        val search = resources.getString(R.string.open_saved_search)
        var windowWidth by mutableStateOf(width)
        var windowHeight by mutableStateOf(height)
        var state by mutableStateOf(SavedState(
            quotes = List(50) { Quote(it.toLong(), "Quote $it", true) },
            isLoading = false
        ))
        var backClicks = 0
        var density = 1f
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = windowWidth
                screenHeightDp = windowHeight
            }
            // Fit each virtual window on the emulator while exercising its actual
            // dp constraints, including card widths and row count.
            val hostSize = LocalWindowInfo.current.containerSize
            density = minOf(
                LocalDensity.current.density,
                hostSize.width.toFloat() / windowWidth,
                hostSize.height.toFloat() / windowHeight
            )
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(density, fontScale)
            ) {
                WolfWisdomTheme {
                    Box(Modifier.requiredSize(windowWidth.dp, windowHeight.dp)) {
                        SavedScreen(
                            state = state,
                            onBackClick = { backClicks++ },
                            onAction = { action ->
                                state = when (action) {
                                    SavedAction.OpenSearch -> state.copy(isSearchVisible = true)
                                    SavedAction.CloseSearch -> state.copy(isSearchVisible = false)
                                    else -> state
                                }
                            }
                        )
                    }
                }
            }
        }
        fun titleY() = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.center.y
        val expandedY = titleY()
        val firstCardY = compose.onNodeWithText("Quote 0").fetchSemanticsNode().boundsInRoot.top
        // Exercise real grid scrolling, including a partial first-row offset.
        val halfDistance = (height * 0.12f).coerceIn(
            if (width > height) 40f else 64f,
            if (width > height) 80f else 100f
        ) * density / 2
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, halfDistance)
        }
        val partialY = titleY()
        compose.onNode(hasScrollAction()).performScrollToIndex(20)
        val collapsedY = titleY()
        assertTrue(partialY < expandedY)
        assertTrue(partialY > collapsedY)
        compose.onNode(hasScrollAction()).performScrollToIndex(10)
        assertEquals(collapsedY, titleY(), 1f)
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(collapsedY, titleY(), 1f)
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        assertEquals(expandedY, titleY(), 1f)
        assertEquals(firstCardY, compose.onNodeWithText("Quote 0").fetchSemanticsNode().boundsInRoot.top, 1f)

        compose.onNode(hasScrollAction()).performScrollToIndex(20)
        compose.runOnIdle {
            windowWidth = height
            windowHeight = width
        }
        val resizedCollapsedY = titleY()
        compose.onNode(hasScrollAction()).performScrollToIndex(10)
        assertEquals(resizedCollapsedY, titleY(), 1f)
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        assertTrue(titleY() > resizedCollapsedY)

        compose.onNodeWithContentDescription(back).performClick()
        compose.runOnIdle { assertEquals(1, backClicks) }
        compose.onNodeWithContentDescription(search).performClick()
        compose.runOnIdle { assertTrue(state.isSearchVisible) }
        compose.onNodeWithContentDescription(resources.getString(R.string.close_saved_search)).performClick()
        compose.runOnIdle { assertTrue(!state.isSearchVisible) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}, font={2}")
        fun windows() = listOf(
            arrayOf<Any>(390, 844, 1f),
            arrayOf<Any>(844, 390, 1f),
            arrayOf<Any>(834, 1112, 1f),
            arrayOf<Any>(1112, 834, 1f),
            arrayOf<Any>(390, 844, 2f)
        )
    }
}
