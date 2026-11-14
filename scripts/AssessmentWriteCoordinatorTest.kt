package ru.tishe.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled IO, no personal user data or replacement of the real Keystore. */
@RunWith(AndroidJUnit4::class)
class AssessmentWriteCoordinatorTest {
    @Test fun canceledUiWaiterDoesNotCancelAcceptedDiskWrite() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val committed = AtomicBoolean(false)
        val acknowledged = AtomicBoolean(false)
        val request = AssessmentWriteCoordinator.submit {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            committed.set(true)
        }
        val ui = CoroutineScope(Dispatchers.Default)
        val waiter = ui.launch { request.await(); acknowledged.set(true) }
        val recreated = ui.async { AssessmentWriteCoordinator.awaitIdle() }
        try {
            assertTrue("IO write never started", entered.await(10, TimeUnit.SECONDS))
            waiter.cancel() // Activity was recreated before commit
            assertFalse("Acknowledged before disk save", acknowledged.get())
            assertFalse("Disk save ended unexpectedly", committed.get())
            assertFalse("New Activity loaded while write was pending", recreated.isCompleted)
        } finally {
            release.countDown()
        }
        runBlocking { withTimeout(12000L) { recreated.await() } }
        assertTrue("Accepted write did not finish", committed.get())
        assertFalse("Canceled UI was notified as if still visible", acknowledged.get())
        ui.cancel()
    }

    @Test fun rapidSubmissionsAreSerializedAndLastSnapshotWins() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondRan = AtomicBoolean(false)
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val first = AssessmentWriteCoordinator.submit {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            order.add(1)
        }
        assertTrue("First save did not start", entered.await(10, TimeUnit.SECONDS))
        val second = AssessmentWriteCoordinator.submit {
            secondRan.set(true)
            order.add(2)
        }
        try {
            assertFalse("Second save overtook the first", secondRan.get())
            assertFalse("Second save completed before first", second.isCompleted)
        } finally {
            release.countDown()
        }
        runBlocking {
            withTimeout(12000L) {
                first.await()
                second.await()
                AssessmentWriteCoordinator.awaitIdle()
            }
        }
        assertEquals(listOf(1, 2), order.toList())
    }

    @Test fun failedWriteCannotBeMistakenForPersistedAnswerOnRecreate() {
        val failed = AssessmentWriteCoordinator.submit {
            throw IllegalStateException("synthetic disk failure")
        }
        runBlocking {
            try {
                failed.await()
                fail("A failed write was treated as success")
            } catch (_: IllegalStateException) { }
            try {
                AssessmentWriteCoordinator.awaitIdle()
                fail("Fresh Activity must not read unconfirmed SharedPreferences RAM")
            } catch (_: IllegalStateException) { }
            // A new explicitly accepted write may recover the store and unlock reads.
            val retry = AssessmentWriteCoordinator.submit { }
            withTimeout(12000L) { retry.await() }
            withTimeout(12000L) { AssessmentWriteCoordinator.awaitIdle() }
        }
    }
}

/** True MainActivity recreation while another queued IO operation is blocked. */
@RunWith(AndroidJUnit4::class)
class AssessmentWriteRecreationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun recreatedActivityWaitsForPriorWriteBeforeRenderingOldState() {
        compose.waitUntil(30000L) {
            compose.onAllNodesWithTag("nav_profile").fetchSemanticsNodes().isNotEmpty()
        }
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val queued = AssessmentWriteCoordinator.submit {
            entered.countDown()
            check(release.await(12, TimeUnit.SECONDS))
        }
        assertTrue("IO didn't begin", entered.await(10, TimeUnit.SECONDS))
        try {
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onAllNodesWithTag("nav_profile").assertCountEquals(0)
        } finally {
            release.countDown()
        }
        runBlocking { withTimeout(15000L) { queued.await() } }
        compose.waitUntil(30000L) {
            compose.onAllNodesWithTag("nav_profile").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
