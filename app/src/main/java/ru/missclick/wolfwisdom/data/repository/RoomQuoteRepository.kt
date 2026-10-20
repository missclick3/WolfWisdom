package ru.missclick.wolfwisdom.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.missclick.wolfwisdom.data.local.QuoteSeedSource
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase
import ru.missclick.wolfwisdom.data.local.dto.QuoteCycleEntryDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteHistoryDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteReadingStateDto
import ru.missclick.wolfwisdom.data.mapper.toDomain
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository

class RoomQuoteRepository(
    private val database: WolfWisdomDatabase,
    private val seedSource: QuoteSeedSource
) : QuoteRepository {
    private val quotes = database.quoteDao()
    private val navigation = database.quoteNavigationDao()

    override suspend fun getCurrentQuote(): Quote = database.withTransaction {
        initializeIfNeeded()
        currentEntry()?.let { quoteFor(it) } ?: appendRandomQuote(previousQuoteId = null)
    }.toDomain()

    override suspend fun nextQuote(): Quote = database.withTransaction {
        initializeIfNeeded()
        val current = currentEntry()
        if (current == null) {
            appendRandomQuote(previousQuoteId = null)
        } else {
            val next = navigation.getNextHistoryEntry(current.id)
            if (next != null) {
                navigation.setCurrentHistoryId(next.id)
                quoteFor(next)
            } else {
                appendRandomQuote(previousQuoteId = current.quoteId)
            }
        }
    }.toDomain()

    override suspend fun previousQuote(): Quote = database.withTransaction {
        initializeIfNeeded()
        val current = currentEntry()
        if (current == null) {
            appendRandomQuote(previousQuoteId = null)
        } else {
            val previous = navigation.getPreviousHistoryEntry(current.id)
            if (previous == null) {
                quoteFor(current)
            } else {
                navigation.setCurrentHistoryId(previous.id)
                quoteFor(previous)
            }
        }
    }.toDomain()

    override fun observeCurrentQuote(): Flow<Quote> = flow {
        getCurrentQuote()
        emitAll(navigation.observeCurrentQuote().map { dto ->
            checkNotNull(dto) { "The current quote must exist after initialization" }.toDomain()
        })
    }.distinctUntilChanged()

    override fun observeSavedQuotes(): Flow<List<Quote>> = flow {
        database.withTransaction { initializeIfNeeded() }
        emitAll(quotes.observeSaved().map { saved -> saved.map { it.toDomain() } })
    }.distinctUntilChanged()

    override suspend fun setSaved(quoteId: Long, isSaved: Boolean) {
        database.withTransaction {
            initializeIfNeeded()
            require(quotes.setSaved(quoteId, isSaved) == 1) { "Unknown quote id: $quoteId" }
        }
    }

    // Called only inside a Room transaction, serializing initialization and navigation
    // across concurrent callers, collectors, and repository instances.
    private suspend fun initializeIfNeeded() {
        if (quotes.count() == 0) {
            quotes.insertAll(seedSource.load())
        }
        navigation.insertReadingState(QuoteReadingStateDto())
    }

    private suspend fun currentEntry(): QuoteHistoryDto? {
        val state = checkNotNull(navigation.getReadingState())
        val historyId = state.currentHistoryId ?: return null
        return checkNotNull(navigation.getHistoryEntry(historyId))
    }

    private suspend fun quoteFor(entry: QuoteHistoryDto): QuoteDto =
        checkNotNull(quotes.getById(entry.quoteId))

    private suspend fun appendRandomQuote(previousQuoteId: Long?): QuoteDto {
        var quote = quotes.getRandomUnread(excludedId = null)
        if (quote == null) {
            // Begin a new cycle, keeping all old history and avoiding an immediate repeat.
            navigation.clearCycle()
            quote = quotes.getRandomUnread(excludedId = previousQuoteId)
                // A one-quote dataset must still allow moving forward.
                ?: quotes.getRandomUnread(excludedId = null)
        }
        val selected = checkNotNull(quote) { "No quotes available after seeding" }
        navigation.markReadInCycle(QuoteCycleEntryDto(quoteId = selected.id))
        val historyId = navigation.appendHistory(QuoteHistoryDto(quoteId = selected.id))
        navigation.setCurrentHistoryId(historyId)
        return selected
    }
}
