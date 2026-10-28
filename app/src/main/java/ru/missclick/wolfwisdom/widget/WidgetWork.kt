package ru.missclick.wolfwisdom.widget

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.missclick.wolfwisdom.logging.AppLog

/** Storage and launcher failures must not escape background work or UI submissions. */
internal suspend fun widgetOperation(
    onFailure: (Exception) -> Unit = { AppLog.e("FavoriteWidget", "Widget operation failed", it) },
    block: suspend () -> Unit
): Boolean = try {
    block()
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
    false
}

/** Failed items finish without a retry loop; stopped items remain owned by JobScheduler. */
internal suspend fun <T : Any> drainWidgetWork(
    dequeue: () -> T?,
    process: suspend (T) -> Unit,
    complete: (T) -> Unit,
    onFailure: (Exception) -> Unit
) {
    while (true) {
        currentCoroutineContext().ensureActive()
        val item = dequeue() ?: return
        widgetOperation(onFailure) { process(item) }
        currentCoroutineContext().ensureActive()
        complete(item)
    }
}
