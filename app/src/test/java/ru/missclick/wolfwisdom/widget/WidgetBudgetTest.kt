package ru.missclick.wolfwisdom.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetBudgetTest {
    @Test fun displayCeiling_doesNotOverflowIntOnLargeDisplays() {
        assertEquals(2_400_000_000L, WidgetBudget.ceiling(20_000, 20_000))
        assertEquals(4L * 1024 * 1024, WidgetBudget.internal(20_000, 20_000))
    }

    @Test fun pixels_usesDensityAndRoundsUp() {
        assertEquals(420, WidgetBudget.pixels(160f, 2.625f))
        assertEquals(281, WidgetBudget.pixels(160f, 1.751f))
    }

    @Test fun decoration_reducesResolutionBeforeRemovingImagery() {
        val (w, h) = WidgetBudget.decorationSize(1200, 600, 720_000)!!
        assertEquals(600, w)
        assertEquals(300, h)
        assertTrue(w.toLong() * h * 4 <= 720_000)
        assertNull(WidgetBudget.decorationSize(1200, 600, 1024))
        assertEquals(1200 to 600, WidgetBudget.decorationSize(1200, 600, 4_000_000))
    }

    @Test fun internalBudget_leavesHeadroomOnSmallDisplays() {
        assertEquals(240_000L, WidgetBudget.ceiling(200, 200))
        assertEquals(60_000L, WidgetBudget.internal(200, 200))
    }
}
