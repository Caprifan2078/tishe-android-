package ru.tishe.app

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** The storage backend is simulated; no user data or real keystore is touched. */
@RunWith(AndroidJUnit4::class)
class AssessmentAsyncWriterTest {
    @Test fun slowEncryptedWriteDoesNotBlockAndroidMainAndAcknowledgesOnlyAfterCompletion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val confirmed = AtomicBoolean(false)
        val onBackground = AtomicBoolean(false)
        val backendCalls = AtomicInteger(0)
        val writer = AssessmentAsyncWriter { _, durable ->
            assertTrue("Answers must use durable writes", durable)
            onBackground.set(Looper.myLooper() != Looper.getMainLooper())
            backendCalls.incrementAndGet()
            started.countDown()
            check(release.await(10, TimeUnit.SECONDS)) { "Timed out before releasing test write" }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            writer.save(AssessmentState(answers = mapOf("anxiety.1" to 3)))
            confirmed.set(true)
        }
        try {
            assertTrue("Write never started", started.await(10, TimeUnit.SECONDS))
            assertTrue("Writer ran on Android main thread", onBackground.get())
            assertFalse("UI confirmed before disk completed", confirmed.get())
            // This synchronous callback cannot execute while main is blocked.
            val responsive = AtomicBoolean(false)
            instrumentation.runOnMainSync { responsive.set(true) }
            assertTrue("Main thread did not respond during blocked IO", responsive.get())
            assertFalse("Unconfirmed data was exposed to the UI", confirmed.get())
        } finally {
            release.countDown()
        }
        runBlocking { withTimeout(10000) { job.join() } }
        assertTrue("UI did not receive the persistence acknowledgment", confirmed.get())
        assertEquals(1, backendCalls.get())
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    @Test fun failedWritePropagatesAndNeverReportsSuccess() {
        val confirmed = AtomicBoolean(false)
        val writer = AssessmentAsyncWriter { _, _ -> throw IllegalStateException("synthetic storage failure") }
        var failed = false
        runBlocking {
            try {
                writer.save(AssessmentState(answers = mapOf("anxiety.1" to 1)))
                confirmed.set(true)
            } catch (expected: IllegalStateException) {
                failed = true
            }
        }
        assertTrue("Failure must propagate to the UI error handler", failed)
        assertFalse("Failed save cannot be acknowledged", confirmed.get())
    }
}
