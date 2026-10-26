package ru.missclick.wolfwisdom.widget

import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

object WidgetBudget {
    fun ceiling(widthPx: Int, heightPx: Int): Long = 6L * widthPx.coerceAtLeast(1) * heightPx.coerceAtLeast(1)
    // One update is capped at 1/4 of the display ceiling and 4 MiB, including every variant.
    fun internal(widthPx: Int, heightPx: Int): Long = min(4L * 1024 * 1024, ceiling(widthPx, heightPx) / 4)
    fun pixels(dp: Float, density: Float): Int = ceil(dp.coerceAtLeast(1f) * density).toInt().coerceIn(1, 4096)
    fun decorationSize(width: Int, height: Int, bytes: Long): Pair<Int, Int>? {
        if (bytes < 2048) return null
        val scale = min(1.0, sqrt(bytes.toDouble() / (width.toLong() * height * 4L)))
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}
