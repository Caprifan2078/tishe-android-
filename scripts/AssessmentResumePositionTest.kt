package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

/** Saved/invalid answers must not quietly skip a missing question. */
class AssessmentResumePositionTest {
    @Test fun resumeAllStandaloneModulesAtFirstMissingOrInvalidChoice() {
        for (module in AssessmentCatalog.modules) {
            val max = if (module.evidenceBased) 3 else 4
            val complete = module.questions.associate { it.id to max }
            assertEquals(module.id, 0,
                AssessmentResultEntryPolicy.firstUnansweredIndex(module, complete))
            if (module.questions.size >= 2) {
                val first = module.questions[0].id
                val second = module.questions[1].id
                assertEquals(module.id, 1,
                    AssessmentResultEntryPolicy.firstUnansweredIndex(module, complete - second))
                assertEquals(module.id, 0,
                    AssessmentResultEntryPolicy.firstUnansweredIndex(module, complete + (first to -1)))
                assertEquals(module.id, 1,
                    AssessmentResultEntryPolicy.firstUnansweredIndex(module, complete + (second to (max + 1))))
            }
        }
    }

    @Test fun noChangeToOriginalAnswersWhenCheckingResumeIndex() {
        val module = AssessmentCatalog.find("anxiety")!!
        val answers = mapOf("anxiety.1" to 4, "anxiety.2" to 2)
        val same = answers.toMap()
        assertEquals(2, AssessmentResultEntryPolicy.firstUnansweredIndex(module, answers))
        assertEquals(same, answers)
    }
}
