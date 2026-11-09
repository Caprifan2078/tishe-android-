package ru.tishe.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Run slow encrypted snapshot writes outside Compose's main thread.
 * Completion returns only after SecureStorage's checked commit succeeds.
 * The parent UI serializes user input while this suspend function is active;
 * never use fire-and-forget write or assume SharedPreferences.apply is durable.
 */
internal class AssessmentAsyncWriter(
    private val write: (AssessmentState, Boolean) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun save(value: AssessmentState, durable: Boolean = true) {
        withContext(ioDispatcher) {
            write(value, durable)
        }
    }
}
