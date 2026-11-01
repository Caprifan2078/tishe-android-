package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class AssessmentSummaryTest {
    @Test fun allFortyAnswersProduceExactlySixRealScales() {
        val questions = AssessmentJourney.questions(JourneyLevel.START)
        assertEquals(40, questions.size)
        val answers = questions.associate { it.question.id to 3 }
        val scales = AssessmentSummary.scales(answers)
        assertEquals(6, scales.size)
        assertEquals(6, scales.count { it.mean != null })
        scales.forEach {
            assertEquals(4, it.total)
            assertEquals(4, it.answered)
            assertEquals(3.0, it.mean!!, 0.0001)
        }
        assertEquals(40, AssessmentJourney.progress(JourneyLevel.START, answers).answered)
        assertTrue(answers.keys.none { it.startsWith("phq4.") })
    }

    @Test fun noAnswerNeverCreatesFakeEmotions() {
        val scales = AssessmentSummary.scales(emptyMap())
        assertEquals(6, scales.size)
        assertTrue(scales.all { it.answered == 0 && it.mean == null })
        assertEquals(0, AssessmentSummary.availableCount(emptyMap()))
    }

    @Test fun oneAnswerIsNotRepresentedAsCompletedScale() {
        val answers = mapOf("anxiety.1" to 4)
        val scales = AssessmentSummary.scales(answers)
        assertEquals(1, scales.first().answered)
        assertNull(scales.first().mean)
    }

    @Test fun editsToAnAnswerAreImmediatelyReflected() {
        val answers = mapOf("anxiety.1" to 0, "anxiety.2" to 4)
        val before = AssessmentSummary.scales(answers).first().mean
        val after = AssessmentSummary.scales(answers + ("anxiety.1" to 4)).first().mean
        assertEquals(2.0, before!!, .0001)
        assertEquals(4.0, after!!, .0001)
    }

    @Test fun invalidRatingsCannotFillScale() {
        val answers = mapOf("anger.1" to 7, "anger.2" to -1, "anger.3" to 1)
        val scale = AssessmentSummary.scales(answers).first { it.moduleId == "anger" }
        assertEquals(1, scale.answered)
        assertNull(scale.mean)
    }
}
