package ru.missclick.wolfwisdom.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetStateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun instances_persistIndependentlyAndRestoreWithoutLosingOverlappingIds() {
        val store = WidgetStateStore(context)
        val first = WidgetState(45, WidgetTheme.LIGHT)
        val second = WidgetState(61, WidgetTheme.ANDROID, allowUnsaved = true)
        try {
            assertTrue(store.write(90101, first))
            assertTrue(store.write(90102, second))
            val reopened = WidgetStateStore(context)
            assertEquals(first, reopened.read(90101))
            assertEquals(second, reopened.read(90102))
            reopened.restore(intArrayOf(90101, 90102), intArrayOf(90102, 90103))
            assertNull(reopened.read(90101))
            assertEquals(first, reopened.read(90102))
            assertEquals(second, reopened.read(90103))
            reopened.delete(90102)
            assertNull(reopened.read(90102))
            assertEquals(second, reopened.read(90103))
        } finally { (90101..90103).forEach(store::delete) }
    }

    @Test fun oldState_migratesWithoutAllowingRemovedFavorites() {
        val prefs = context.getSharedPreferences("favorite_widgets", 0)
        val store = WidgetStateStore(context)
        try {
            prefs.edit().putString("instance.90104", "1|45|LIGHT").commit()
            assertEquals(WidgetState(45, WidgetTheme.LIGHT), store.read(90104))
            store.write(90104, WidgetState(61, WidgetTheme.ANDROID, allowUnsaved = true))
            assertEquals(WidgetState(61, WidgetTheme.ANDROID, allowUnsaved = true), WidgetStateStore(context).read(90104))
        } finally { store.delete(90104) }
    }
}
