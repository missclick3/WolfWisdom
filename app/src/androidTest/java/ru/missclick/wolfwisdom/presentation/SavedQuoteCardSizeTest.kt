package ru.missclick.wolfwisdom.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.presentation.saved.components.SavedQuoteCard
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@RunWith(AndroidJUnit4::class)
class SavedQuoteCardSizeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allArtworkAndQuoteLengthsHaveEqualCardSizes() {
        var scale by mutableStateOf(1f)
        var fontScale by mutableStateOf(1f)
        val quotes = listOf(
            "Короткая цитата.",
            "Сохраняй спокойствие и иди вперёд. ".repeat(40),
            "Тишина — это тоже ответ.",
            "Если два волка спорят, третий может вообще не знать темы."
        )
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                WolfWisdomTheme {
                    val artwork = ImageBitmap.imageResource(R.drawable.saved_landscapes)
                    Column(Modifier.width((378f * scale).dp).testTag("cards")) {
                        quotes.forEachIndexed { index, text ->
                            SavedQuoteCard(
                                quote = Quote(index.toLong(), text, true),
                                artwork = artwork,
                                sceneIndex = index,
                                isRemoving = false,
                                onRemoveClick = {},
                                onShareClick = {},
                                onWidgetClick = {},
                                modifier = Modifier.testTag("card-$index"),
                                scaleToWidth = true
                            )
                        }
                    }
                }
            }
        }
        for ((cardScale, textScale) in listOf(1f to 1f, 1.36f to 1f, 1f to 1.5f)) {
            compose.runOnIdle {
                scale = cardScale
                fontScale = textScale
            }
            val first = compose.onNodeWithTag("card-0").fetchSemanticsNode().boundsInRoot
            assertEquals(176f * cardScale, first.height, 1f)
            for (index in 1..3) {
                val bounds = compose.onNodeWithTag("card-$index").fetchSemanticsNode().boundsInRoot
                assertEquals(first.width, bounds.width, 1f)
                assertEquals(first.height, bounds.height, 1f)
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val directory = File(context.cacheDir, "layout-qa").apply { mkdirs() }
            val bitmap = compose.onNodeWithTag("cards").captureToImage().asAndroidBitmap()
            File(directory, "uniform-saved-$cardScale-$textScale.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
