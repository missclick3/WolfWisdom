package ru.missclick.wolfwisdom.presentation.main

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.domain.util.DataError
import ru.missclick.wolfwisdom.domain.util.EmptyResult
import ru.missclick.wolfwisdom.domain.util.Result

@OptIn(ExperimentalCoroutinesApi::class)
class MainLoadingErrorTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel() = MainViewModel(repository).also { store.put("main", it) }

    @Test
    fun initialization_pendingResult_showsLoadingUntilQuoteAndHistoryArrive() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.observationGate = gate
        val model = viewModel()

        assertTrue(model.state.value.isLoading)
        assertTrue(model.state.value.showLoading)
        gate.complete(Unit)

        assertFalse(model.state.value.showLoading)
        assertEquals(1L, model.state.value.quoteId)
    }

    @Test
    fun reload_existingQuote_showsLoadingUntilFreshResultArrives() = runTest(dispatcher) {
        val model = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.observationGate = gate

        model.onAction(MainAction.Reload)

        assertTrue(model.state.value.showLoading)
        gate.complete(Unit)
        assertFalse(model.state.value.showLoading)
    }

    @Test
    fun initialization_failureThenRetry_restartsObservationAndLoadsQuote() = runTest(dispatcher) {
        repository.failObservation = true
        val model = viewModel()
        assertEquals(DataError.Local.DATABASE_ERROR, model.errorHandler.state.value.error)
        assertTrue(model.state.value.showLoading)
        assertTrue(model.state.value.hasLoadError)

        repository.failObservation = false
        model.errorHandler.retry()

        assertEquals(1L, model.state.value.quoteId)
        assertEquals(null, model.errorHandler.state.value.error)
        assertEquals(2, repository.observations)
        assertFalse(model.state.value.showLoading)
    }

    @Test
    fun loadingNext_failureThenRetry_repeatsNextOperation() = runTest(dispatcher) {
        val model = viewModel()
        repository.failNext = true
        model.onAction(MainAction.SwipeUp)
        assertEquals(1L, model.state.value.quoteId)
        assertEquals(DataError.Local.DATABASE_ERROR, model.errorHandler.state.value.error)

        repository.failNext = false
        model.errorHandler.retry()

        assertEquals(2, repository.nextCalls)
        assertEquals(2L, model.state.value.quoteId)
        assertFalse(model.state.value.isNavigating)
        assertEquals(null, model.errorHandler.state.value.error)
        assertFalse(model.state.value.showLoading)
    }

    @Test
    fun historyRead_failureThenRetry_refreshesQuoteAndHistory() = runTest(dispatcher) {
        repository.failHistory = true
        val model = viewModel()
        assertEquals(DataError.Local.DATABASE_ERROR, model.errorHandler.state.value.error)

        repository.failHistory = false
        model.errorHandler.retry()

        assertEquals(1L, model.state.value.quoteId)
        assertEquals(null, model.errorHandler.state.value.error)
    }

    private class FakeRepository : QuoteRepository {
        val current = MutableStateFlow(Quote(1, "Current quote", false))
        var failObservation = false
        var failNext = false
        var failHistory = false
        var observations = 0
        var nextCalls = 0
        var observationGate: CompletableDeferred<Unit>? = null
        override fun observeCurrentQuote(): Flow<Result<Quote, DataError.Local>> {
            observations++
            val results = if (failObservation) flowOf(Result.Failure(DataError.Local.DATABASE_ERROR))
            else current.map { Result.Success(it) }
            return flow {
                observationGate?.await()
                emitAll(results)
            }
        }
        override suspend fun nextQuote(): Result<Quote, DataError.Local> {
            nextCalls++
            return if (failNext) Result.Failure(DataError.Local.DATABASE_ERROR)
            else Result.Success(Quote(2, "Next quote", false))
        }
        override suspend fun hasPreviousQuote(): Result<Boolean, DataError.Local> =
            if (failHistory) Result.Failure(DataError.Local.DATABASE_ERROR) else Result.Success(false)
        override suspend fun getCurrentQuote(): Result<Quote, DataError.Local> = error("Unused")
        override suspend fun previousQuote(): Result<Quote, DataError.Local> = error("Unused")
        override fun observeSavedQuotes(): Flow<Result<List<Quote>, DataError.Local>> = emptyFlow()
        override suspend fun setSaved(quoteId: Long, isSaved: Boolean): EmptyResult<DataError.Local> = error("Unused")
    }
}
