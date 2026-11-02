package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class SuperCompassSignalsTest {
    @Test fun noResponsesDoNotCreateImaginaryProblemsOrTrauma() {
        assertTrue(SuperCompassSignals.themes(emptyMap(), emptyMap()).isEmpty())
        assertTrue(SuperCompassSignals.noticedResources(emptyMap()).isEmpty())
    }

    @Test fun honestHighCurrentFeelingsCreateExplainableThemes() {
        val themes = SuperCompassSignals.themes(
            mapOf("fatigue" to 5, "anxiety" to 4, "shame" to 4), emptyMap())
        assertEquals(listOf("rest", "worry", "self_kindness"), themes.map { it.id })
        assertTrue(themes.first().evidence.contains("Истощение 5/5"))
        assertFalse(themes.any { it.title.contains("травм", ignoreCase = true) })
    }

    @Test fun finishedFortyQuestionsCanShowThemesWithoutMoodEntries() {
        val answers = AssessmentJourney.questions(JourneyLevel.START)
            .associate { it.question.id to 4 }
        assertEquals(40, answers.size)
        val themes = SuperCompassSignals.themes(emptyMap(), answers)
        assertTrue(themes.isNotEmpty())
        assertTrue(themes.all { it.evidence.contains("В ответах за 7 дней") })
        assertTrue(themes.size <= 3)
    }

    @Test fun incompleteSurveyDoesNotClaimConfirmedInsight() {
        val answers = mapOf("anxiety.1" to 4, "anxiety.2" to 4)
        assertTrue(SuperCompassSignals.themes(emptyMap(), answers).isEmpty())
    }

    @Test fun invalidRatingsNeverCreateTheme() {
        val answers = mapOf("stress.1" to 6, "stress.2" to -1, "stress.3" to 12)
        assertTrue(SuperCompassSignals.themes(emptyMap(), answers).isEmpty())
    }

    @Test fun notesAreNotWrittenAndInputsAreNotMutated() {
        val feelings = mutableMapOf("fear" to 5, "hope" to 4)
        val answers = mutableMapOf("stress.1" to 4, "stress.2" to 4, "stress.3" to 4)
        val beforeFeelings = feelings.toMap()
        val beforeAnswers = answers.toMap()
        val result = SuperCompassSignals.themes(feelings, answers)
        assertEquals(beforeFeelings, feelings)
        assertEquals(beforeAnswers, answers)
        assertTrue(result.isNotEmpty())
        assertEquals(listOf("hope"), SuperCompassSignals.noticedResources(feelings).map { it.id })
    }

    @Test fun totalAxesAndSurveyDimensionsAreNotConfused() {
        assertEquals(18, EmotionCompass.axes.size)
        assertEquals(6, AssessmentAnswerSummary.domains.size)
        assertTrue(EmotionCompass.axes.none { it.id == "thoughts" })
    }

    @Test fun positiveFeelingsDoNotCancelDifficultFeelings() {
        val feelings = mapOf("joy" to 5, "fear" to 5)
        assertTrue(SuperCompassSignals.themes(feelings, emptyMap()).any { it.id == "worry" })
        assertEquals(listOf("joy"), SuperCompassSignals.noticedResources(feelings).map { it.id })
    }
}
