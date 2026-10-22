package ru.missclick.wolfwisdom.presentation.sharing

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@RunWith(AndroidJUnit4::class)
class QuoteSharingTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun exportsEveryLandscapeAndKeepsFullLongQuote() {
        var scene by mutableIntStateOf(0)
        var quote by mutableStateOf("Мечтай масштабно. Делай по силам. Спи регулярно.")
        val captured = AtomicReference<Result<ImageBitmap>>()
        compose.setContent {
            WolfWisdomTheme {
                QuoteCardCapture(quote, scene) { captured.set(it) }
            }
        }
        val colors = mutableSetOf<Int>()
        for (index in 0..3) {
            if (index > 0) compose.runOnIdle {
                captured.set(null)
                scene = index
            }
            compose.waitUntil(10_000) { captured.get() != null }
            val bitmap = captured.get().getOrThrow().asAndroidBitmap()
                .copy(Bitmap.Config.ARGB_8888, false)
            assertEquals(1080, bitmap.width)
            assertTrue(bitmap.height > 440)
            assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
            assertEquals(255, Color.alpha(bitmap.getPixel(200, 200)))
            colors += bitmap.getPixel(150, 220)
        }
        assertEquals("Each scene must use different artwork", 4, colors.size)
        compose.runOnIdle {
            captured.set(null)
            quote = "Сохраняй спокойствие и иди вперёд. ".repeat(30)
        }
        compose.waitUntil(10_000) { captured.get() != null }
        assertTrue(
            "Long quotes must not be cropped to the screen height",
            captured.get().getOrThrow().height > context.resources.displayMetrics.heightPixels
        )
    }

    @Test
    fun sharesReadablePngWithTemporaryReadPermission() {
        val shared = AtomicReference<Intent>()
        compose.setContent {
            WolfWisdomTheme {
                QuoteCardCapture("Иди вперёд.", 2) { result ->
                    shared.set(createQuoteShareIntent(context, result.getOrThrow()))
                }
            }
        }
        compose.waitUntil(10_000) { shared.get() != null }
        val intent = shared.get()
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)!!
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.quote_shares", uri.authority)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        assertNotEquals(0, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertEquals("image/png", context.contentResolver.getType(uri))
        context.contentResolver.openInputStream(uri)!!.use {
            val bitmap = BitmapFactory.decodeStream(it)
            assertEquals(1080, bitmap.width)
            assertTrue(bitmap.height > 440)
        }
        context.contentResolver.delete(uri, null, null)
    }
}
