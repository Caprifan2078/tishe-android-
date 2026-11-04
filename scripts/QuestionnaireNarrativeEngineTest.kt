package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

/** Synthetic responses only; no personal data, no clinical cutoff for custom scales. */
class QuestionnaireNarrativeEngineTest {
    private fun complete(level: JourneyLevel, value: Int): Map<String, Int> =
        AssessmentJourney.questions(level).associate { it.question.id to value }

    @Test fun allFourCompletedJourneysProvideGroundedTextAndActions() {
        for (level in JourneyLevel.entries) {
            val answers = complete(level, 3)
            val result = QuestionnaireNarrativeEngine.forJourney(level, answers)
            assertNotNull(level.name, result.headline)
            assertTrue(level.name, result.overview.contains("ответов"))
            assertTrue(level.name, result.evidence.isNotEmpty())
            assertTrue(level.name, result.gentleSteps.isNotEmpty())
            assertTrue(level.name, result.limitations.isNotEmpty())
        }
    }

    @Test fun detailedEvidenceComesOnlyFromActualEndorsedItems() {
        val answers = complete(JourneyLevel.START, 0) +
                mapOf("anxiety.1" to 4, "anxiety.2" to 4, "anxiety.3" to 0, "anxiety.4" to 0)
        val result = QuestionnaireNarrativeEngine.forJourney(JourneyLevel.START, answers)
        val anxiety = result.evidence.firstOrNull { it.title.contains("Тревога") }
        assertNotNull(anxiety)
        assertTrue(anxiety!!.observation.contains("2 из 4"))
        assertTrue(anxiety.source.contains("возможные неприятности"))
        assertTrue(anxiety.source.contains("переключиться"))
        assertFalse(anxiety.source.contains("напряжение в теле"))
        assertTrue(result.limitations.any { it.contains("различаются") })
    }

    @Test fun lowAnswersDoNotMeanHealthyOrNoProblems() {
        val result = QuestionnaireNarrativeEngine.forJourney(JourneyLevel.START, complete(JourneyLevel.START, 0))
        assertTrue(result.overview.contains("не исключает"))
        assertTrue(result.evidence.isEmpty())
        assertTrue(result.limitations.any { it.contains("не являются валидированным") })
    }

    @Test fun incompleteDoesNotSilentlyImputeAnswers() {
        val result = QuestionnaireNarrativeEngine.forJourney(JourneyLevel.START, mapOf("anxiety.1" to 4))
        assertTrue(result.limitations.any { it.contains("предварительный") })
        assertTrue(result.evidence.isEmpty())
        assertFalse(result.overview.contains("чаще других встречаются высокие"))
    }

    @Test fun resourcesAreNotIncorrectlyLabeledAsStrengths() {
        val answers = complete(JourneyLevel.START, 0) +
                mapOf("resources.1" to 4, "resources.2" to 4, "values.1" to 4, "values.4" to 4)
        val result = QuestionnaireNarrativeEngine.forJourney(JourneyLevel.START, answers)
        assertTrue(result.additionalContext.any { it.contains("нагрузки") && it.contains("недосып") })
        assertTrue(result.additionalContext.any { it.contains("опоры") })
        assertTrue(result.additionalContext.none { it.contains("причина") && it.contains("доказана") })
    }

    @Test fun missingOrOutOfRangeAnswersCannotGenerateScale() {
        val result = QuestionnaireNarrativeEngine.forJourney(JourneyLevel.START,
            mapOf("anxiety.1" to 44, "stress.1" to -4, "unknown.1" to 4))
        assertTrue(result.overview.contains("без ответов") || result.overview.contains("нет ответов"))
        assertTrue(result.evidence.isEmpty())
    }

    @Test fun mixedDirectionSingleModuleNeverInventsSeverity() {
        val module = AssessmentCatalog.find("emotion_layers")!!
        val answers = module.questions.associate { it.id to 4 }
        val result = QuestionnaireNarrativeEngine.forSingle(module, answers)
        assertTrue(result.overview.contains("конкретных ответов"))
        assertTrue(result.limitations.any { it.contains("диагноз") })
        assertTrue(result.evidence.first().source.contains(module.questions.first().text))
    }

    @Test fun ipipIsNotPresentedAsDiagnosisAndNeedsAllAnswers() {
        val module = AssessmentCatalog.find("ipip_stability")!!
        val partial = QuestionnaireNarrativeEngine.forSingle(
            module, module.questions.dropLast(1).associate { it.id to 4 })
        assertTrue(partial.overview.contains("все вопросы"))
        val full = QuestionnaireNarrativeEngine.forSingle(module, module.questions.associate { it.id to 4 })
        assertTrue(full.overview.contains("обратного подсчёта"))
        assertTrue(full.limitations.any { it.contains("сравнительных норм нет") })
        assertFalse(full.overview.contains("ты тревожный человек"))
    }

    @Test fun phqFourHandlesCompleteAndIncompleteWithoutDiagnosis() {
        val module = AssessmentCatalog.find("phq4")!!
        val incomplete = QuestionnaireNarrativeEngine.forSingle(module,
            mapOf("phq4.1" to 3, "phq4.2" to 3, "phq4.3" to 3))
        assertTrue(incomplete.overview.contains("не рассчитывается"))
        val high = QuestionnaireNarrativeEngine.forSingle(module,
            mapOf("phq4.1" to 3, "phq4.2" to 3, "phq4.3" to 3, "phq4.4" to 3))
        assertTrue(high.overview.contains("не диагноз"))
        assertTrue(high.gentleSteps.any { it.contains("специалистом") })
        assertEquals("PHQ-4: краткий скрининг", high.headline)
        val low = QuestionnaireNarrativeEngine.forSingle(module,
            mapOf("phq4.1" to 0, "phq4.2" to 0, "phq4.3" to 0, "phq4.4" to 0))
        assertTrue(low.overview.contains("не исключает"))
    }

    @Test fun previousSavedAnswersCanBeReusedWithoutMutation() {
        val answers = complete(JourneyLevel.DEEP, 2).toMutableMap()
        val copy = answers.toMap()
        for (level in JourneyLevel.entries) {
            QuestionnaireNarrativeEngine.forJourney(level, answers)
        }
        assertEquals(copy, answers)
    }
}
