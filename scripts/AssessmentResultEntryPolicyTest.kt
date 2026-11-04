package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class AssessmentResultEntryPolicyTest {
    private fun answers(level: JourneyLevel): Map<String, Int> =
        AssessmentJourney.questions(level).associate { it.question.id to 2 }

    @Test fun allFourCompletedRoutesOpenTheirReports() {
        for (level in JourneyLevel.entries) {
            assertTrue(level.name, AssessmentResultEntryPolicy.completedJourney(level, answers(level)))
        }
    }

    @Test fun incompleteRoutesKeepQuestionNavigation() {
        for (level in JourneyLevel.entries) {
            val all = answers(level)
            val omit = AssessmentJourney.questions(level).last().question.id
            assertFalse(level.name, AssessmentResultEntryPolicy.completedJourney(level, all - omit))
            assertFalse(level.name, AssessmentResultEntryPolicy.completedJourney(level, emptyMap()))
        }
    }

    @Test fun invalidStoredChoiceDoesNotCountAsCompleted() {
        for (level in JourneyLevel.entries) {
            val all = answers(level)
            val changedId = AssessmentJourney.questions(level).first().question.id
            assertFalse(AssessmentResultEntryPolicy.completedJourney(level, all + (changedId to 9)))
        }
    }

    @Test fun standaloneModulesAlsoOpenReportsAfterCompletion() {
        for (module in AssessmentCatalog.modules) {
            val max = if (module.evidenceBased) 3 else 4
            val complete = module.questions.associate { it.id to max }
            assertTrue(module.id, AssessmentResultEntryPolicy.completedModule(module, complete))
            assertEquals(module.questions.size, AssessmentResultEntryPolicy.answered(module, complete))
            assertFalse(module.id, AssessmentResultEntryPolicy.completedModule(module,
                complete - module.questions.last().id))
        }
    }

    @Test fun phq4MustRespectItsOwnThreePointMaximum() {
        val phq = AssessmentCatalog.find("phq4")!!
        val valid = phq.questions.associate { it.id to 3 }
        assertTrue(AssessmentResultEntryPolicy.completedModule(phq, valid))
        assertEquals(3, AssessmentResultEntryPolicy.answered(phq,
            valid + ("phq4.1" to 4)))
        assertFalse(AssessmentResultEntryPolicy.completedModule(phq,
            valid + ("phq4.1" to 4)))
    }

    @Test fun noAnswersAreAlteredByResultEntryCheck() {
        val saved = answers(JourneyLevel.DEEP).toMutableMap()
        val copy = saved.toMap()
        for (level in JourneyLevel.entries) {
            AssessmentResultEntryPolicy.completedJourney(level, saved)
        }
        for (module in AssessmentCatalog.modules) {
            AssessmentResultEntryPolicy.completedModule(module, saved)
        }
        assertEquals(copy, saved)
    }
}
