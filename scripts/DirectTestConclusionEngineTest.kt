package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class DirectTestConclusionEngineTest {
    @Test fun allFourJourneysHaveDirectResult() {
        for (level in JourneyLevel.entries) {
            val answers = AssessmentJourney.questions(level).associate { it.question.id to 4 }
            val result = DirectTestConclusionEngine.journey(level, answers)
            assertTrue(result.headline.contains("часто возникают трудности"))
            assertTrue(result.nextStep.isNotBlank())
        }
    }
    @Test fun lowScoresDoNotCreateFalseProblems() {
        for (level in JourneyLevel.entries) {
            val answers = AssessmentJourney.questions(level).associate { it.question.id to 0 }
            val result = DirectTestConclusionEngine.journey(level, answers)
            assertTrue(result.headline.contains("не отметили частых трудностей"))
        }
    }
    @Test fun angerEvidenceAndMissingInputs() {
        val zero = AssessmentJourney.questions(JourneyLevel.START).associate { it.question.id to 0 }
        val high = DirectTestConclusionEngine.journey(JourneyLevel.START, zero +
            mapOf("anger.1" to 4, "anger.2" to 4, "anger.3" to 4))
        assertTrue(high.headline.contains("гневом"))
        assertTrue(high.explanation.contains("3 из 4"))
        val partial = DirectTestConclusionEngine.journey(JourneyLevel.START,
            mapOf("anxiety.1" to 4, "anxiety.2" to 4, "anxiety.3" to 4))
        assertTrue(partial.headline.contains("предварительный"))
        assertTrue(partial.explanation.contains("3 из 3"))
        val invalid = DirectTestConclusionEngine.journey(JourneyLevel.START,
            mapOf("anxiety.1" to 400, "anger.1" to -4))
        assertTrue(invalid.headline.contains("не пройден"))
    }
    @Test fun everyIndividualSurveyHasConclusion() {
        for (m in AssessmentCatalog.modules) {
            val value = if (m.evidenceBased) 3 else 4
            val r = DirectTestConclusionEngine.single(m, m.questions.associate { it.id to value })
            assertTrue(m.id, r.headline.isNotBlank())
            assertTrue(m.id, r.explanation.isNotBlank())
            assertTrue(m.id, r.nextStep.isNotBlank())
        }
    }
    @Test fun positiveInputsAreNotInventedProblems() {
        val values = AssessmentCatalog.find("values")!!
        val r = DirectTestConclusionEngine.single(values, values.questions.associate { it.id to 4 })
        assertTrue(r.headline.contains("близки"))
        val mixed = AssessmentCatalog.find("low_energy_paths")!!
        val context = DirectTestConclusionEngine.single(mixed, mixed.questions.associate { it.id to 4 })
        assertTrue(context.headline.contains("заметна тема"))
    }
    @Test fun phq4RequiresAllAnswersAndIsNotDiagnosis() {
        val m = AssessmentCatalog.find("phq4")!!
        val partial = DirectTestConclusionEngine.single(m, mapOf("phq4.1" to 3))
        assertTrue(partial.headline.contains("не завершён"))
        val full = DirectTestConclusionEngine.single(m, m.questions.associate { it.id to 3 })
        assertTrue(full.headline.contains("PHQ-4"))
        assertTrue(full.explanation.contains("не диагноз"))
    }
    @Test fun ipipRequiresFullAnswersAndDoesNotAssignTypes() {
        val m = AssessmentCatalog.find("ipip_stability")!!
        val partial = DirectTestConclusionEngine.single(m, m.questions.dropLast(1).associate { it.id to 4 })
        assertTrue(partial.headline.contains("не завершено"))
        val full = DirectTestConclusionEngine.single(m, m.questions.associate { it.id to 4 })
        assertTrue(full.headline.contains("По вашим ответам") || full.headline.contains("неоднозначное"))
    }
    @Test fun previousAnswersRemainUntouched() {
        val answers = AssessmentJourney.questions(JourneyLevel.DEEP).associate { it.question.id to 2 }.toMutableMap()
        val snapshot = answers.toMap()
        for (level in JourneyLevel.entries) DirectTestConclusionEngine.journey(level, answers)
        for (module in AssessmentCatalog.modules) DirectTestConclusionEngine.single(module, answers)
        assertEquals(snapshot, answers)
    }
}
