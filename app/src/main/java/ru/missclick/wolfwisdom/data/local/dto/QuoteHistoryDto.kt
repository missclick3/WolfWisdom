package ru.missclick.wolfwisdom.data.local.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quote_history",
    foreignKeys = [ForeignKey(
        entity = QuoteDto::class,
        parentColumns = ["id"],
        childColumns = ["quoteId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("quoteId")]
)
data class QuoteHistoryDto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quoteId: Long
)
