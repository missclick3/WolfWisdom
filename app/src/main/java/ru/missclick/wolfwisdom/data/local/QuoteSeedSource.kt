package ru.missclick.wolfwisdom.data.local

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.missclick.wolfwisdom.data.local.dto.QuoteDto

class QuoteSeedSource(private val assets: AssetManager) {
    suspend fun load(): List<QuoteDto> = withContext(Dispatchers.IO) {
        assets.open("quotes/volchi_citaty_renumbered.txt").bufferedReader(Charsets.UTF_8).use { reader ->
            val numberedQuote = Regex("""(\d+)\.\s+(.+)""")
            val quotes = reader.lineSequence()
                .map { it.trim().removePrefix("\uFEFF") }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, line ->
                    val match = requireNotNull(numberedQuote.matchEntire(line)) {
                        "Invalid seed quote at line ${index + 1}"
                    }
                    require(match.groupValues[1].toInt() == index + 1) {
                        "Seed quote numbering must be sequential"
                    }
                    QuoteDto(quote = match.groupValues[2].trim())
                }
                .toList()
            require(quotes.isNotEmpty()) { "The quote seed must not be empty" }
            require(quotes.map { it.quote }.distinct().size == quotes.size) {
                "The quote seed must not contain duplicate quotes"
            }
            quotes
        }
    }
}
