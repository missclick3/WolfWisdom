package ru.missclick.wolfwisdom.widget

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WidgetWorkTest {
    @Test fun operation_storageFailure_returnsFalseAndReportsFailure() = runTest {
        val failure = IOException("Storage full")
        val reported = mutableListOf<Exception>()

        assertFalse(widgetOperation(reported::add) { throw failure })

        assertEquals(listOf(failure), reported)
    }

    @Test fun operation_schemaFailure_returnsFalseAndReportsFailure() = runTest {
        val failure = IllegalStateException("Room schema mismatch")
        val reported = mutableListOf<Exception>()

        assertFalse(widgetOperation(reported::add) { throw failure })

        assertEquals(listOf(failure), reported)
    }

    @Test fun operation_cancellation_propagatesWithoutReportingFailure() = runTest {
        val cancelled = CancellationException("Job stopped")
        try {
            widgetOperation({ fail("Cancellation is not an operation failure") }) { throw cancelled }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }

    @Test fun operation_fatalError_isNotSwallowed() = runTest {
        val failure = AssertionError("Programming error")
        try {
            widgetOperation({ fail("Fatal errors must propagate") }) { throw failure }
            fail("Fatal error must propagate")
        } catch (actual: AssertionError) {
            assertSame(failure, actual)
        }
    }

    @Test fun queue_failedItem_finishesOnceAndDoesNotPreventNextItem() = runTest {
        val queue = ArrayDeque(listOf("delete", "restore", "update"))
        val completed = mutableListOf<String>()
        val processed = mutableListOf<String>()
        val failures = mutableListOf<Exception>()

        drainWidgetWork(
            dequeue = { queue.removeFirstOrNull() },
            process = { item ->
                processed.add(item)
                if (item == "delete") throw IOException("Storage full")
                if (item == "restore") throw IllegalStateException("Room schema mismatch")
            },
            complete = { completed.add(it) },
            onFailure = { failures.add(it) }
        )

        assertEquals(listOf("delete", "restore", "update"), processed)
        assertEquals(processed, completed)
        assertEquals(2, failures.size)
    }

    @Test fun queue_stoppedDuringLockWait_doesNotCompleteUnfinishedWork() = runTest {
        val queue = ArrayDeque(listOf("waiting", "next"))
        val completed = mutableListOf<String>()
        val lockAvailable = CompletableDeferred<Unit>()
        val worker = launch {
            drainWidgetWork(
                dequeue = { queue.removeFirstOrNull() },
                process = { lockAvailable.await() },
                complete = { completed.add(it) },
                onFailure = { fail("Stopping a job must not report an operation failure") }
            )
        }
        runCurrent()
        worker.cancel()
        worker.join()

        assertTrue(completed.isEmpty())
        assertEquals(listOf("next"), queue.toList())
    }

    @Test fun queue_delayedRendering_onlyCompletesAfterRenderingFinishes() = runTest {
        var dequeued = false
        val rendering = CompletableDeferred<Unit>()
        var completed = 0
        val worker = launch {
            drainWidgetWork(
                dequeue = { if (dequeued) null else "update".also { dequeued = true } },
                process = { rendering.await() },
                complete = { completed++ },
                onFailure = { fail("Unexpected failure: $it") }
            )
        }
        runCurrent()
        assertEquals(0, completed)

        rendering.complete(Unit)
        worker.join()
        assertEquals(1, completed)
    }
}
