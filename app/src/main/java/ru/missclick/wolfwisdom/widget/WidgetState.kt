package ru.missclick.wolfwisdom.widget

import android.content.Context
import androidx.annotation.StringRes
import ru.missclick.wolfwisdom.R
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.flow.first
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.domain.util.Result

enum class WidgetTheme(@param:StringRes val labelRes: Int) {
    DARK(R.string.widget_theme_dark), LIGHT(R.string.widget_theme_light), ANDROID(R.string.widget_theme_android)
}

data class WidgetState(val quoteId: Long?, val theme: WidgetTheme = WidgetTheme.DARK,
                       val allowUnsaved: Boolean = false, val version: Int = 2)

/** Compact private instance metadata. Quotes remain exclusively in the existing Room repository. */
class WidgetStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("favorite_widgets", Context.MODE_PRIVATE)

    fun read(id: Int): WidgetState? {
        val raw = prefs.getString("instance.$id", null) ?: return null
        val fields = raw.split('|')
        if (!((fields.size == 3 && fields[0] == "1") || (fields.size == 4 && fields[0] == "2"))) return null
        val theme = WidgetTheme.entries.find { it.name == fields[2] } ?: return null
        val allowUnsaved = if (fields[0] == "2") fields[3].toBooleanStrictOrNull() ?: return null else false
        return WidgetState(fields[1].toLongOrNull()?.takeIf { it > 0 }, theme, allowUnsaved)
    }

    fun write(id: Int, state: WidgetState): Boolean = prefs.edit()
        .putString("instance.$id", encode(state)).commit()

    fun delete(id: Int) { prefs.edit().remove("instance.$id").commit() }

    fun restore(oldIds: IntArray, newIds: IntArray) {
        // Snapshot first: restored IDs can overlap IDs from the old host.
        val states = oldIds.map(::read)
        val editor = prefs.edit()
        oldIds.forEach { editor.remove("instance.$it") }
        oldIds.zip(newIds).forEachIndexed { index, (_, new) ->
            states[index]?.let { editor.putString("instance.$new", encode(it)) }
        }
        editor.commit()
    }

    companion object {
        private fun encode(state: WidgetState) = "${state.version}|${state.quoteId ?: ""}|${state.theme.name}|${state.allowUnsaved}"

        suspend fun initial(quoteId: Long? = null, repository: QuoteRepository = GlobalContext.get().get()): WidgetState {
            if (quoteId != null && quoteId > 0) return WidgetState(quoteId)
            return when (val result = repository.observeSavedQuotes().first()) {
                is Result.Success -> {
                    val favorite = result.data.maxWithOrNull(
                        compareBy<Quote> { it.savedAt ?: Long.MIN_VALUE }
                            .thenByDescending { it.id }
                    )
                    if (favorite != null) WidgetState(favorite.id) else when (val current = repository.getCurrentQuote()) {
                        is Result.Success -> WidgetState(current.data.id, allowUnsaved = true)
                        is Result.Failure -> throw WidgetDataException()
                    }
                }
                is Result.Failure -> throw WidgetDataException()
            }
        }
    }
}

class WidgetDataException : java.io.IOException("Quote storage unavailable")

data class WidgetQuote(val id: Long?, val text: String, val author: String, val hasQuote: Boolean)

suspend fun loadWidgetQuote(context: Context, state: WidgetState, allowUnsaved: Boolean = false,
                            repository: QuoteRepository = GlobalContext.get().get()): WidgetQuote {
    if (state.quoteId == null) return WidgetQuote(null, context.getString(R.string.widget_empty), "", false)
    return when (val result = repository.getQuoteById(state.quoteId)) {
        is Result.Success -> if (result.data.isSaved || state.allowUnsaved || allowUnsaved) {
            WidgetQuote(result.data.id, result.data.quote.ifBlank { context.getString(R.string.widget_quote_empty) }, context.getString(R.string.quote_author), true)
        } else WidgetQuote(null, context.getString(R.string.widget_quote_removed), context.getString(R.string.widget_open_saved), false)
        is Result.Failure -> WidgetQuote(null, context.getString(R.string.widget_quote_unavailable), context.getString(R.string.widget_open_saved), false)
    }
}
