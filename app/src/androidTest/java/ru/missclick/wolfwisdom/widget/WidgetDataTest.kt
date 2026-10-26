package ru.missclick.wolfwisdom.widget

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.data.local.QuoteSeedSource
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.data.repository.RoomQuoteRepository

@RunWith(AndroidJUnit4::class)
class WidgetDataTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun binding_keepsIdentityThroughEditsRemovalAndDeletion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WolfWisdomDatabase::class.java).build()
        try {
            db.quoteDao().insertAll(listOf(QuoteDto(1, "Первый оригинал", true, 10), QuoteDto(2, "Второй оригинал", true, 20)))
            val repo = RoomQuoteRepository(db, QuoteSeedSource(context.assets))
            assertEquals(2L, WidgetStateStore.initial(repository = repo).quoteId)
            assertEquals(1L, WidgetStateStore.initial(1L, repo).quoteId)
            val bound = WidgetState(1)
            assertEquals("Первый оригинал", loadWidgetQuote(context, bound, repository = repo).text)
            db.openHelper.writableDatabase.execSQL("UPDATE quotes SET quote = 'Изменённый оригинал' WHERE id = 1")
            assertEquals("Изменённый оригинал", loadWidgetQuote(context, bound, repository = repo).text)
            repo.setSaved(1, false)
            assertFalse(loadWidgetQuote(context, bound, repository = repo).hasQuote)
            assertNull(loadWidgetQuote(context, bound, repository = repo).id)
            db.openHelper.writableDatabase.execSQL("DELETE FROM quotes WHERE id = 1")
            assertFalse(loadWidgetQuote(context, bound, repository = repo).hasQuote)
            assertEquals(1L, bound.quoteId)
            repo.setSaved(2, false)
            val fallback = WidgetStateStore.initial(repository = repo)
            assertEquals(2L, fallback.quoteId)
            assertTrue(fallback.allowUnsaved)
            assertEquals("Второй оригинал", loadWidgetQuote(context, fallback, repository = repo).text)
        } finally { db.close() }
    }

    @Test fun emptyFavorites_usesCurrentQuoteWithoutSavingOrFollowingLaterMainNavigation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WolfWisdomDatabase::class.java).build()
        try {
            db.quoteDao().insertAll(listOf(QuoteDto(1, "Один"), QuoteDto(2, "Два")))
            val repo = RoomQuoteRepository(db, QuoteSeedSource(context.assets))
            val current = (repo.getCurrentQuote() as ru.missclick.wolfwisdom.domain.util.Result.Success).data
            val bound = WidgetStateStore.initial(repository = repo)
            assertEquals(current.id, bound.quoteId)
            assertTrue(loadWidgetQuote(context, bound, repository = repo).hasQuote)
            assertFalse(db.quoteDao().getById(current.id)!!.isSaved)
            repo.nextQuote()
            assertEquals(current.quote, loadWidgetQuote(context, bound, repository = repo).text)
            val otherId = if (current.id == 1L) 2L else 1L
            repo.setSaved(otherId, true)
            assertEquals(WidgetState(otherId), WidgetStateStore.initial(repository = repo))
        } finally { db.close() }
    }
}
