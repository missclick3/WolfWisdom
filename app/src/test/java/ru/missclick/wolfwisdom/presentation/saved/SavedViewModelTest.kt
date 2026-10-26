package ru.missclick.wolfwisdom.presentation.saved

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
    }

    @Test
    fun observeSaved_repositoryFailure_stopsLoadingAndReportsError() = runTest(dispatcher) {
        repository.failObservation = true

        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(SavedEffect.ShowError(R.string.saved_load_error), viewModel.effects.first())
    }

    private class FakeQuoteRepository : QuoteRepository {
        val saved = MutableStateFlow<List<Quote>>(emptyList())
        val writes = mutableListOf<Pair<Long, Boolean>>()
        var writeGate: CompletableDeferred<Unit>? = null
        var failWrites = false
        var failObservation = false

        override fun observeSavedQuotes(): Flow<Result<List<Quote>, DataError.Local>> =
            if (failObservation) flowOf(Result.Failure(DataError.Local.DATABASE_ERROR))
            else saved.map { Result.Success(it) }

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
