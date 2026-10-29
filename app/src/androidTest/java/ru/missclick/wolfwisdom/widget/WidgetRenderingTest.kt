package ru.missclick.wolfwisdom.widget

import android.content.Intent
import android.graphics.Color
import android.os.Parcel
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.R

@RunWith(AndroidJUnit4::class)
class WidgetRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val quote = WidgetQuote(42, "Не бойся идти медленно. Бойся стоять на месте.", "— Волк", true)

    @After fun clean() {
        context.getSharedPreferences("widget_debug", 0).edit().clear().commit()
        WidgetRenderer.clearCache()
    }

    @Test fun preview_inAppCompatActivity_usesFrameworkViewsForImagesAndFallback() {
        val intent = Intent(context, WidgetConfigurationActivity::class.java)
            .putExtra("request_pin", true)
        ActivityScenario.launch<WidgetConfigurationActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                for (theme in WidgetTheme.entries) {
                    val state = WidgetState(42, theme)
                    val views = WidgetRenderer.render(activity, -1, state, quote,
                        WidgetDimensions(240f, 240f), WidgetRenderer.budget(activity))
                    val root = views.apply(activity, FrameLayout(activity))
                    val image = root.findViewById<ImageView>(R.id.widget_text_image)
                    assertEquals(ImageView::class.java, image.javaClass)
                    assertEquals(View.VISIBLE, image.visibility)
                    assertNotNull(image.drawable)

                    val fallback = WidgetRenderer.fallback(activity, -1, state, quote, "preview-test")
                        .apply(activity, FrameLayout(activity))
                    val text = fallback.findViewById<TextView>(R.id.widget_quote)
                    assertEquals(TextView::class.java, text.javaClass)
                    assertEquals(quote.text, text.text.toString())
                    assertEquals(View.VISIBLE, fallback.findViewById<View>(R.id.widget_native).visibility)
                }
            }
        }
    }

    @Test fun textFitting_selectsLargestCompleteOptionAndPreservesAuthor() {
        val fitter = WidgetText(context)
        val density = context.resources.displayMetrics.density
        for (width in listOf(128, 216, 304)) {
            val short = fitter.fit("Иди вперёд.", "— Волк", (width * density).toInt(), (128 * density).toInt(), Color.WHITE, Color.GRAY)!!
            assertEquals(20, short.sizeSp)
            assertFalse(short.truncated)
            val long = fitter.fit("Мудрость приходит с опытом. ".repeat(50), "— Волк", (width * density).toInt(), (128 * density).toInt(), Color.WHITE, Color.GRAY)!!
            assertEquals(16, long.sizeSp)
            assertTrue(long.truncated)
            assertTrue(long.quote.text.toString().endsWith("…"))
            assertEquals("— Волк", long.author.text.toString())
            assertTrue(long.height <= (128 * density).toInt())
        }
        assertNull(fitter.fit("Текст", "— Волк", 100, 1, Color.WHITE, Color.GRAY))
    }

    @Test fun textFitting_selects18And16OnlyWhenCompleteTextNeedsThem() {
        val fitter = WidgetText(context)
        val density = context.resources.displayMetrics.density
        val selected = (1..100).mapNotNull { n ->
            fitter.fit("Сила в пути. ".repeat(n), "— Волк", (216 * density).toInt(), (128 * density).toInt(), Color.WHITE, Color.GRAY)
        }.filterNot { it.truncated }.map { it.sizeSp }.toSet()
        assertTrue("18sp option must be reachable: $selected", 18 in selected)
        assertTrue("16sp option must be reachable: $selected", 16 in selected)
    }

    @Test fun graphemes_andExplicitNewlinesStayWithinBounds() {
        val tokens = listOf("👨‍👩‍👧‍👦", "е\u0301", "👍🏽", "🇷🇺")
        tokens.forEach { assertEquals(listOf(0, it.length), WidgetText.graphemeCuts(it)) }
        val input = (tokens.joinToString("") + "Сверхдлинноесловобезпробелов\n").repeat(100)
        val fit = WidgetText(context).fit(input, "— Волк", 240, 300, Color.WHITE, Color.GRAY)!!
        val shown = fit.quote.text.toString().removeSuffix("…")
        assertTrue(shown.length in WidgetText.graphemeCuts(input))
        assertTrue(fit.height <= 300)
        assertEquals(input, (tokens.joinToString("") + "Сверхдлинноесловобезпробелов\n").repeat(100))
    }

    @Test fun increasedFontScale_prioritizesVisibleAttribution() {
        val config = android.content.res.Configuration(context.resources.configuration).apply { fontScale = 2f }
        val scaled = context.createConfigurationContext(config)
        val density = scaled.resources.displayMetrics.density
        val fit = WidgetText(scaled).fit("Длинная цитата. ".repeat(100), "— Волк", (216 * density).toInt(), (128 * density).toInt(), Color.WHITE, Color.GRAY)!!
        assertTrue(fit.truncated)
        assertTrue(fit.height <= (128 * density).toInt())
        assertEquals("— Волк", fit.author.text.toString())
        val bitmap = WidgetText(scaled).bitmap(fit, (216 * density).toInt())
        assertTrue(bitmap.hasAlpha())
        assertEquals(android.graphics.Bitmap.Config.ARGB_8888, bitmap.config)
        bitmap.recycle()
    }

    @Test fun everySizeAndTheme_canInflateAndRepeatedResizesBoundCache() {
        for (theme in WidgetTheme.entries) for (width in listOf(160f, 248f, 336f)) {
            val views = WidgetRenderer.render(context, 42, WidgetState(42, theme), quote,
                WidgetDimensions(width, 160f), WidgetRenderer.budget(context))
            instrumentation.runOnMainSync {
                val root = views.apply(context, FrameLayout(context))
                assertTrue(root.contentDescription.contains(quote.text))
                assertEquals(View.VISIBLE, root.findViewById<ImageView>(R.id.widget_text_image).visibility)
                assertEquals(View.GONE, root.findViewById<View>(R.id.widget_native).visibility)
                val w = WidgetBudget.pixels(width, context.resources.displayMetrics.density)
                val h = WidgetBudget.pixels(160f, context.resources.displayMetrics.density)
                root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, w, h)
                val screenshot = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                root.draw(android.graphics.Canvas(screenshot))
                val directory = java.io.File(context.filesDir, "widget-verification").apply { mkdirs() }
                java.io.File(directory, "${width.toInt()}-${theme.name}.png").outputStream().use {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                screenshot.recycle()
            }
        }
        repeat(45) { n -> WidgetRenderer.render(context, 42, WidgetState(42), quote, WidgetDimensions(160f + n * 5f, 160f), 2_000_000) }
        assertTrue(WidgetRenderer.cacheBytes() <= 4 * 1024 * 1024)
    }

    @Test fun failuresAndReducedBudget_produceWorkingZeroBitmapFallback() {
        for (failImages in listOf(false, true)) {
            context.getSharedPreferences("widget_debug", 0).edit().putBoolean("fail_images", failImages).commit()
            val views = WidgetRenderer.render(context, 42, WidgetState(42), quote, WidgetDimensions(336f, 160f), if (failImages) 2_000_000 else 0)
            // Parcel is small because no Bitmap payload is attached, regardless of the quote's length.
            val parcel = Parcel.obtain()
            try { views.writeToParcel(parcel, 0); assertTrue(parcel.dataSize() < 16_000) } finally { parcel.recycle() }
            instrumentation.runOnMainSync {
                val root = views.apply(context, FrameLayout(context))
                assertEquals(quote.text, root.findViewById<TextView>(R.id.widget_quote).text.toString())
                assertEquals(quote.author, root.findViewById<TextView>(R.id.widget_author).text.toString())
                assertNull(root.findViewById<ImageView>(R.id.widget_text_image).drawable)
                assertNull(root.findViewById<ImageView>(R.id.widget_background).drawable)
                assertTrue(root.hasOnClickListeners())
            }
        }
    }

    @Test fun landscape_paletteChangesRecolorDetailsWithoutReusingStaleCache() {
        val base = WidgetRenderer.palette(context, WidgetTheme.LIGHT)
        val sage = base.copy(shadow = 0xFF14271B.toInt(), midtone = 0xFF638268.toInt(), highlight = 0xFFEDF5E5.toInt())
        val lilac = base.copy(shadow = 0xFF241433.toInt(), midtone = 0xFF88659F.toInt(), highlight = 0xFFF5E9FF.toInt())
        val green = WidgetRenderer.background(context, WidgetTheme.ANDROID, sage, 672, 320, false)
        val purple = WidgetRenderer.background(context, WidgetTheme.ANDROID, lilac, 672, 320, false)
        assertSame(green, WidgetRenderer.background(context, WidgetTheme.ANDROID, sage, 672, 320, false))
        assertNotSame(green, purple)
        val x = 600
        val y = 240
        assertTrue(Color.green(green.getPixel(x, y)) > Color.red(green.getPixel(x, y)))
        assertTrue(Color.blue(purple.getPixel(x, y)) > Color.green(purple.getPixel(x, y)))
        assertNotEquals(green.getPixel(x, y), green.getPixel(x, 60))
        val night = sage.copy(light = false, surface = 0xFF08120C.toInt(),
            shadow = 0xFF020804.toInt(), midtone = 0xFF203E2A.toInt(), highlight = 0xFFA0C4A7.toInt())
        val dark = WidgetRenderer.background(context, WidgetTheme.ANDROID, night, 672, 320, false)
        assertTrue(Color.green(dark.getPixel(x, 60)) < Color.green(green.getPixel(x, 60)))
        val directory = java.io.File(context.filesDir, "widget-verification").apply { mkdirs() }
        listOf("sage" to green, "lilac" to purple, "sage-dark" to dark).forEach { (name, bitmap) ->
            java.io.File(directory, "palette-$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
