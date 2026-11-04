package ru.tishe.app

/**
 * Opening a completed assessment should show its result, not a stale last question.
 * These checks use only existing responses; they do not store, reset or mutate anything.
 */
internal object AssessmentResultEntryPolicy {
    fun answered(module: SurveyModule, answers: Map<String, Int>): Int {
        val range = if (module.evidenceBased) 0..3 else 0..4
        return module.questions.count { answers[it.id] in range }
    }

    fun completedModule(module: SurveyModule, answers: Map<String, Int>): Boolean =
        module.questions.isNotEmpty() && answered(module, answers) == module.questions.size

    fun completedJourney(level: JourneyLevel, answers: Map<String, Int>): Boolean =
        AssessmentJourney.progress(level, answers).complete
}
