package ru.missclick.wolfwisdom.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import ru.missclick.wolfwisdom.domain.util.DataError
import ru.missclick.wolfwisdom.domain.util.Result

class LocalResultTest {
    @Test
    fun localResult_knownFailure_returnsDomainError() = runTest {
        val result = localResult<Unit> { throw LocalDataException(DataError.Local.INVALID_SEED_DATA) }

        assertEquals(Result.Failure(DataError.Local.INVALID_SEED_DATA), result)
    }

    @Test
    fun localResult_unexpectedException_returnsUnknown() = runTest {
        val result = localResult<Unit> { error("Unexpected failure") }

        assertEquals(Result.Failure(DataError.Local.UNKNOWN), result)
    }

    @Test
    fun localResult_cancellation_propagates() = runTest {
        val cancellation = CancellationException("Cancelled")

        try {
            localResult<Unit> { throw cancellation }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun asLocalResult_upstreamFailure_emitsSuccessThenFailureAndCompletes() = runTest {
        val source = flow {
            emit(42)
            throw LocalDataException(DataError.Local.NOT_FOUND)
        }

        assertEquals(
            listOf(Result.Success(42), Result.Failure(DataError.Local.NOT_FOUND)),
            source.asLocalResult().toList()
        )
    }

    @Test
    fun asLocalResult_cancellation_propagatesWithoutFailureEmission() = runTest {
        val cancellation = CancellationException("Cancelled")
        val emissions = mutableListOf<Result<Int, DataError.Local>>()

        try {
            flow<Int> { throw cancellation }.asLocalResult().toList(emissions)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertEquals(emptyList<Result<Int, DataError.Local>>(), emissions)
    }

    @Test
    fun asLocalResult_collectorFailure_propagatesWithoutConversion() = runTest {
        val failure = IllegalStateException("Collector failed")

        try {
            flowOf(42).asLocalResult().collect { throw failure }
            fail("Collector failure must propagate")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
    }
}
