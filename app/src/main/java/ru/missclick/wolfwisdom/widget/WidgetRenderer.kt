package ru.missclick.wolfwisdom.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import ru.missclick.wolfwisdom.logging.AppLog
import android.util.LruCache
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CancellationException
import ru.missclick.wolfwisdom.BuildConfig
import ru.missclick.wolfwisdom.MainActivity
import ru.missclick.wolfwisdom.R
import kotlin.math.max
import kotlin.math.roundToInt

data class WidgetDimensions(val width: Float, val height: Float)
data class WidgetPalette(
    val surface: Int, val text: Int, val author: Int, val light: Boolean,
    val shadow: Int = surface, val midtone: Int = surface, val highlight: Int = surface
)

object WidgetRenderer {
    private const val TAG = "FavoriteWidget"
    private const val CACHE_BYTES = 4 * 1024 * 1024
    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    fun cacheBytes(): Int = cache.size()
    fun clearCache() = cache.evictAll()

    fun palette(context: Context, theme: WidgetTheme): WidgetPalette {
        val light = theme == WidgetTheme.LIGHT || (theme == WidgetTheme.ANDROID &&
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES)
        if (theme == WidgetTheme.ANDROID && Build.VERSION.SDK_INT >= 31) {
            val colors = if (light) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
            return WidgetPalette(colors.surface.toArgb(), colors.onSurface.toArgb(), colors.onSurfaceVariant.toArgb(), light,
                context.getColor(if (light) android.R.color.system_accent1_900 else android.R.color.system_accent1_1000),
                context.getColor(if (light) android.R.color.system_accent1_500 else android.R.color.system_accent1_800),
                context.getColor(if (light) android.R.color.system_accent1_50 else android.R.color.system_accent1_200))
        }
        return if (light) WidgetPalette(0xFFEDF3F9.toInt(), 0xFF0B1927.toInt(), 0xFF314859.toInt(), true,
            0xFF142E43.toInt(), 0xFF6A89A5.toInt(), 0xFFEDF3F9.toInt())
        else WidgetPalette(0xFF071522.toInt(), 0xFFF0F5FF.toInt(), 0xFFA7BDD3.toInt(), false,
            0xFF020A12.toInt(), 0xFF173850.toInt(), 0xFF9ABBD6.toInt())
    }

    fun budget(context: Context): Long {
        val bounds = if (Build.VERSION.SDK_INT >= 30) context.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        else context.resources.displayMetrics.let { Rect(0, 0, it.widthPixels, it.heightPixels) }
        val normal = WidgetBudget.internal(bounds.width(), bounds.height())
        return if (BuildConfig.DEBUG) context.getSharedPreferences("widget_debug", 0)
            .getLong("budget_bytes", normal).coerceIn(0, normal) else normal
    }

    fun openAppIntent(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        context, id, Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            // Reset a previously opened secondary destination to the main screen as well.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun fallback(context: Context, id: Int, state: WidgetState, quote: WidgetQuote, reason: String): RemoteViews {
        AppLog.w(TAG, "id=$id fallback=$reason bitmaps=0")
        val p = palette(context, state.theme)
        return base(context, id, quote, p).apply {
            setViewVisibility(R.id.widget_text_image, View.GONE)
            setViewVisibility(R.id.widget_native, View.VISIBLE)
        }
    }

