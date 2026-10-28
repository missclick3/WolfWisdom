package ru.missclick.wolfwisdom

import ru.missclick.wolfwisdom.analytics.AnalyticsConsent
import ru.missclick.wolfwisdom.analytics.analyticsModule

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.missclick.wolfwisdom.data.di.quoteDataModule
import ru.missclick.wolfwisdom.presentation.main.di.mainModule
import ru.missclick.wolfwisdom.presentation.saved.di.savedModule
import ru.missclick.wolfwisdom.widget.WidgetUpdates

class WolfWisdomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsConsent.initialize(this)
        SeasonalIconManager.initialize(this)
        startKoin {
            androidContext(this@WolfWisdomApplication)
            modules(quoteDataModule, mainModule, savedModule, analyticsModule)
        }
        WidgetUpdates.initialize(this)
    }
}
