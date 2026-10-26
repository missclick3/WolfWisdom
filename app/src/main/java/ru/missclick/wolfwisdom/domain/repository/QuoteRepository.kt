package ru.missclick.wolfwisdom.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.domain.util.DataError
import ru.missclick.wolfwisdom.domain.util.EmptyResult
import ru.missclick.wolfwisdom.domain.util.Result

/** Local failures are returned as values; coroutine cancellation still propagates. */
interface QuoteRepository {
    /** Stable identity lookup; never changes the reading cursor. */
    suspend fun getQuoteById(quoteId: Long): Result<Quote, DataError.Local> =
        Result.Failure(DataError.Local.NOT_FOUND)

    /** Seeds the database if needed and resumes the persisted position. */
    suspend fun getCurrentQuote(): Result<Quote, DataError.Local>

    /** Replays forward history, or appends a random unread quote at its end. */
    suspend fun nextQuote(): Result<Quote, DataError.Local>

    /** Moves back through history; stays on the first quote at the beginning. */
    suspend fun previousQuote(): Result<Quote, DataError.Local>

    /** Whether the persisted cursor has an earlier history entry. */
    suspend fun hasPreviousQuote(): Result<Boolean, DataError.Local>

    /** Initializes on collection. Emits a failure and completes if initialization or observation fails. */
    fun observeCurrentQuote(): Flow<Result<Quote, DataError.Local>>

    fun observeSavedQuotes(): Flow<Result<List<Quote>, DataError.Local>>

    /** Sets an explicit value so repeated requests cannot accidentally toggle it twice. */
    suspend fun setSaved(quoteId: Long, isSaved: Boolean): EmptyResult<DataError.Local>
}
