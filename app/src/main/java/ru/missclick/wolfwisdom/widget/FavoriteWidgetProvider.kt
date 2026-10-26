package ru.missclick.wolfwisdom.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.room.InvalidationTracker
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.context.GlobalContext
import ru.missclick.wolfwisdom.BuildConfig
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase

open class FavoriteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = dispatch(context, ids)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = dispatch(context, intArrayOf(id))
    override fun onDeleted(context: Context, ids: IntArray) {
        val pending = goAsync()
        WidgetUpdates.scope.launch {
            try {
                WidgetUpdates.mutex.withLock { ids.forEach { WidgetStateStore(context).delete(it) } }
                WidgetRenderer.clearCache()
            } finally { pending.finish() }
        }
    }
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val pending = goAsync()
        WidgetUpdates.scope.launch {
            try {
                WidgetUpdates.mutex.withLock { WidgetStateStore(context).restore(oldWidgetIds, newWidgetIds) }
                newWidgetIds.forEach { id ->
                    if (WidgetUpdates.update(context, id)) AppWidgetManager.getInstance(context)
                        .updateAppWidgetOptions(id, Bundle().apply {
                            putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED, true)
                        })
                }
            } finally { pending.finish() }
        }
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_RESTORED) {
            val oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS) ?: return
            val newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: return
            // The base receiver invokes both onRestored and onUpdate for this single broadcast.
            // Our restore already updates, so keep exactly one goAsync/PendingResult owner.
            onRestored(context, oldIds, newIds)
            return
        }
        super.onReceive(context, intent)
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_WALLPAPER_CHANGED, Intent.ACTION_LOCALE_CHANGED, "ru.missclick.wolfwisdom.WIDGET_DEBUG")) {
            if (intent.action?.endsWith("WIDGET_DEBUG") == true && BuildConfig.DEBUG) {
                val edit = context.getSharedPreferences("widget_debug", 0).edit()
                if (intent.hasExtra("fail_images")) edit.putBoolean("fail_images", intent.getBooleanExtra("fail_images", false))
                if (intent.hasExtra("budget_bytes")) edit.putLong("budget_bytes", intent.getLongExtra("budget_bytes", Long.MAX_VALUE))
                edit.commit()
            }
            dispatch(context, WidgetUpdates.ids(context))
        }
    }
    private fun dispatch(context: Context, ids: IntArray) {
        val pending = goAsync()
        WidgetUpdates.scope.launch {
            try { ids.forEach { WidgetUpdates.update(context.applicationContext, it) } }
            finally { pending.finish() }
        }
    }
}

class FavoriteWidget2 : FavoriteWidgetProvider()
class FavoriteWidget3 : FavoriteWidgetProvider()
class FavoriteWidget4 : FavoriteWidgetProvider()

object WidgetUpdates {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val mutex = Mutex()
    private val changes = Channel<Unit>(Channel.CONFLATED)
    val providers = listOf(FavoriteWidget2::class.java, FavoriteWidget3::class.java, FavoriteWidget4::class.java)
    fun ids(context: Context): IntArray = providers.flatMap {
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, it)).toList()
    }.toIntArray()

    fun initialize(application: Application) {
        GlobalContext.get().get<WolfWisdomDatabase>().invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("quotes") {
                override fun onInvalidated(tables: Set<String>) { changes.trySend(Unit) }
            })
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { changes.trySend(Unit) }
        }
        ContextCompat.registerReceiver(application, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_WALLPAPER_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= 27) {
            android.app.WallpaperManager.getInstance(application).addOnColorsChangedListener(
                { _, _ -> changes.trySend(Unit) }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
        scope.launch { for (ignored in changes) ids(application).forEach { update(application, it) } }
        changes.trySend(Unit)
    }

    fun referenceWidth(manager: AppWidgetManager, id: Int): Float = when (manager.getAppWidgetInfo(id)?.provider?.className) {
        FavoriteWidget3::class.java.name -> 248f
        FavoriteWidget4::class.java.name -> 336f
        else -> 160f
    }

    fun sizes(manager: AppWidgetManager, id: Int): List<WidgetDimensions> {
        val options = manager.getAppWidgetOptions(id)
        if (Build.VERSION.SDK_INT >= 31) {
            @Suppress("DEPRECATION")
            val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            // Hosts normally supply portrait/landscape. Bound payload variants even on unusual hosts.
            sizes?.filter { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 }
                ?.distinct()?.sortedBy { it.width * it.height }?.let { actual ->
                    if (actual.isNotEmpty()) {
                        val selected = if (actual.size <= 3) actual else listOf(actual.first(), actual[actual.size / 2], actual.last())
                        return selected.map { WidgetDimensions(it.width.coerceIn(48f, 1024f), it.height.coerceIn(48f, 1024f)) }.distinct()
                    }
                }
        }
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 }?.toFloat() ?: referenceWidth(manager, id)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 0 }?.toFloat() ?: minW
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 }?.toFloat() ?: 160f
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 }?.toFloat() ?: minH
        return listOf(WidgetDimensions(minW.coerceIn(48f, 1024f), maxH.coerceIn(48f, 1024f)),
            WidgetDimensions(maxW.coerceIn(48f, 1024f), minH.coerceIn(48f, 1024f))).distinct()
    }

    suspend fun update(context: Context, id: Int): Boolean = mutex.withLock {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetInfo(id) == null) return@withLock false
        var state = WidgetStateStore(context).read(id) ?: return@withLock false // Never commit canceled drafts.
        if (state.quoteId == null) {
            try {
                val selected = WidgetStateStore.initial()
                if (selected.quoteId != null) {
                    val bound = selected.copy(theme = state.theme)
                    if (WidgetStateStore(context).write(id, bound)) state = bound
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: WidgetDataException) {
                // Preserve the empty binding; the fallback still opens the application.
            }
        }
        val quote = loadWidgetQuote(context, state)
        val sizes = sizes(manager, id)
        val budget = WidgetRenderer.budget(context)
        val views = sizes.map { WidgetRenderer.render(context, id, state, quote, it, budget / sizes.size) }
        val payload = if (Build.VERSION.SDK_INT >= 31 && views.size > 1) RemoteViews(
            sizes.zip(views).associate { (size, view) -> SizeF(size.width, size.height) to view }
        ) else if (views.size > 1) RemoteViews(views[1], views[0]) else views.single()
        Log.d("FavoriteWidget", "id=$id variants=${views.size} totalBudget=$budget")
        try {
            manager.updateAppWidget(id, payload) // Full update: never accumulate bitmap actions.
            true
        } catch (error: IllegalArgumentException) {
            submitFallback(context, manager, id, state, quote, "submission-${error.javaClass.simpleName}")
        } catch (error: SecurityException) {
            submitFallback(context, manager, id, state, quote, "host-${error.javaClass.simpleName}")
        }
    }

    private fun submitFallback(context: Context, manager: AppWidgetManager, id: Int, state: WidgetState, quote: WidgetQuote, reason: String): Boolean {
        return try {
            manager.updateAppWidget(id, WidgetRenderer.fallback(context, id, state, quote, reason))
            true
        } catch (error: RuntimeException) {
            // Only the terminal Binder submission boundary; no retry and no quote text in logs.
            Log.e("FavoriteWidget", "id=$id fallback rejected type=${error.javaClass.simpleName}")
            false
        }
    }
}
