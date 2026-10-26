package ru.missclick.wolfwisdom.presentation.saved

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
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
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.domain.util.DataError
import ru.missclick.wolfwisdom.domain.util.Result
import ru.missclick.wolfwisdom.domain.util.EmptyResult

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeQuoteRepository()
    private val quiet = Quote(1, "Тишина — это тоже ответ.", true)
    private val strength = Quote(2, "Сила — в спокойствии.", true)

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(quotes: List<Quote> = listOf(quiet, strength)): SavedViewModel {
        repository.saved.value = quotes
        return SavedViewModel(repository).also { store.put("saved", it) }
    }

    @Test
    fun observeSaved_pendingResult_showsLoadingUntilEmptyCollectionIsConfirmed() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.observationGate = gate
        val viewModel = createViewModel(emptyList())

        assertTrue(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.showLoading)
        gate.complete(Unit)

        assertFalse(viewModel.state.value.showLoading)
        assertFalse(viewModel.state.value.hasLoadError)
        assertTrue(viewModel.state.value.visibleQuotes.isEmpty())
    }

    @Test
    fun retry_pendingResult_keepsLoadingUntilCollectionArrives() = runTest(dispatcher) {
        repository.failObservation = true
        val viewModel = createViewModel()
        repository.failObservation = false
        val gate = CompletableDeferred<Unit>()
        repository.observationGate = gate

        val retry = async(dispatcher) { viewModel.errorHandler.retry() }

        assertTrue(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.showLoading)
        gate.complete(Unit)
        retry.await()
        assertFalse(viewModel.state.value.showLoading)
        assertEquals(listOf(quiet, strength), viewModel.state.value.quotes)
    }

    @Test
    fun observeSaved_repositoryChanges_updatesCollection() = runTest(dispatcher) {
        val viewModel = createViewModel()

        repository.saved.value = listOf(strength)

        assertEquals(listOf(strength), viewModel.state.value.quotes)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun search_mixedCaseAndSurroundingSpaces_filtersSavedQuotes() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onAction(SavedAction.OpenSearch)

        viewModel.onAction(SavedAction.SearchQueryChanged("  ТИШИНА  "))

        assertEquals(listOf(quiet), viewModel.state.value.visibleQuotes)
    }

    @Test
    fun search_noMatches_keepsSearchAvailable() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onAction(SavedAction.OpenSearch)

        viewModel.onAction(SavedAction.SearchQueryChanged("рассвет"))

        assertTrue(viewModel.state.value.visibleQuotes.isEmpty())
        assertTrue(viewModel.state.value.isSearchVisible)
        assertEquals(listOf(quiet, strength), viewModel.state.value.quotes)
    }

    @Test
    fun closeSearch_activeQuery_restoresAllQuotes() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onAction(SavedAction.OpenSearch)
        viewModel.onAction(SavedAction.SearchQueryChanged("тишина"))

        viewModel.onAction(SavedAction.CloseSearch)

        assertFalse(viewModel.state.value.isSearchVisible)
        assertEquals("", viewModel.state.value.searchQuery)
        assertEquals(listOf(quiet, strength), viewModel.state.value.visibleQuotes)
    }

    @Test
    fun removeQuote_lastSavedQuote_closesSearchAndEmptiesCollection() = runTest(dispatcher) {
        val viewModel = createViewModel(listOf(quiet))
        viewModel.onAction(SavedAction.OpenSearch)
        viewModel.onAction(SavedAction.SearchQueryChanged("тишина"))

        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))

        assertTrue(viewModel.state.value.quotes.isEmpty())
        assertFalse(viewModel.state.value.isSearchVisible)
        assertEquals("", viewModel.state.value.searchQuery)
        assertEquals(listOf(quiet.id to false), repository.writes)
        viewModel.onAction(SavedAction.OpenSearch)
        assertFalse(viewModel.state.value.isSearchVisible)
    }

    @Test
    fun removeQuote_repeatedWhileSaving_writesOnlyOnce() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = gate

        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))
        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))

        assertEquals(listOf(quiet.id to false), repository.writes)
        assertEquals(setOf(quiet.id), viewModel.state.value.removingQuoteIds)
        gate.complete(Unit)
        assertEquals(listOf(strength), viewModel.state.value.quotes)
        assertTrue(viewModel.state.value.removingQuoteIds.isEmpty())
    }

    @Test
    fun removeQuote_repositoryFailure_keepsQuoteAndReportsError() = runTest(dispatcher) {
        val viewModel = createViewModel(listOf(quiet))
        repository.failWrites = true

        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))

        assertEquals(listOf(quiet), viewModel.state.value.quotes)
        assertTrue(viewModel.state.value.removingQuoteIds.isEmpty())
        assertEquals(SavedEffect.ShowError(R.string.saved_remove_error), viewModel.effects.first())
        assertEquals(null, viewModel.errorHandler.state.value.error)
        assertFalse(viewModel.state.value.showLoading)
    }

    @Test
    fun removeQuote_failureDuringSearch_preservesSearchAndSavedCollection() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onAction(SavedAction.OpenSearch)
        viewModel.onAction(SavedAction.SearchQueryChanged(quiet.quote))
        repository.failWrites = true

        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))

        assertEquals(SavedEffect.ShowError(R.string.saved_remove_error), viewModel.effects.first())
        assertTrue(viewModel.state.value.isSearchVisible)
        assertEquals(quiet.quote, viewModel.state.value.searchQuery)
        assertEquals(listOf(quiet, strength), viewModel.state.value.quotes)
        assertEquals(listOf(quiet), viewModel.state.value.visibleQuotes)
        assertEquals(null, viewModel.errorHandler.state.value.error)
    }

    @Test
    fun removeQuote_retryAfterFailedWrite_removesQuoteAndResetsBusyState() = runTest(dispatcher) {
        val viewModel = createViewModel()
        repository.failWrites = true
        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))
        assertEquals(SavedEffect.ShowError(R.string.saved_remove_error), viewModel.effects.first())
        repository.failWrites = false

        viewModel.onAction(SavedAction.RemoveQuote(quiet.id))

        assertEquals(listOf(strength), viewModel.state.value.quotes)
        assertTrue(viewModel.state.value.removingQuoteIds.isEmpty())
        assertEquals(listOf(quiet.id to false, quiet.id to false), repository.writes)
        assertEquals(null, viewModel.errorHandler.state.value.error)
    }

    @Test
    fun observeSaved_repositoryFailure_stopsLoadingAndReportsError() = runTest(dispatcher) {
        repository.failObservation = true

        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(DataError.Local.DATABASE_ERROR, viewModel.errorHandler.state.value.error)
        assertTrue(viewModel.state.value.hasLoadError)
        assertTrue(viewModel.state.value.showLoading)
    }

    @Test
    fun retry_failedObservation_resubscribesAndUpdatesCollection() = runTest(dispatcher) {
        repository.failObservation = true
        val viewModel = createViewModel()
        repository.failObservation = false

        viewModel.errorHandler.retry()

        assertEquals(listOf(quiet, strength), viewModel.state.value.quotes)
        assertEquals(null, viewModel.errorHandler.state.value.error)
        assertEquals(2, repository.observationCount)
        repository.saved.value = listOf(strength)
        assertEquals(listOf(strength), viewModel.state.value.quotes)
    }

    @Test
    fun retry_repositoryStillFails_keepsErrorAndAllowsAnotherAttempt() = runTest(dispatcher) {
        repository.failObservation = true
        val viewModel = createViewModel()

        viewModel.errorHandler.retry()

        assertEquals(DataError.Local.DATABASE_ERROR, viewModel.errorHandler.state.value.error)
        assertFalse(viewModel.errorHandler.state.value.isRetrying)
        repository.failObservation = false
        viewModel.errorHandler.retry()
        assertEquals(null, viewModel.errorHandler.state.value.error)
    }

    private class FakeQuoteRepository : QuoteRepository {
        val saved = MutableStateFlow<List<Quote>>(emptyList())
        val writes = mutableListOf<Pair<Long, Boolean>>()
        var writeGate: CompletableDeferred<Unit>? = null
        var failWrites = false
        var failObservation = false
        var observationCount = 0
        var observationGate: CompletableDeferred<Unit>? = null

        override fun observeSavedQuotes(): Flow<Result<List<Quote>, DataError.Local>> {
            observationCount++
            val results = if (failObservation) flowOf(Result.Failure(DataError.Local.DATABASE_ERROR))
            else saved.map { Result.Success(it) }
            return flow {
                observationGate?.await()
                emitAll(results)
            }
        }

        override suspend fun setSaved(quoteId: Long, isSaved: Boolean): EmptyResult<DataError.Local> {
            writes += quoteId to isSaved
            writeGate?.await()
            if (failWrites) return Result.Failure(DataError.Local.DATABASE_ERROR)
            saved.value = saved.value.filterNot { it.id == quoteId }
            return Result.Success(Unit)
        }

        override suspend fun getCurrentQuote(): Result<Quote, DataError.Local> = error("Unused")
        override suspend fun nextQuote(): Result<Quote, DataError.Local> = error("Unused")
        override suspend fun previousQuote(): Result<Quote, DataError.Local> = error("Unused")
        override suspend fun hasPreviousQuote(): Result<Boolean, DataError.Local> = Result.Success(false)
        override fun observeCurrentQuote(): Flow<Result<Quote, DataError.Local>> = emptyFlow()
    }
}
