package ru.missclick.wolfwisdom.presentation.error

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.missclick.wolfwisdom.domain.util.DataError

@OptIn(ExperimentalCoroutinesApi::class)
class LoadErrorHandlerTest {
    @Test
    fun retry_emitsFailedUpdateEventAndWaitsForResult() = runTest {
        val handler = LoadErrorHandler<String>()
        handler.onFailure(DataError.Local.DATABASE_ERROR, "reload-saved")

        val retry = async(UnconfinedTestDispatcher(testScheduler)) { handler.retry() }

        assertEquals("reload-saved", handler.retryEvents.first())
        assertTrue(handler.state.value.isRetrying)
        assertFalse(retry.isCompleted)
        handler.onSuccess()
        retry.await()
        assertEquals(LoadErrorState(), handler.state.value)
    }

    @Test
    fun retry_repeatedWhilePending_emitsOnlyOneUpdate() = runTest {
        val handler = LoadErrorHandler<String>()
        handler.onFailure(DataError.Local.DATABASE_ERROR, "refresh")
        val pending = async(UnconfinedTestDispatcher(testScheduler)) { handler.retry() }
        handler.retry()
        assertEquals("refresh", handler.retryEvents.first())
        val duplicate = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            handler.retryEvents.first()
        }
        assertFalse(duplicate.isCompleted)
        handler.onFailure(DataError.Local.DATABASE_ERROR, "refresh")
        pending.await()
        assertFalse(handler.state.value.isRetrying)
        assertEquals(DataError.Local.DATABASE_ERROR, handler.state.value.error)
    }

    @Test
    fun retry_cancelledWhilePending_keepsErrorAndEnablesRetry() = runTest {
        val handler = LoadErrorHandler<String>()
        handler.onFailure(DataError.Local.DATABASE_ERROR, "refresh")
        val pending = async(UnconfinedTestDispatcher(testScheduler)) { handler.retry() }
        handler.retryEvents.first()

        pending.cancel()
        pending.join()

        assertFalse(handler.state.value.isRetrying)
        assertEquals(DataError.Local.DATABASE_ERROR, handler.state.value.error)
    }
}
