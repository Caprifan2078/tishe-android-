package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class JourneyReportEngineTest {
    @Test fun everyCompletedRouteProvidesARealConclusionAndScales() {
        for (level in JourneyLevel.entries) {
            val answers = AssessmentJourney.questions(level).associate { it.question.id to 4 }
            val report = JourneyReportEngine.report(level, answers)
            assertEquals(level.count, report.answered)
            assertEquals(level.count, report.total)
            assertTrue(report.completed)
            assertTrue(report.usableModules.isNotEmpty())
            assertEquals(6, report.modules.count {
                it.moduleId in AssessmentAnswerSummary.domains && it.average != null
            })
            assertTrue(report.observations.isNotEmpty())
            assertTrue(report.observations.first().contains("ответах"))
            assertTrue(report.modules.all { it.answered == it.total })
        }
    }

    @Test fun partialAndInvalidRatingsNeverGenerateInventedScores() {
        val answers = mapOf("anxiety.1" to 4, "anxiety.2" to 7, "emotion_layers.1" to 4)
        val report = JourneyReportEngine.report(JourneyLevel.START, answers)
        assertEquals(2, report.answered)
        assertNull(report.modules.first { it.moduleId == "anxiety" }.average)
        assertEquals(1, report.modules.first { it.moduleId == "emotion_layers" }.answered)
        assertFalse(report.completed)
    }

    @Test fun mixedDirectionTopicsAreReportedAsActualStatementsNotSeverity() {
        val module = AssessmentCatalog.find("emotion_layers")!!
        val report = JourneyReportEngine.single(module, module.questions.associate { it.id to 4 })
        assertNull(report.average)
        assertEquals(3, report.highlightedStatements.size)
        assertTrue(report.highlightedStatements.all { it in module.questions.map { q -> q.text } })
    }

    @Test fun ipipRequiresFullScaleAndReversesNegativelyKeyedQuestions() {
        val module = AssessmentCatalog.find("ipip_stability")!!
        val partial = module.questions.take(19).associate { it.id to 4 }
        assertNull(JourneyReportEngine.single(module, partial).average)
        val full = module.questions.associate { it.id to 4 }
        val mean = JourneyReportEngine.single(module, full).average!!
        val expected = module.questions.count { !it.reversed }.toDouble() * 4.0 / module.questions.size
        assertEquals(expected, mean, .00001)
    }

    @Test fun adultPhq4NeverLeaksIntoAnyOfFourJourneys() {
        for (level in JourneyLevel.entries) {
            val answers = AssessmentJourney.questions(level).associate { it.question.id to 1 } +
                mapOf("phq4.1" to 3, "phq4.2" to 3, "phq4.3" to 3, "phq4.4" to 3)
            assertTrue(JourneyReportEngine.report(level, answers).modules.none { it.moduleId == "phq4" })
        }
    }

    @Test fun nestedShortRouteUsesOnlyItsFortyQuestions() {
        val answers = AssessmentJourney.questions(JourneyLevel.DEEP).associate { it.question.id to 2 }
        val report = JourneyReportEngine.report(JourneyLevel.START, answers)
        assertEquals(40, report.total)
        assertEquals(40, report.answered)
        assertEquals(4, report.modules.first { it.moduleId == "emotion_layers" }.total)
    }

    @Test fun lowFrequencyAnswersStillGenerateCarefulResult() {
        val answers = AssessmentJourney.questions(JourneyLevel.START).associate { it.question.id to 0 }
        val report = JourneyReportEngine.report(JourneyLevel.START, answers)
        assertTrue(report.completed)
        assertTrue(report.observations.first().contains("трудностей нет"))
    }
}
