package ru.missclick.wolfwisdom.data.local.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteDto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quote: String,
    @ColumnInfo(defaultValue = "0")
    val isSaved: Boolean = false,
    /** UTC epoch milliseconds; null when unsaved or the original save time is unknown. */
    @ColumnInfo(defaultValue = "NULL")
    val savedAt: Long? = null
)
