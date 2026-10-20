package ru.missclick.wolfwisdom.domain.model

data class Quote(
    val id: Long,
    val quote: String,
    val isSaved: Boolean = false,
    val savedAt: Long? = null
)
