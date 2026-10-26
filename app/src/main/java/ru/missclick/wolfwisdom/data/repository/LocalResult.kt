package ru.missclick.wolfwisdom.data.repository

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.missclick.wolfwisdom.domain.util.DataError
import ru.missclick.wolfwisdom.domain.util.Result

internal class LocalDataException(
    val error: DataError.Local,
    cause: Throwable? = null
) : Exception(error.name, cause)

internal fun Throwable.toLocalError(): DataError.Local = when (this) {
    is CancellationException -> throw this
    is LocalDataException -> error
    is SQLiteFullException -> DataError.Local.DISK_FULL
    is SQLiteException -> DataError.Local.DATABASE_ERROR
    else -> DataError.Local.UNKNOWN
}

internal suspend fun <T> localResult(block: suspend () -> T): Result<T, DataError.Local> =
    try {
        Result.Success(block())
    } catch (error: Exception) {
        Result.Failure(error.toLocalError())
    }

internal fun <T> Flow<T>.asLocalResult(): Flow<Result<T, DataError.Local>> =
    map<T, Result<T, DataError.Local>> { Result.Success(it) }
        .catch { error ->
            if (error !is Exception) throw error
            emit(Result.Failure(error.toLocalError()))
        }
