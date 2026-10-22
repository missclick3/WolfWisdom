package ru.missclick.wolfwisdom

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.missclick.wolfwisdom.data.di.quoteDataModule
import ru.missclick.wolfwisdom.presentation.main.di.mainModule
import ru.missclick.wolfwisdom.presentation.saved.di.savedModule

class WolfWisdomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SeasonalIconManager.initialize(this)
        startKoin {
            androidContext(this@WolfWisdomApplication)
            modules(quoteDataModule, mainModule, savedModule)
        }
    }
}
