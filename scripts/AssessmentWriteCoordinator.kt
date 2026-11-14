package ru.tishe.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A process-local, ordered durable-save queue that is NOT owned by a Compose
 * Activity scope. Android rotation can cancel the old UI's await, but it must
 * never cancel an already accepted on-disk write.
 *
 * A freshly recreated Activity waits for queued writes BEFORE reading any
 * preferences. Otherwise, an old store could commit after the new store loaded
 * an older document and the next full-document save could erase an answer.
 *
 * No assessment responses are retained after a task completes, and no
 * plaintext is placed in SavedState, logs or persistent queue metadata.
 */
internal object AssessmentWriteCoordinator {
    private val lock = Any()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var last: Deferred<Unit>? = null
    private var failed = false

    /**
     * Call synchronously from main when the UI accepts a save, BEFORE launching
     * a UI-scoped waiter. Deferred.await() does not cancel this write if the
     * waiter is canceled by Activity recreation.
     */
    fun submit(write: () -> Unit): Deferred<Unit> {
        val request = CompletableDeferred<Unit>()
        val predecessor = synchronized(lock) {
            val previous = last
            last = request
            previous
        }
        io.launch {
            try {
                // join() waits for order but allows an explicit retry after failure.
                predecessor?.join()
                write()
                synchronized(lock) {
                    failed = false
                    if (last === request) last = null
                }
                request.complete(Unit)
            } catch (error: Throwable) {
                // A failed SharedPreferences.commit may expose unconfirmed RAM
                // data to a different SecureStorage instance in this process.
                // Block cold-start reads until a subsequently confirmed write,
                // rather than interpreting that RAM value as durable.
                synchronized(lock) {
                    failed = true
                    if (last === request) last = null
                }
                request.completeExceptionally(error)
            }
        }
        return request
    }

    /** Called off main, before constructing a fresh Activity's StartupSnapshot. */
    suspend fun awaitIdle() {
        while (true) {
            val pending = synchronized(lock) {
                if (last == null && failed) {
                    error("Assessment write was not confirmed; reload safely after process restart")
                }
                last
            } ?: return
            // Failure intentionally propagates into the existing recovery screen.
            pending.await()
        }
    }
}
