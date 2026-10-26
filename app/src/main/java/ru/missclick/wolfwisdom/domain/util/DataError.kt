package ru.missclick.wolfwisdom.domain.util

sealed interface DataError : Error {
    enum class Local : DataError {
        /** There is not enough storage to persist quotes, saved state, or reading history. */
        DISK_FULL,

        /** A requested quote or persisted history entry does not exist. */
        NOT_FOUND,

        /** The local database cannot be opened, read, or updated. */
        DATABASE_ERROR,

        /** The bundled quote seed file cannot be opened or read. */
        SEED_READ_FAILED,

        /** The quote seed is empty, malformed, incorrectly numbered, or contains duplicates. */
        INVALID_SEED_DATA,

        /** An unexpected local failure that does not match a known category. */
        UNKNOWN
    }
}
