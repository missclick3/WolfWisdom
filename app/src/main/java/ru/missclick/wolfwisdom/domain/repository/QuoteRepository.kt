package ru.missclick.wolfwisdom.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.missclick.wolfwisdom.domain.model.Quote

interface QuoteRepository {
    /** Seeds the database if needed and resumes the persisted position. */
    suspend fun getCurrentQuote(): Quote

    /** Replays forward history, or appends a random unread quote at its end. */
    suspend fun nextQuote(): Quote

    /** Moves back through history; stays on the first quote at the beginning. */
    suspend fun previousQuote(): Quote

    /** Initializes the feed on collection and observes position and saved-state changes. */
    fun observeCurrentQuote(): Flow<Quote>

    fun observeSavedQuotes(): Flow<List<Quote>>

    /** Sets an explicit value so repeated requests cannot accidentally toggle it twice. */
    suspend fun setSaved(quoteId: Long, isSaved: Boolean)
}
