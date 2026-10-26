package ru.missclick.wolfwisdom.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import android.icu.text.BreakIterator
import ru.missclick.wolfwisdom.R
import java.util.Locale

data class FittedWidgetText(val quote: StaticLayout, val author: StaticLayout, val sizeSp: Int,
                            val truncated: Boolean, val extended: Boolean, val gap: Int) {
    val height: Int get() = quote.height + gap + author.height
}

/** Measurement and rasterization share the very same StaticLayouts, including font fallback. */
class WidgetText(private val context: Context) {
    private val density = context.resources.displayMetrics.density
    private fun sp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value.toFloat(), context.resources.displayMetrics)
    private val serif = ResourcesCompat.getFont(context, R.font.tinos_regular) ?: Typeface.SERIF
    private val sans = ResourcesCompat.getFont(context, R.font.inter_variable) ?: Typeface.SANS_SERIF

    // API 37 annotates this parameter with LineBreaker constants (introduced in 29).
    // Layout's public API 23 constant has the identical value and supports our API 26 floor.
    @android.annotation.SuppressLint("WrongConstant")
    private fun layout(text: String, width: Int, size: Int, line: Int, font: Typeface, color: Int): StaticLayout {
        val paint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(size); typeface = font; this.color = color
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
            .setLineSpacing((sp(line) - (paint.fontMetrics.descent - paint.fontMetrics.ascent)).coerceAtLeast(0f), 1f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).build()
    }

    fun fit(text: String, author: String, width: Int, height: Int, color: Int, authorColor: Int): FittedWidgetText? {
        val authorPaint = TextPaint().apply { textSize = sp(12); typeface = sans }
        val authorText = TextUtils.ellipsize(author, authorPaint, width.toFloat(), TextUtils.TruncateAt.END).toString()
        val attribution = layout(authorText, width, 12, 16, sans, authorColor)
        val gap = if (author.isEmpty()) 0 else (8 * density).toInt()
        val available = height - attribution.height - gap
        if (available <= 0 || width <= 0) return null
        val options = listOf(Triple(20, 24, 4), Triple(18, 22, 4), Triple(16, 20, 5))
        fun fits(l: StaticLayout, max: Int) = l.height <= available && l.lineCount <= max
        options.forEachIndexed { index, (size, line, max) ->
            val measured = layout(text, width, size, line, serif, color)
            if (fits(measured, max)) return FittedWidgetText(measured, attribution, size, false, index > 0, gap)
        }
        val cuts = graphemeCuts(text)
        var low = 0
        var high = cuts.lastIndex
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (fits(layout(text.substring(0, cuts[mid]).trimEnd() + "…", width, 16, 20, serif, color), 5)) low = mid
            else high = mid - 1
        }
        var end = cuts[low]
        val prefix = text.substring(0, end)
        val word = prefix.indexOfLast { it.isWhitespace() }
        // Prefer a word boundary without discarding most of a line for an unbroken token.
        if (word > end * 3 / 4 && word in cuts) end = word
        val measured = layout(text.substring(0, end).trimEnd() + "…", width, 16, 20, serif, color)
        if (!fits(measured, 5)) return null
        return FittedWidgetText(measured, attribution, 16, true, true, gap)
    }

    fun bitmap(fit: FittedWidgetText, width: Int): Bitmap = Bitmap.createBitmap(width, fit.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it)
        fit.quote.draw(canvas)
        canvas.translate(0f, (fit.quote.height + fit.gap).toFloat())
        fit.author.draw(canvas)
    }

    companion object {
        fun graphemeCuts(text: String): List<Int> {
            val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
            val cuts = mutableListOf(0)
            var next = iterator.next()
            while (next != BreakIterator.DONE) {
                // Older ICU versions can expose boundaries inside newer emoji ZWJ sequences.
                val before = text.codePointBefore(next)
                val after = if (next < text.length) text.codePointAt(next) else -1
                if (before != 0x200D && after != 0x200D && after !in 0x1F3FB..0x1F3FF &&
                    after != 0xFE0F && after != 0xFE0E) cuts.add(next)
                next = iterator.next()
            }
            if (cuts.last() != text.length) cuts.add(text.length)
            return cuts
        }
    }
}
