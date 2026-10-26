package ru.missclick.wolfwisdom.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.WolfErrorSnackbar
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@RunWith(AndroidJUnit4::class)
class WolfErrorSnackbarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun messagesWrapWithoutClippingAtNarrowWidthsAndLargeFontSizes() {
        var width by mutableStateOf(350)
        var fontScale by mutableStateOf(1f)
        var customMessage by mutableStateOf<String?>(null)
        var renderedHeadline = ""
        var renderedDetail = ""
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                WolfWisdomTheme {
                    renderedHeadline = customMessage ?: stringResource(R.string.saved_remove_error)
                    renderedDetail = stringResource(R.string.error_snackbar_detail)
                    WolfErrorSnackbar(
                        headline = renderedHeadline,
                        detail = renderedDetail,
                        modifier = Modifier.width(width.dp).testTag("error")
                    )
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "snackbar-qa").apply { mkdirs() }
        for ((targetWidth, scale) in listOf(350 to 1f, 480 to 1f, 280 to 2f)) {
            compose.runOnIdle {
                width = targetWidth
                fontScale = scale
                customMessage = if (scale > 1f) {
                    "A caller-supplied message that needs several lines on a narrow screen"
                } else null
            }
            compose.waitForIdle()
            val surface = compose.onNodeWithTag("error")
            val bounds = surface.fetchSemanticsNode().boundsInRoot
            assertEquals(targetWidth.toFloat(), bounds.width, 1f)
            if (scale == 1f) assertEquals(72f, bounds.height, 1f)
            for (text in listOf(renderedHeadline, renderedDetail)) {
                val textBounds = compose.onNodeWithText(text, useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                assertTrue("Text must fit horizontally", textBounds.left >= bounds.left && textBounds.right <= bounds.right)
                assertTrue("Text must fit vertically", textBounds.top >= bounds.top && textBounds.bottom <= bounds.bottom)
            }
            File(directory, "error-$targetWidth-$scale.png").outputStream().use {
                surface.captureToImage().asAndroidBitmap()
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
