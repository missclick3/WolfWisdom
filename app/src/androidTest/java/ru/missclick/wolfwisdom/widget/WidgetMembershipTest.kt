package ru.missclick.wolfwisdom.widget

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.data.mapper.toDomain

@RunWith(AndroidJUnit4::class)
class WidgetMembershipTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun membership_tracksMultipleWidgetsRebindingDeletionAndStaleRecords() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, WolfWisdomDatabase::class.java).build()
        val store = WidgetStateStore(context)
        val first = 90201
        val second = 90202
        val stale = 90203
        val dao = database.quoteDao()
        try {
            dao.insertAll(listOf(QuoteDto(1, "First", true, 123L), QuoteDto(2, "Second")))
            suspend fun sync(vararg ids: Int) = WidgetUpdates.synchronizeQuotes(context, ids, database)
            assertTrue(store.write(first, WidgetState(1)))
            assertTrue(store.write(second, WidgetState(1)))
            sync(first, second)
            assertTrue(dao.getById(1)!!.toDomain().isOnWidget)
            assertFalse(dao.getById(2)!!.isOnWidget)

            store.delete(first)
            sync(second)
            assertTrue(dao.getById(1)!!.isOnWidget)

            // Future quote changes use the same binding write path.
            assertTrue(store.write(second, WidgetState(2)))
            sync(second)
            assertFalse(dao.getById(1)!!.isOnWidget)
            assertTrue(dao.getById(2)!!.toDomain().isOnWidget)
            assertTrue(dao.getById(1)!!.isSaved)
            assertEquals(123L, dao.getById(1)!!.savedAt)
            assertFalse(dao.getById(2)!!.isSaved)
            assertEquals(listOf(1L), dao.observeSaved().first().map { it.id })

            // Persisted records alone do not count if the launcher no longer has that widget.
            assertTrue(store.write(stale, WidgetState(1)))
            sync(second)
            assertFalse(dao.getById(1)!!.isOnWidget)
            store.restore(intArrayOf(second), intArrayOf(first))
            sync(first)
            assertTrue(dao.getById(2)!!.isOnWidget)
            store.delete(first)
            sync()
            assertFalse(dao.getById(2)!!.isOnWidget)
            // Reconciliation is idempotent, including an empty host after restart.
            sync()
            assertFalse(dao.getById(1)!!.isOnWidget)
        } finally {
            listOf(first, second, stale).forEach(store::delete)
            database.close()
        }
    }
}
