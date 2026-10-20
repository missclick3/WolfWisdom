package ru.missclick.wolfwisdom.data.local.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Quotes already generated in the current shuffled cycle, independent of the cursor. */
@Entity(
    tableName = "quote_cycle",
    foreignKeys = [ForeignKey(
        entity = QuoteDto::class,
        parentColumns = ["id"],
        childColumns = ["quoteId"],
        onDelete = ForeignKey.RESTRICT
    )]
)
data class QuoteCycleEntryDto(
    @PrimaryKey
    val quoteId: Long
)
