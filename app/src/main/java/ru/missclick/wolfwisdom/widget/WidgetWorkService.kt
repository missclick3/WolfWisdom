package ru.missclick.wolfwisdom.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.app.job.JobWorkItem
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import ru.missclick.wolfwisdom.logging.AppLog

/** Receivers only enqueue: disk I/O, lock waits and rendering belong to this job. */
class WidgetWorkService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            try {
                drainWidgetWork(
                    dequeue = { params.dequeueWork() },
                    process = { item -> withContext(Dispatchers.IO) { process(item.intent) } },
                    complete = { params.completeWork(it) },
                    onFailure = ::reportFailure
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Includes failures at the JobScheduler/Binder boundary itself.
                reportFailure(error)
                try {
                    jobFinished(params, true)
                } catch (error: RuntimeException) {
                    reportFailure(error)
                }
            }
            // dequeueWork(null) finishes an empty queue atomically. Calling jobFinished
            // here could discard work enqueued concurrently with the last dequeue.
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        running = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun process(work: Intent) {
        val ids = work.getIntArrayExtra(IDS)
        when (work.action) {
            DELETE -> {
                WidgetUpdates.mutex.withLock {
                    (ids ?: intArrayOf()).forEach { WidgetStateStore(this).delete(it) }
                }
                WidgetRenderer.clearCache()
            }
            RESTORE -> WidgetUpdates.mutex.withLock {
                WidgetStateStore(this).restore(work.getIntArrayExtra(OLD_IDS) ?: intArrayOf(), ids ?: intArrayOf(),
                    work.getStringExtra(TOKEN))
            }
            PIN -> {
                val id = work.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider !=
                    ComponentName(this, FavoriteWidget2::class.java)) return
                WidgetUpdates.mutex.withLock {
                    val token = work.getStringExtra(TOKEN) ?: return
                    val prefs = getSharedPreferences("widget_pin_requests", 0)
                    val fields = prefs.getString(token, null)?.split('|') ?: return
                    val theme = WidgetTheme.entries.find { it.name == fields.getOrNull(1) } ?: return
                    val state = WidgetState(fields.firstOrNull()?.toLongOrNull()?.takeIf { it > 0 }, theme)
                    if (!WidgetStateStore(this).write(id, state)) throw java.io.IOException("Widget commit failed")
                    prefs.edit().remove(token).remove("time.$token").commit()
                }
                WidgetUpdates.update(this, id)
            }
        }
        // Reconcile even if the last widget was deleted. Each failed item is contained.
        widgetOperation { WidgetUpdates.synchronizeQuotes(this) }
        if (work.action == UPDATE || work.action == RESTORE) {
            (ids ?: WidgetUpdates.ids(this)).forEach { id ->
                currentCoroutineContext().ensureActive()
                widgetOperation {
                    if (WidgetUpdates.update(this, id) && work.action == RESTORE && Build.VERSION.SDK_INT >= 30) {
                        AppWidgetManager.getInstance(this).updateAppWidgetOptions(id, Bundle().apply {
                            putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED, true)
                        })
                    }
                }
            }
        }
    }

    companion object {
        private const val JOB_ID = 1701
        internal const val UPDATE = "widget.update"
        internal const val DELETE = "widget.delete"
        internal const val RESTORE = "widget.restore"
        internal const val PIN = "widget.pin"
        internal const val IDS = "ids"
        internal const val OLD_IDS = "old_ids"
        internal const val TOKEN = "token"

        private fun reportFailure(error: Exception) =
            AppLog.e("FavoriteWidget", "Scheduled widget work failed", error)

        internal fun enqueue(context: Context, work: Intent) {
            try {
                val job = JobInfo.Builder(JOB_ID, ComponentName(context, WidgetWorkService::class.java))
                    .setOverrideDeadline(0).build()
                if (context.getSystemService(JobScheduler::class.java).enqueue(job, JobWorkItem(work)) !=
                    JobScheduler.RESULT_SUCCESS) {
                    AppLog.e("FavoriteWidget", "Widget job was rejected")
                }
            } catch (error: RuntimeException) {
                reportFailure(error)
            }
        }
    }
}
