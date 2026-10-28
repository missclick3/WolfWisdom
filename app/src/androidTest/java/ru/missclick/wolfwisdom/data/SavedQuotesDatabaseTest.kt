package ru.missclick.wolfwisdom.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.data.local.QuoteSeedSource
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.data.mapper.toDomain
import ru.missclick.wolfwisdom.data.repository.RoomQuoteRepository
import ru.missclick.wolfwisdom.domain.util.Result
import ru.missclick.wolfwisdom.domain.util.DataError

@RunWith(AndroidJUnit4::class)
class SavedQuotesDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var database: WolfWisdomDatabase? = null
    private val migrationDatabaseName = "saved-quotes-migration-test.db"

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(migrationDatabaseName)
    }

    private fun inMemoryDatabase(): WolfWisdomDatabase =
        Room.inMemoryDatabaseBuilder(context, WolfWisdomDatabase::class.java)
            .build().also { database = it }

    @Test
    fun migration_preservesSavedQuotesAndReadingHistory() = runBlocking { verifyMigration(1) }

    @Test
    fun migration2_preservesSavedQuotesAndReadingHistory() = runBlocking { verifyMigration(2) }

    @Test
    fun migration3_preservesOtherSavedQuotesAndReadingHistory() = runBlocking { verifyMigration(3) }

    @Test
    fun withdrawnQuotesAreRemovedWithoutBreakingForeignKeysOrOtherSaves() = runBlocking {
        val db = inMemoryDatabase()
        val sql = db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO quotes (id, quote, isSaved, savedAt) VALUES (100, 'Keep saved', 1, 123), (200, 'Keep next', 0, NULL)")
        // Deliberately different IDs from the original seed: deletion must match text.
        sql.execSQL("INSERT INTO quotes (id, quote, isSaved) VALUES (901, ?, 1), (902, ?, 0)", arrayOf(
            "Лучше светлое пиво, чем тёмное будущее.", "Если не спится ночью, попробуй спиться днём."
        ))
        sql.execSQL("INSERT INTO quote_history (id, quoteId) VALUES (1, 100), (2, 901), (3, 902), (4, 200)")
        sql.execSQL("INSERT INTO quote_reading_state (id, currentHistoryId) VALUES (1, 3)")
        sql.execSQL("INSERT INTO quote_cycle (quoteId) VALUES (100), (901), (902)")
        db.runInTransaction { WolfWisdomDatabase.MIGRATION_3_4.migrate(sql) }

        assertNull(db.quoteDao().getById(901))
        assertNull(db.quoteDao().getById(902))
        assertEquals(listOf(QuoteDto(100, "Keep saved", true, 123)), db.quoteDao().observeSaved().first())
        assertEquals(1L, db.quoteNavigationDao().getReadingState()?.currentHistoryId)
        assertNull(db.quoteNavigationDao().getHistoryEntry(2))
        assertNull(db.quoteNavigationDao().getHistoryEntry(3))
        assertEquals(200L, db.quoteNavigationDao().getHistoryEntry(4)?.quoteId)
        sql.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        sql.query("SELECT quoteId FROM quote_cycle").use {
            assertTrue(it.moveToFirst())
            assertEquals(100L, it.getLong(0))
            assertFalse(it.moveToNext())
        }
    }

    private suspend fun verifyMigration(fromVersion: Int) {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase/$fromVersion.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(migrationDatabaseName)
        path.parentFile?.mkdirs()
        context.deleteDatabase(migrationDatabaseName)
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                old.execSQL(entity.getString("createSql").replace(
                    "\${TABLE_NAME}", entity.getString("tableName")
                ))
                entity.optJSONArray("indices")?.let { indices ->
                    for (indexNumber in 0 until indices.length()) {
                        old.execSQL(indices.getJSONObject(indexNumber).getString("createSql").replace(
                            "\${TABLE_NAME}", entity.getString("tableName")
                        ))
                    }
                }
            }
            old.execSQL("INSERT INTO quotes (id, quote, isSaved) VALUES (1, 'Old saved', 1), (2, 'Unsaved', 0)")
            old.execSQL("INSERT INTO quote_history (id, quoteId) VALUES (1, 1)")
            old.execSQL("INSERT INTO quote_reading_state (id, currentHistoryId) VALUES (1, 1)")
            old.execSQL("INSERT INTO quote_cycle (quoteId) VALUES (1)")
            old.version = fromVersion
        }

        val migrated = Room.databaseBuilder(context, WolfWisdomDatabase::class.java, migrationDatabaseName)
            .addMigrations(WolfWisdomDatabase.MIGRATION_1_2, WolfWisdomDatabase.MIGRATION_2_3, WolfWisdomDatabase.MIGRATION_3_4)
            .build().also { database = it }

        val saved = migrated.quoteDao().observeSaved().first()
        assertEquals(listOf(QuoteDto(1, "Old saved", true, null)), saved)
        assertFalse(requireNotNull(migrated.quoteDao().getById(2)).isSaved)
        assertEquals(1L, migrated.quoteNavigationDao().getReadingState()?.currentHistoryId)
        assertEquals(1L, migrated.quoteNavigationDao().getHistoryEntry(1)?.quoteId)
        migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM quote_cycle").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        assertEquals(4, migrated.openHelper.readableDatabase.version)
        assertFalse(requireNotNull(migrated.quoteDao().getById(1)).isOnWidget)
    }

    @Test
    fun savedQuotes_newestFirstWithLegacySavesLast() = runBlocking {
        val dao = inMemoryDatabase().quoteDao()
        dao.insertAll(listOf(
            QuoteDto(1, "Legacy", true),
            QuoteDto(2, "Newest"),
            QuoteDto(3, "Earlier"),
            QuoteDto(4, "Same timestamp")
        ))
        dao.setSaved(3, true, 100L)
        dao.setSaved(4, true, 100L)
        dao.setSaved(2, true, 200L)

        assertEquals(listOf(2L, 3L, 4L, 1L), dao.observeSaved().first().map { it.id })
        assertEquals(200L, dao.getById(2)?.toDomain()?.savedAt)
    }

    @Test
    fun savedTimestamp_repeatedSavePreservesDateAndResaveRefreshesIt() = runBlocking {
        val dao = inMemoryDatabase().quoteDao()
        dao.insertAll(listOf(QuoteDto(1, "First"), QuoteDto(2, "Second")))
        dao.setSaved(1, true, 100L)
        dao.setSaved(2, true, 200L)

        dao.setSaved(1, true, 300L)
        assertEquals(100L, dao.getById(1)?.savedAt)
        assertEquals(listOf(2L, 1L), dao.observeSaved().first().map { it.id })

        dao.setSaved(1, false, null)
        assertNull(dao.getById(1)?.savedAt)
        assertEquals(listOf(2L), dao.observeSaved().first().map { it.id })

        dao.setSaved(1, true, 400L)
        assertEquals(400L, dao.getById(1)?.savedAt)
        assertEquals(listOf(1L, 2L), dao.observeSaved().first().map { it.id })
    }

    @Test
    fun repository_saveRecordsCurrentEpochMilliseconds() = runBlocking {
        val db = inMemoryDatabase()
        db.quoteDao().insertAll(listOf(QuoteDto(1, "Quote")))
        val repository = RoomQuoteRepository(db, QuoteSeedSource(context.assets))
        val before = System.currentTimeMillis()

        assertEquals(Result.Success(Unit), repository.setSaved(1, true))

        val savedAt = requireNotNull((repository.observeSavedQuotes().first() as Result.Success).data.single().savedAt)
        assertTrue(savedAt in before..System.currentTimeMillis())
        assertEquals(Result.Success(Unit), repository.setSaved(1, false))
        assertNull(db.quoteDao().getById(1)?.savedAt)
    }
    @Test
    fun repository_unknownSaveId_returnsNotFoundWithoutChangingSavedQuotes() = runBlocking {
        val db = inMemoryDatabase()
        db.quoteDao().insertAll(listOf(QuoteDto(1, "Saved quote", true, 100L)))
        val repository = RoomQuoteRepository(db, QuoteSeedSource(context.assets))

        assertEquals(Result.Failure(DataError.Local.NOT_FOUND), repository.setSaved(999, false))
        assertEquals(
            Result.Success(listOf(QuoteDto(1, "Saved quote", true, 100L).toDomain())),
            repository.observeSavedQuotes().first()
        )
    }

    @Test
    fun repository_navigationAndObservation_returnSuccessfulResults() = runBlocking {
        val db = inMemoryDatabase()
        db.quoteDao().insertAll(listOf(QuoteDto(1, "Only quote")))
        val repository = RoomQuoteRepository(db, QuoteSeedSource(context.assets))
        val expected = Result.Success(QuoteDto(1, "Only quote").toDomain())

        assertEquals(expected, repository.observeCurrentQuote().first())
        assertEquals(expected, repository.getCurrentQuote())
        assertEquals(Result.Success(false), repository.hasPreviousQuote())
        assertEquals(expected, repository.previousQuote())
        assertEquals(expected, repository.nextQuote())
        assertEquals(Result.Success(true), repository.hasPreviousQuote())
    }

}
