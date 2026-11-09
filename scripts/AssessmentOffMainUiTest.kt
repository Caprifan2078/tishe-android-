package ru.tishe.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * QA-only end-to-end check. Real app UI and Keystore, isolated preference namespace.
 * The wrapped Editor rejects disk operations from Android's main thread.
 */
@RunWith(AndroidJUnit4::class)
class AssessmentOffMainUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun answersAndCursorAreSavedOffMainInOrder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testName = "tishe_qa_offmain_confirmed_v1"
        val real = context.getSharedPreferences(testName, Context.MODE_PRIVATE)
        assertTrue(real.edit().clear().commit())
        val commits = AtomicInteger(0)
        val applies = AtomicInteger(0)
        val checkedPreferences = object : SharedPreferences by real {
            override fun edit(): SharedPreferences.Editor {
                val editor = real.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                        editor.putString(key, value)
                        return this
                    }
                    override fun commit(): Boolean {
                        assertFalse("Assessment commit must not block Android main thread",
                            Looper.myLooper() == Looper.getMainLooper())
                        commits.incrementAndGet()
                        return editor.commit()
                    }
                    override fun apply() {
                        assertFalse("Even cursor-only encryption/apply must be off main thread",
                            Looper.myLooper() == Looper.getMainLooper())
                        applies.incrementAndGet()
                        editor.apply()
                    }
                }
            }
        }
        val isolatedContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                checkedPreferences
        }
        try {
            val storage = SecureStorage(isolatedContext)
            compose.setContent { TisheApp(storage) }
            compose.waitUntil(30000L) {
                compose.onAllNodesWithTag("nav_profile").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("nav_profile").performClick()
            compose.onNodeWithTag("profile_test_list").performScrollToNode(hasText("Начальный · 40"))
            compose.onNodeWithText("Начальный · 40").performClick()
            compose.waitUntil(30000L) {
                compose.onAllNodesWithText("вопрос 1 из 40", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Часто").performClick()
            compose.waitUntil(30000L) {
                compose.onAllNodesWithText("Отвечено: 1/40", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Дальше").performClick()
            compose.waitUntil(30000L) {
                compose.onAllNodesWithText("вопрос 2 из 40", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Иногда").performClick()
            compose.waitUntil(30000L) {
                compose.onAllNodesWithText("Отвечено: 2/40", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val ids = AssessmentJourney.questions(JourneyLevel.START).map { it.question.id }
            assertEquals(3, storage.loadAssessment().answers[ids[0]])
            assertEquals(2, storage.loadAssessment().answers[ids[1]])
            assertTrue("Expected 3+ confirmed writes, including the opening checkpoint", commits.get() >= 3)
            assertTrue("Expected a cursor-only update", applies.get() >= 1)
        } finally {
            real.edit().clear().commit()
            context.deleteSharedPreferences(testName)
        }
    }
}
