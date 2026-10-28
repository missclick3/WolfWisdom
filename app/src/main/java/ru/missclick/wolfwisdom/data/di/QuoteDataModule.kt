package ru.missclick.wolfwisdom.data.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.missclick.wolfwisdom.data.local.QuoteSeedSource
import ru.missclick.wolfwisdom.data.local.WolfWisdomDatabase
import ru.missclick.wolfwisdom.data.repository.RoomQuoteRepository
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository

val quoteDataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            WolfWisdomDatabase::class.java,
            "wolf_wisdom.db"
        ).addMigrations(WolfWisdomDatabase.MIGRATION_1_2, WolfWisdomDatabase.MIGRATION_2_3,
            WolfWisdomDatabase.MIGRATION_3_4).build()
    }
    single { get<WolfWisdomDatabase>().quoteDao() }
    single { get<WolfWisdomDatabase>().quoteNavigationDao() }
    single { QuoteSeedSource(androidContext().assets) }
    single<QuoteRepository> { RoomQuoteRepository(get(), get()) }
}
