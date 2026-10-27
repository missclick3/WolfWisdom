package ru.missclick.wolfwisdom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto

@Dao
interface QuoteDao {
    /** Reconcile all memberships atomically; unchanged rows do not trigger observer loops. */
    @Query("UPDATE quotes SET isOnWidget = (id IN (:quoteIds)) WHERE isOnWidget != (id IN (:quoteIds))")
    suspend fun synchronizeWidgetQuotes(quoteIds: List<Long>)

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(quotes: List<QuoteDto>)

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: Long): QuoteDto?

    @Query("SELECT * FROM quotes WHERE isSaved = 1 ORDER BY savedAt DESC, id ASC")
    fun observeSaved(): Flow<List<QuoteDto>>

    @Query("""
        UPDATE quotes
        SET isSaved = :isSaved,
            savedAt = CASE
                WHEN :isSaved = 0 THEN NULL
                WHEN isSaved = 1 AND savedAt IS NOT NULL THEN savedAt
                ELSE :savedAt
            END
        WHERE id = :id
    """)
    suspend fun setSaved(id: Long, isSaved: Boolean, savedAt: Long?): Int

    @Query("""
        SELECT * FROM quotes
        WHERE id NOT IN (SELECT quoteId FROM quote_cycle)
            AND (:excludedId IS NULL OR id != :excludedId)
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getRandomUnread(excludedId: Long?): QuoteDto?
}
