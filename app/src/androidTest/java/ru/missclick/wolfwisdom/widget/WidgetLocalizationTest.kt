package ru.missclick.wolfwisdom.widget

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.missclick.wolfwisdom.R
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetLocalizationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun interfaceAndWidgetFallbacks_followLocaleWithoutChangingQuoteBinding() = runBlocking {
        for ((language, title, dark, author) in listOf(
            listOf("en", "Widget settings", "Dark", "— Wolf"),
            listOf("ru", "Настройка виджета", "Тёмная", "— Волк"),
            listOf("fr", "Widget settings", "Dark", "— Wolf")
        )) {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList(Locale.forLanguageTag(language)))
            })
            assertEquals(title, localized.getString(R.string.widget_configuration_title))
            assertEquals(dark, localized.getString(WidgetTheme.DARK.labelRes))
            assertEquals(author, localized.getString(R.string.quote_author))
            val state = WidgetState(null, WidgetTheme.ANDROID)
            val quote = loadWidgetQuote(localized, state)
            assertFalse(quote.hasQuote)
            assertEquals(localized.getString(R.string.widget_empty), quote.text)
            assertNull(state.quoteId)
            assertEquals(WidgetTheme.ANDROID, state.theme)
            assertEquals(localized.getString(R.string.widget_preview),
                if (language == "ru") "Предпросмотр" else "Preview")
        }
    }
}
