package ru.missclick.wolfwisdom.data.repository

import androidx.room.withTransaction
import java.io.IOException
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
import ru.missclick.wolfwisdom.domain.util.DataError
import ru.missclick.wolfwisdom.domain.util.EmptyResult
import ru.missclick.wolfwisdom.domain.util.Result

class RoomQuoteRepository(
    private val database: WolfWisdomDatabase,
    private val seedSource: QuoteSeedSource
) : QuoteRepository {
    private val quotes = database.quoteDao()
    private val navigation = database.quoteNavigationDao()

    override suspend fun getCurrentQuote(): Result<Quote, DataError.Local> = localResult {
        database.withTransaction {
            initializeIfNeeded()
            currentEntry()?.let { quoteFor(it) } ?: appendRandomQuote(previousQuoteId = null)
        }.toDomain()
    }

    override suspend fun nextQuote(): Result<Quote, DataError.Local> = localResult {
        database.withTransaction {
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
    }

    override suspend fun previousQuote(): Result<Quote, DataError.Local> = localResult {
        database.withTransaction {
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
    }

    override suspend fun hasPreviousQuote(): Result<Boolean, DataError.Local> = localResult {
        database.withTransaction {
            initializeIfNeeded()
            currentEntry()?.let { navigation.getPreviousHistoryEntry(it.id) != null } ?: false
        }
    }

    override fun observeCurrentQuote(): Flow<Result<Quote, DataError.Local>> = flow {
        // Keep initialization failures inside the upstream flow so they become Failure emissions.
        database.withTransaction {
            initializeIfNeeded()
            if (currentEntry() == null) appendRandomQuote(previousQuoteId = null)
        }
        emitAll(navigation.observeCurrentQuote().map { dto ->
            (dto ?: throw LocalDataException(DataError.Local.NOT_FOUND)).toDomain()
        })
    }.asLocalResult().distinctUntilChanged()

    override fun observeSavedQuotes(): Flow<Result<List<Quote>, DataError.Local>> = flow {
        database.withTransaction { initializeIfNeeded() }
        emitAll(quotes.observeSaved().map { saved -> saved.map { it.toDomain() } })
    }.asLocalResult().distinctUntilChanged()

    override suspend fun setSaved(quoteId: Long, isSaved: Boolean): EmptyResult<DataError.Local> = localResult {
        database.withTransaction {
            initializeIfNeeded()
            val savedAt = if (isSaved) System.currentTimeMillis() else null
            if (quotes.setSaved(quoteId, isSaved, savedAt) != 1) {
                throw LocalDataException(DataError.Local.NOT_FOUND)
            }
        }
    }

    // Called only inside a Room transaction, serializing initialization and navigation
    // across concurrent callers, collectors, and repository instances.
    private suspend fun initializeIfNeeded() {
        if (quotes.count() == 0) {
            val seed = try {
                seedSource.load()
            } catch (error: IOException) {
                throw LocalDataException(DataError.Local.SEED_READ_FAILED, error)
            } catch (error: IllegalArgumentException) {
                throw LocalDataException(DataError.Local.INVALID_SEED_DATA, error)
            }
            quotes.insertAll(seed)
        }
        navigation.insertReadingState(QuoteReadingStateDto())
    }

    private suspend fun currentEntry(): QuoteHistoryDto? {
        val state = navigation.getReadingState() ?: throw LocalDataException(DataError.Local.NOT_FOUND)
        val historyId = state.currentHistoryId ?: return null
        return navigation.getHistoryEntry(historyId) ?: throw LocalDataException(DataError.Local.NOT_FOUND)
    }

    private suspend fun quoteFor(entry: QuoteHistoryDto): QuoteDto =
        quotes.getById(entry.quoteId) ?: throw LocalDataException(DataError.Local.NOT_FOUND)

    private suspend fun appendRandomQuote(previousQuoteId: Long?): QuoteDto {
        var quote = quotes.getRandomUnread(excludedId = null)
        if (quote == null) {
            // Begin a new cycle, keeping all old history and avoiding an immediate repeat.
            navigation.clearCycle()
            quote = quotes.getRandomUnread(excludedId = previousQuoteId)
                // A one-quote dataset must still allow moving forward.
                ?: quotes.getRandomUnread(excludedId = null)
        }
        val selected = quote ?: throw LocalDataException(DataError.Local.NOT_FOUND)
        navigation.markReadInCycle(QuoteCycleEntryDto(quoteId = selected.id))
        val historyId = navigation.appendHistory(QuoteHistoryDto(quoteId = selected.id))
        navigation.setCurrentHistoryId(historyId)
        return selected
    }
}
