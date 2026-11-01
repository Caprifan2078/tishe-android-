package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class AssessmentAnswerSummaryTest {
    private val all40 = AssessmentJourney.questions(JourneyLevel.START)
        .associate { it.question.id to 3 }

    @Test fun fortyAnswersPopulateSixRealScalesWithoutReentry() {
        assertEquals(40, all40.size)
        assertTrue(AssessmentAnswerSummary.firstJourneyCompleted(all40))
        val scales = AssessmentAnswerSummary.scales(all40)
        assertEquals(6, scales.size)
        assertEquals(AssessmentAnswerSummary.domains, scales.map { it.id })
        assertTrue(scales.all { it.average == 3.0 && it.answered == 4 && it.radarValue == 4 })
    }

    @Test fun unfinishedOrInvalidModulesCannotCreateMadeUpScores() {
        val partial = mapOf("anxiety.1" to 4, "anger.1" to -5, "anger.2" to 2)
        assertTrue(AssessmentAnswerSummary.scales(partial).isEmpty())
        assertFalse(AssessmentAnswerSummary.firstJourneyCompleted(partial))
    }

    @Test fun twoAnswersProducePartialClearlyMarkedScore() {
        val result = AssessmentAnswerSummary.scales(mapOf("sadness.1" to 2, "sadness.2" to 4))
        assertEquals(1, result.size)
        assertEquals("sadness", result.single().id)
        assertEquals(2, result.single().answered)
        assertEquals(4, result.single().total)
        assertEquals(3.0, result.single().average, 0.000001)
    }

    @Test fun responseChangesRecalculateScoreWithoutChangingSourceAnswers() {
        val changed = all40 + ("anxiety.1" to 0)
        assertEquals(3.0, AssessmentAnswerSummary.scales(all40).first().average, 0.0001)
        assertEquals(2.25, AssessmentAnswerSummary.scales(changed).first().average, 0.0001)
        assertEquals(3, all40.getValue("anxiety.1"))
    }

    @Test fun resourcesAndValuesDoNotGetMistakenForEmotionAxes() {
        val onlyOther = all40.filterKeys { it.startsWith("resources") || it.startsWith("values") }
        assertTrue(AssessmentAnswerSummary.scales(onlyOther).isEmpty())
    }
}
