package ru.missclick.wolfwisdom.data.local.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** The singleton row is the persisted cursor into the append-only reading history. */
@Entity(
    tableName = "quote_reading_state",
    foreignKeys = [ForeignKey(
        entity = QuoteHistoryDto::class,
        parentColumns = ["id"],
        childColumns = ["currentHistoryId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("currentHistoryId")]
)
data class QuoteReadingStateDto(
    @PrimaryKey
    val id: Int = 1,
    val currentHistoryId: Long? = null
)
