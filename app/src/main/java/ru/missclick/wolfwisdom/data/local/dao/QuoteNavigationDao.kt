package ru.missclick.wolfwisdom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.missclick.wolfwisdom.data.local.dto.QuoteCycleEntryDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteHistoryDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteReadingStateDto

@Dao
interface QuoteNavigationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadingState(state: QuoteReadingStateDto)

    @Query("SELECT * FROM quote_reading_state WHERE id = 1")
    suspend fun getReadingState(): QuoteReadingStateDto?

    @Query("UPDATE quote_reading_state SET currentHistoryId = :historyId WHERE id = 1")
    suspend fun setCurrentHistoryId(historyId: Long)

    @Query("SELECT * FROM quote_history WHERE id = :id")
    suspend fun getHistoryEntry(id: Long): QuoteHistoryDto?

    @Query("SELECT * FROM quote_history WHERE id > :id ORDER BY id ASC LIMIT 1")
    suspend fun getNextHistoryEntry(id: Long): QuoteHistoryDto?

    @Query("SELECT * FROM quote_history WHERE id < :id ORDER BY id DESC LIMIT 1")
    suspend fun getPreviousHistoryEntry(id: Long): QuoteHistoryDto?

    @Insert
    suspend fun appendHistory(entry: QuoteHistoryDto): Long

    @Insert
    suspend fun markReadInCycle(entry: QuoteCycleEntryDto)

    @Query("DELETE FROM quote_cycle")
    suspend fun clearCycle()

    @Query("""
        SELECT quotes.* FROM quotes
        INNER JOIN quote_history ON quote_history.quoteId = quotes.id
        INNER JOIN quote_reading_state ON quote_reading_state.currentHistoryId = quote_history.id
        WHERE quote_reading_state.id = 1
    """)
    fun observeCurrentQuote(): Flow<QuoteDto?>
}
