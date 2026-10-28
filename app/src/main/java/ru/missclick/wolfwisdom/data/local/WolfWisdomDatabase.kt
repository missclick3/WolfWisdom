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
    version = 4,
    exportSchema = true
)
abstract class WolfWisdomDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteNavigationDao(): QuoteNavigationDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Match text, not seed row numbers: existing IDs must remain stable for
                // saved quotes, reading history and home-screen widget bindings.
                val removedQuotes = arrayOf(
                    "Лучше иметь друга, чем друг друга.",
                    "Лучше светлое пиво, чем тёмное будущее.",
                    "Волку не нужны отношения, волку нужно пиво.",
                    "У пастуха, боявшегося волка, стадо не увеличивалось.",
                    "Там, где раньше волки срали, мы построим магистрали.",
                    "Бегать за овцами — удел баранов. Я бегаю только за пивом.",
                    "Если не спится ночью, попробуй спиться днём."
                )
                db.execSQL("CREATE TEMP TABLE wolf_removed_quotes_v4 (id INTEGER PRIMARY KEY)")
                db.execSQL(
                    "INSERT INTO wolf_removed_quotes_v4 SELECT id FROM quotes WHERE quote IN (${removedQuotes.joinToString { "?" }})",
                    removedQuotes
                )
                // Repair an affected cursor before removing RESTRICT-linked history.
                // Prefer the closest earlier surviving entry, then the next one.
                // A null cursor is initialized normally by the repository.
                db.execSQL("""
                    UPDATE quote_reading_state SET currentHistoryId = COALESCE(
                        (SELECT MAX(id) FROM quote_history
                         WHERE id < quote_reading_state.currentHistoryId
                         AND quoteId NOT IN (SELECT id FROM wolf_removed_quotes_v4)),
                        (SELECT MIN(id) FROM quote_history
                         WHERE id > quote_reading_state.currentHistoryId
                         AND quoteId NOT IN (SELECT id FROM wolf_removed_quotes_v4))
                    ) WHERE currentHistoryId IN (
                        SELECT id FROM quote_history WHERE quoteId IN (SELECT id FROM wolf_removed_quotes_v4)
                    )
                """.trimIndent())
                db.execSQL("DELETE FROM quote_history WHERE quoteId IN (SELECT id FROM wolf_removed_quotes_v4)")
                db.execSQL("DELETE FROM quote_cycle WHERE quoteId IN (SELECT id FROM wolf_removed_quotes_v4)")
                db.execSQL("DELETE FROM quotes WHERE id IN (SELECT id FROM wolf_removed_quotes_v4)")
                db.execSQL("DROP TABLE wolf_removed_quotes_v4")
            }
        }

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
