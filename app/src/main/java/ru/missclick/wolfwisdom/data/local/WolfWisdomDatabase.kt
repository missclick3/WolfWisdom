package ru.missclick.wolfwisdom.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true
)
abstract class WolfWisdomDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteNavigationDao(): QuoteNavigationDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotes ADD COLUMN isOnWidget INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotes ADD COLUMN savedAt INTEGER DEFAULT NULL")
            }
        }
    }
}
