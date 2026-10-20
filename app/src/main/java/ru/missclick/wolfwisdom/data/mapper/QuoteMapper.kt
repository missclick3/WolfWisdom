package ru.missclick.wolfwisdom.data.mapper

import ru.missclick.wolfwisdom.data.local.dto.QuoteDto
import ru.missclick.wolfwisdom.domain.model.Quote

fun QuoteDto.toDomain(): Quote = Quote(id = id, quote = quote, isSaved = isSaved)
