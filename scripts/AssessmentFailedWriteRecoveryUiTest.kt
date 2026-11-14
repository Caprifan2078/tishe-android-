package ru.tishe.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A failed commit must not lead to a false "saved" state after rotation.
 * This tests the REAL MainActivity recovery UI, with synthetic failing storage
 * and no personal data, resets, network, or key manipulation.
 */
@RunWith(AndroidJUnit4::class)
class AssessmentFailedWriteRecoveryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun unconfirmedWriteShowsActionableRecoveryWithoutInfiniteRetry() {
        compose.waitUntil(30000L) {
            compose.onAllNodesWithTag("nav_profile").fetchSemanticsNodes().isNotEmpty()
        }
        val failed = AssessmentWriteCoordinator.submit {
            throw IllegalStateException("synthetic commit=false")
        }
        runBlocking {
            try {
                withTimeout(10000L) { failed.await() }
                fail("A failed write must not complete successfully")
            } catch (_: IllegalStateException) {
                // The process-local gate is now poisoned until a confirmed write/restart.
            }
        }
        try {
            compose.activityRule.scenario.recreate()
            compose.waitUntil(30000L) {
                compose.onAllNodesWithText("Последняя запись не подтверждена")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Последняя запись не подтверждена").assertExists()
            compose.onNodeWithText("Открыть настройки приложения").assertExists()
            compose.onNodeWithText("Повторить загрузку").assertDoesNotExist()
            compose.onNodeWithText("Мне тяжело — помощь без записей").assertExists()
            compose.onNodeWithText("Срочная поддержка · без загрузки данных").assertExists()
            compose.onNodeWithText("Не очищай данные и не удаляй приложение.", substring = true)
                .assertExists()
        } finally {
            // Restore isolated test-process coordinator state to protect other tests.
            val good = AssessmentWriteCoordinator.submit { }
            runBlocking {
                withTimeout(10000L) {
                    good.await()
                    AssessmentWriteCoordinator.awaitIdle()
                }
            }
        }
    }
}
