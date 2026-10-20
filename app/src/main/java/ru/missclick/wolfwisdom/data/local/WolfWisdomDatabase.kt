package ru.missclick.wolfwisdom.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.missclick.wolfwisdom.data.local.dao.QuoteDao
import ru.missclick.wolfwisdom.data.local.dao.QuoteNavigationDao
import ru.missclick.wolfwisdom.data.local.dto.QuoteCycleEntryDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteHistoryDto
import ru.missclick.wolfwisdom.data.local.dto.QuoteReadingStateDto

@Database(
    entities = [
        QuoteDto::class,
        QuoteHistoryDto::class,
        QuoteReadingStateDto::class,
        QuoteCycleEntryDto::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WolfWisdomDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteNavigationDao(): QuoteNavigationDao
}