    private fun base(context: Context, id: Int, quote: WidgetQuote, p: WidgetPalette) =
        RemoteViews(context.packageName, R.layout.favorite_widget).apply {
            setInt(R.id.widget_root, "setBackgroundResource", if (p.light) R.drawable.widget_light else R.drawable.widget_dark)
            setTextViewText(R.id.widget_quote, quote.text)
            setTextViewText(R.id.widget_author, quote.author)
            setTextColor(R.id.widget_quote, p.text)
            setTextColor(R.id.widget_author, p.author)
            setContentDescription(R.id.widget_root, listOf(quote.text, quote.author).filter(String::isNotBlank).joinToString(". "))
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, id))
        }

    /** Called on Dispatchers.IO. Every bitmap is allocated only after reserving its worst-case bytes. */
    fun render(context: Context, id: Int, state: WidgetState, quote: WidgetQuote, size: WidgetDimensions, budget: Long): RemoteViews {
        if (!quote.hasQuote) return fallback(context, id, state, quote, "empty-or-unavailable")
        val density = context.resources.displayMetrics.density
        val w = WidgetBudget.pixels(size.width, density)
        val h = WidgetBudget.pixels(size.height, density)
        val pad = (16 * density).roundToInt()
        val p = palette(context, state.theme)
        try {
            if (BuildConfig.DEBUG && context.getSharedPreferences("widget_debug", 0).getBoolean("fail_images", false)) {
                return fallback(context, id, state, quote, "debug-image-failure")
            }
            val fit = WidgetText(context).fit(quote.text, quote.author, w - pad * 2, h - pad * 2, p.text, p.author)
                ?: return fallback(context, id, state, quote, "text-area")
            val textBytes = (w - pad * 2).toLong() * fit.height * 4L
            if (textBytes > budget) return fallback(context, id, state, quote, "text-budget")
            val text = WidgetText(context).bitmap(fit, w - pad * 2)
            val remaining = budget - text.allocationByteCount
            val backgroundSize = WidgetBudget.decorationSize(w, h, remaining)
            val background = backgroundSize?.let { (bw, bh) -> background(context, state.theme, p, bw, bh, fit.extended) }
            val used = text.allocationByteCount.toLong() + (background?.allocationByteCount ?: 0)
            if (used > budget) return fallback(context, id, state, quote, "actual-allocation")
            AppLog.d(TAG, "id=$id dp=${size.width}x${size.height} px=${w}x$h text=${text.config}:${text.allocationByteCount} background=${background?.width}x${background?.height}:${background?.config}:${background?.allocationByteCount} bytes=$used budget=$budget cache=${cache.size()}")
            return base(context, id, quote, p).apply {
                setViewVisibility(R.id.widget_native, View.GONE)
                setViewVisibility(R.id.widget_text_image, View.VISIBLE)
                setImageViewBitmap(R.id.widget_text_image, text)
                if (background != null) {
                    setViewVisibility(R.id.widget_background, View.VISIBLE)
                    setImageViewBitmap(R.id.widget_background, background)
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: android.content.res.Resources.NotFoundException) {
            return fallback(context, id, state, quote, "resource-${error.javaClass.simpleName}")
        } catch (error: IllegalArgumentException) {
            return fallback(context, id, state, quote, "render-${error.javaClass.simpleName}")
        } catch (_: OutOfMemoryError) {
            clearCache()
            return fallback(context, id, state, quote, "allocation")
        }
    }

    internal fun background(context: Context, theme: WidgetTheme, p: WidgetPalette, w: Int, h: Int, extended: Boolean): Bitmap {
        // Android recolors one master; fixed themes retain their supplied artwork.
        val resource = if (theme == WidgetTheme.DARK) R.drawable.widget_landscape_dark else R.drawable.widget_landscape_light
        val key = "$resource:4:$theme:$w:$h:${context.resources.displayMetrics.densityDpi}:$p:$extended"
        cache.get(key)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resource, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid landscape" }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565; inScaled = false }
        val source = requireNotNull(BitmapFactory.decodeResource(context.resources, resource, options)) { "Landscape decode failed" }
        AppLog.d(TAG, "decode resource=$resource sample=$sample px=${source.width}x${source.height} config=${source.config} bytes=${source.allocationByteCount}")
        // Transformation has a bounded output, never a full-resolution duplicate.
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        try {
            val canvas = Canvas(output)
            val scale = max(w.toFloat() / source.width, h.toFloat() / source.height)
            val matrix = Matrix().apply {
                // Supplied artwork already has its wolf on the right.
                setScale(scale, scale)
                postTranslate(w - source.width * scale, (h - source.height * scale) / 2f)
            }
            canvas.drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
            if (theme == WidgetTheme.ANDROID) toneLandscape(output, p)
            val rgb = p.surface and 0xFFFFFF
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint(Paint.DITHER_FLAG).apply {
                shader = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(rgb or (220 shl 24), rgb or (90 shl 24), rgb), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
            })
            // A light 25% tint supports longer text while retaining the landscape.
            if (extended) canvas.drawColor(rgb or (64 shl 24))
        } finally { source.recycle() }
        if (output.allocationByteCount <= CACHE_BYTES) cache.put(key, output)
        return output
    }

    /** Preserve relief and fog by mapping luminance to three system palette tones.
     * Uses one scanline and a small LUT, never another full-size bitmap.
     */
    private fun toneLandscape(bitmap: Bitmap, p: WidgetPalette) {
        val tones = IntArray(256) { value ->
            val low = if (value < 128) p.shadow else p.midtone
            val high = if (value < 128) p.midtone else p.highlight
            val fraction = if (value < 128) value / 128f else (value - 128) / 127f
            fun channel(a: Int, b: Int) = (a + (b - a) * fraction).roundToInt()
            Color.rgb(channel(Color.red(low), Color.red(high)),
                channel(Color.green(low), Color.green(high)), channel(Color.blue(low), Color.blue(high)))
        }
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, row.size, 0, y, row.size, 1)
            for (x in row.indices) {
                val pixel = row[x]
                val luminance = (54 * Color.red(pixel) + 183 * Color.green(pixel) + 19 * Color.blue(pixel)) shr 8
                row[x] = tones[luminance]
            }
            bitmap.setPixels(row, 0, row.size, 0, y, row.size, 1)
        }
    }
}
