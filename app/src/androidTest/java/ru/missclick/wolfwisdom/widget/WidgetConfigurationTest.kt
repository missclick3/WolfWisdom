package ru.missclick.wolfwisdom.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto

@RunWith(AndroidJUnit4::class)
class WidgetConfigurationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, 9701)
    private val ids = mutableListOf<Int>()
    private val database get() = GlobalContext.get().get<WolfWisdomDatabase>()
    private val quoteId = 989898L

    @Before fun seed() = runBlocking {
        database.quoteDao().insertAll(listOf(QuoteDto(quoteId, "Это полный оригинал цитаты. ".repeat(40), true, Long.MAX_VALUE)))
    }

    @After fun clean() {
        instrumentation.runOnMainSync {
            androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).toList().forEach { it.finish() }
        }
        ids.forEach { WidgetStateStore(context).delete(it); host.deleteAppWidgetId(it) }
        database.openHelper.writableDatabase.execSQL("DELETE FROM quotes WHERE id = $quoteId")
        instrumentation.uiAutomation.dropShellPermissionIdentity()
    }

    private fun allocate(provider: Class<out FavoriteWidgetProvider> = FavoriteWidget2::class.java): Int {
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        val id = host.allocateAppWidgetId()
        ids.add(id)
        assertTrue("Test host must bind widget", manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, provider)))
        manager.updateAppWidgetOptions(id, Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 336)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160)
        })
        return id
    }

    private fun intent(id: Int) = Intent(context, WidgetConfigurationActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        .putExtra("quote_id", quoteId)

    private fun ready(scenario: ActivityScenario<WidgetConfigurationActivity>) {
        compose.waitUntil(15_000) {
            var loaded = false
            scenario.onActivity { loaded = !ViewModelProvider(it)[WidgetConfigViewModel::class.java].state.value.loading }
            loaded
        }
    }

    @Test fun invalidId_finishesCanceled() {
        ActivityScenario.launchActivityForResult<WidgetConfigurationActivity>(intent(-1)).use { scenario ->
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test fun canceledDraft_doesNotCreateInstance() {
        val id = allocate()
        ActivityScenario.launchActivityForResult<WidgetConfigurationActivity>(intent(id)).use { scenario ->
            ready(scenario)
            compose.waitUntil(10_000) { compose.onAllNodesWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_create)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_theme_light)).performScrollTo().performClick()
            scenario.onActivity { it.finish() }
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertNull(WidgetStateStore(context).read(id))
        }
    }

    @Test fun widgetTap_opensMainScreenIncludingWhenAppIsOnSavedScreen(): Unit = runBlocking {
        val id = allocate()
        WidgetStateStore(context).write(id, WidgetState(quoteId))
        assertTrue(WidgetUpdates.update(context, id))
        val launch = WidgetRenderer.openAppIntent(context, id)
        val menu = context.getString(ru.missclick.wolfwisdom.R.string.more_options)
        fun waitForMain() {
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(menu).fetchSemanticsNodes().isNotEmpty() }
        }
        instrumentation.runOnMainSync { launch.send() }
        waitForMain()
        compose.onNodeWithContentDescription(menu).performClick()
        compose.onNodeWithText(context.getString(ru.missclick.wolfwisdom.R.string.saved)).performClick()
        val savedSearch = context.getString(ru.missclick.wolfwisdom.R.string.open_saved_search)
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(savedSearch).fetchSemanticsNodes().isNotEmpty() }
        instrumentation.runOnMainSync { launch.send() }
        waitForMain()
    }

    @Test fun theme_survivesRecreationAndConfirmationReturnsSameId() {
        val id = allocate(FavoriteWidget3::class.java)
        ActivityScenario.launchActivityForResult<WidgetConfigurationActivity>(intent(id)).use { scenario ->
            ready(scenario)
            compose.waitUntil(10_000) { compose.onAllNodesWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_preview)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Android").performScrollTo().performClick()
            scenario.onActivity {
                assertEquals(WidgetTheme.ANDROID, ViewModelProvider(it)[WidgetConfigViewModel::class.java].state.value.theme)
            }
            scenario.recreate()
            ready(scenario)
            compose.waitForIdle()
            scenario.onActivity {
                assertEquals(WidgetTheme.ANDROID, ViewModelProvider(it)[WidgetConfigViewModel::class.java].state.value.theme)
            }
            compose.waitUntil(15_000) {
                var rendered = false
                scenario.onActivity {
                    rendered = it.findViewById<android.view.View>(ru.missclick.wolfwisdom.R.id.widget_root)?.width?.let { width -> width > 0 } == true
                }
                rendered
            }
            compose.waitForIdle()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            val directory = java.io.File(context.filesDir, "widget-verification").apply { mkdirs() }
            java.io.File(directory, "configuration-3-android.png").outputStream().use {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
            assertNull(WidgetStateStore(context).read(id))
            compose.onNodeWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_create)).performScrollTo().performClick()
            compose.waitUntil(15_000) { WidgetStateStore(context).read(id) != null }
            compose.waitUntil(15_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            assertEquals(id, scenario.result.resultData.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
            assertEquals(WidgetState(quoteId, WidgetTheme.ANDROID), WidgetStateStore(context).read(id))
        }
        // Reconfiguration loads the committed selection; cancel leaves it intact.
        ActivityScenario.launchActivityForResult<WidgetConfigurationActivity>(intent(id)).use { scenario ->
            ready(scenario)
            compose.waitForIdle()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_preview)).fetchSemanticsNodes().isNotEmpty() }
            scenario.onActivity {
                assertEquals(WidgetTheme.ANDROID, ViewModelProvider(it)[WidgetConfigViewModel::class.java].state.value.theme)
                it.finish()
            }
        }
        assertEquals(WidgetTheme.ANDROID, WidgetStateStore(context).read(id)?.theme)
    }

    @Test fun systemHost_receivesResizedPayloadAndKeepsOtherInstanceTheme(): Unit = runBlocking {
        val first = allocate(FavoriteWidget2::class.java)
        val second = allocate(FavoriteWidget4::class.java)
        WidgetStateStore(context).write(first, WidgetState(quoteId, WidgetTheme.DARK))
        WidgetStateStore(context).write(second, WidgetState(quoteId, WidgetTheme.LIGHT))
        assertTrue(WidgetUpdates.update(context, first))
        assertTrue(WidgetUpdates.update(context, second))
        manager.updateAppWidgetOptions(first, Bundle().apply {
            putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(android.util.SizeF(248f, 160f), android.util.SizeF(336f, 160f)))
        })
        assertTrue(WidgetUpdates.update(context, first))
        assertEquals(WidgetTheme.LIGHT, WidgetStateStore(context).read(second)?.theme)
        instrumentation.runOnMainSync {
            val hostView = host.createView(context, first, manager.getAppWidgetInfo(first))
            assertNotNull(hostView.findViewById<android.widget.ImageView>(ru.missclick.wolfwisdom.R.id.widget_text_image))
            val w = WidgetBudget.pixels(248f, context.resources.displayMetrics.density)
            val h = WidgetBudget.pixels(160f, context.resources.displayMetrics.density)
            hostView.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
            hostView.layout(0, 0, w, h)
            assertTrue(hostView.width > 0)
        }
    }

    @Test fun unsavedQuote_survivesReconfigurationWithoutAddingFavorite(): Unit = runBlocking {
        val id = allocate()
        database.quoteDao().setSaved(quoteId, false, null)
        WidgetStateStore(context).write(id, WidgetState(quoteId))
        ActivityScenario.launchActivityForResult<WidgetConfigurationActivity>(intent(id)).use { scenario ->
            ready(scenario)
            scenario.onActivity {
                val draft = ViewModelProvider(it)[WidgetConfigViewModel::class.java].state.value
                assertTrue(draft.quote!!.hasQuote)
            }
            compose.onNodeWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_theme_light)).performScrollTo().performClick()
            scenario.recreate()
            ready(scenario)
            scenario.onActivity {
                val draft = ViewModelProvider(it)[WidgetConfigViewModel::class.java].state.value
                assertEquals(WidgetTheme.LIGHT, draft.theme)
            }
            compose.onNodeWithText(context.getString(ru.missclick.wolfwisdom.R.string.widget_create)).performScrollTo().performClick()
            compose.waitUntil(15_000) { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        }
        assertEquals(WidgetState(quoteId, WidgetTheme.LIGHT), WidgetStateStore(context).read(id))
        assertFalse(database.quoteDao().getById(quoteId)!!.isSaved)
    }

    @Test fun pinConfirmation_doesNotSaveQuote(): Unit = runBlocking {
        database.quoteDao().setSaved(quoteId, false, null)
        val store = androidx.lifecycle.ViewModelStore()
        lateinit var model: WidgetConfigViewModel
        try {
            instrumentation.runOnMainSync {
                model = WidgetConfigViewModel(context.applicationContext as android.app.Application,
                    androidx.lifecycle.SavedStateHandle())
                store.put("pin-test", model)
                model.initialize(-1, true, quoteId)
            }
            compose.waitUntil(15_000) { !model.state.value.loading }
            assertTrue(model.state.value.quote!!.hasQuote)
            instrumentation.runOnMainSync { model.confirm() }
            compose.waitUntil(15_000) { !model.state.value.submitting }
            assertFalse(database.quoteDao().getById(quoteId)!!.isSaved)
            assertNull(database.quoteDao().getById(quoteId)!!.savedAt)
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }
}
