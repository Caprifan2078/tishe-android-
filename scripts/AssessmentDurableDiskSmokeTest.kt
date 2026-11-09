package ru.tishe.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated QA package: assert ciphertext is on disk, not just in SharedPreferences RAM. */
@RunWith(AndroidJUnit4::class)
class AssessmentDurableDiskSmokeTest {
    @Test fun encryptedAnswerIsOnDiskWhenSaveReturns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = SecureStorage(context)
        val original = storage.loadAssessment()
        val id = AssessmentJourney.questions(JourneyLevel.START).first().question.id
        val changed = if (original.answers[id] == 1) 2 else 1
        val preferences = context.getSharedPreferences("tishe_private_v1", Context.MODE_PRIVATE)
        val xml = File(context.applicationInfo.dataDir, "shared_prefs/tishe_private_v1.xml")
        try {
            storage.saveAssessment(original.copy(answers = original.answers + (id to changed)))
            val ciphertext = preferences.getString("encrypted", null)
            assertNotNull("Missing ciphertext", ciphertext)
            assertTrue("SharedPreferences XML was not written", xml.isFile)
            assertTrue("Disk snapshot differs from the saved ciphertext", xml.readText().contains(ciphertext!!))
            assertEquals(changed, SecureStorage(context).loadAssessment().answers[id])
        } finally {
            storage.saveAssessment(original)
        }
    }
}
