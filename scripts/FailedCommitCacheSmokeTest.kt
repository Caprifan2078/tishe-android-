package ru.tishe.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Simulates commit=false AFTER changing the SharedPreferences in-memory value. */
@RunWith(AndroidJUnit4::class)
class FailedCommitCacheSmokeTest {
    @Test fun unconfirmedAnswerIsNotAdoptedAfterCommitFailure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val isolatedName = "tishe_qa_simulated_write_failure"
        val real = context.getSharedPreferences(isolatedName, Context.MODE_PRIVATE)
        real.edit().clear().commit()
        val failedContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return object : SharedPreferences by real {
                    override fun edit(): SharedPreferences.Editor {
                        val editor = real.edit()
                        return object : SharedPreferences.Editor by editor {
                            override fun commit(): Boolean {
                                // Model a failed disk commit whose new value is visible in RAM.
                                editor.apply()
                                return false
                            }
                        }
                    }
                }
            }
        }
        try {
            val store = SecureStorage(failedContext)
            val original = store.loadAssessment()
            val id = AssessmentJourney.questions(JourneyLevel.START).first().question.id
            val changed = if (original.answers[id] == 1) 2 else 1
            try {
                store.saveAssessment(original.copy(answers = original.answers + (id to changed)))
                fail("A failed commit must be reported")
            } catch (expected: IllegalStateException) {
                // The caller must see the persistence failure.
            }
            assertNotNull("Failed edit should be visible in SharedPreferences RAM", real.getString("encrypted", null))
            assertEquals("Never adopt a value without a confirmed disk write",
                original.answers[id], store.loadAssessment().answers[id])
        } finally {
            // Only this isolated QA file is touched; not the user's storage.
            real.edit().clear().commit()
            context.deleteSharedPreferences(isolatedName)
        }
    }
}
