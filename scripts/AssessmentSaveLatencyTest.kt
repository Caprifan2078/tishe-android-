package ru.tishe.app

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicLong
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * QA-only measurement of the real main-thread AES-GCM + SharedPreferences.commit() path.
 * Uses independent QA preferences, never the app user's encrypted document.
 * Reports measurements; does not claim emulator timings represent physical devices.
 */
@RunWith(AndroidJUnit4::class)
class AssessmentSaveLatencyTest {
    private fun saveOnMain(
        instrumentation: Instrumentation,
        storage: SecureStorage,
        assessment: AssessmentState
    ): Double {
        val nanos = AtomicLong(-1L)
        instrumentation.runOnMainSync {
            val started = SystemClock.elapsedRealtimeNanos()
            storage.saveAssessment(assessment)
            nanos.set(SystemClock.elapsedRealtimeNanos() - started)
        }
        assertTrue("Main-thread save timing was not captured", nanos.get() > 0L)
        return nanos.get() / 1_000_000.0
    }

    @Test fun benchmarkDurableWritesWithFortyToFiveHundredAnswers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val qaName = "tishe_qa_assessment_latency_v1"
        val preferences = context.getSharedPreferences(qaName, Context.MODE_PRIVATE)
        assertTrue("QA pre-test reset failed", preferences.edit().clear().commit())
        val isolatedContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                context.getSharedPreferences(qaName, mode)
        }

        val metrics = JSONArray()
        try {
            val storage = SecureStorage(isolatedContext)
            for (level in JourneyLevel.entries) {
                val ids = AssessmentJourney.questions(level).map { it.question.id }
                assertEquals(level.name, level.count, ids.size)
                assertEquals(level.name, ids.size, ids.distinct().size)
                val changingId = ids.last()
                var answers = ids.associateWith { 2 }
                saveOnMain(instrumentation, storage, AssessmentState(answers = answers))

                // Warm up the crypto/provider and filesystem without recording their timings.
                repeat(4) { index ->
                    answers = answers + (changingId to if (index % 2 == 0) 1 else 3)
                    saveOnMain(instrumentation, storage, AssessmentState(answers = answers))
                }

                val observations = mutableListOf<Double>()
                repeat(32) { index ->
                    // Alternating valid answers guarantees a real changed encrypted snapshot.
                    answers = answers + (changingId to if (index % 2 == 0) 1 else 3)
                    val elapsed = saveOnMain(instrumentation, storage, AssessmentState(answers = answers))
                    observations.add(elapsed)
                    assertEquals(
                        "The latest write must be readable after the committed save",
                        answers[changingId], storage.loadAssessment().answers[changingId]
                    )
                }
                val sorted = observations.sorted()
                fun ms(value: Double): Double = kotlin.math.round(value * 1000.0) / 1000.0
                val item = JSONObject()
                    .put("questionCount", level.count)
                    .put("samples", sorted.size)
                    .put("medianMs", ms((sorted[15] + sorted[16]) / 2.0))
                    .put("p95Ms", ms(sorted[30]))
                    .put("maxMs", ms(sorted.last()))
                    .put("minMs", ms(sorted.first()))
                metrics.put(item)
                println("TISHE_QA_WRITE_LATENCY " + item.toString())
            }

            val report = JSONObject()
                .put("format", "tishe-assessment-main-thread-latency-v1")
                .put("androidApi", Build.VERSION.SDK_INT)
                .put("deviceModel", Build.MODEL)
                .put("manufacturer", Build.MANUFACTURER)
                .put("samplesPerSize", 32)
                .put("warmupPerSize", 4)
                .put("includes", "AES-GCM, JSON serialization, SharedPreferences.commit")
                .put("excludes", "Compose layout, input dispatch, map copy")
                .put("measurements", metrics)
            context.openFileOutput("qa-assessment-latency.json", Context.MODE_PRIVATE).use {
                it.write(report.toString(2).toByteArray(Charsets.UTF_8))
            }
            assertEquals(4, metrics.length())
        } finally {
            // Do not touch the real "tishe_private_v1" preferences or personal records.
            assertTrue("QA cleanup failed", preferences.edit().clear().commit())
            context.deleteSharedPreferences(qaName)
        }
    }
}
