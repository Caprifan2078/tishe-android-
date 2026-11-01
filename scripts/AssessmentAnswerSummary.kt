package ru.tishe.app

import kotlin.math.roundToInt

/** Descriptive summaries of responses, not current emotions or clinical scores. */
internal data class AnswerDomain(
    val id: String,
    val label: String,
    val average: Double,
    val answered: Int,
    val total: Int
) {
    /** Convert 0–4 response scale to the 0–5 polygon drawing range. */
    val radarValue: Int get() = (average * 5.0 / 4.0).roundToInt().coerceIn(0, 5)
}

internal object AssessmentAnswerSummary {
    val domains: List<String> = listOf("anxiety", "anger", "sadness", "apathy", "thoughts", "stress")

    /** Require at least two valid answers per four-question domain; never impute missing values. */
    fun scales(answers: Map<String, Int>): List<AnswerDomain> = domains.mapNotNull { id ->
        val module = AssessmentCatalog.find(id) ?: return@mapNotNull null
        val valid = module.questions.mapNotNull { q -> answers[q.id]?.takeIf { it in 0..4 } }
        if (valid.size < 2) null
        else AnswerDomain(id, module.title, valid.average(), valid.size, module.questions.size)
    }

    fun firstJourneyCompleted(answers: Map<String, Int>): Boolean =
        AssessmentJourney.progress(JourneyLevel.START, answers).complete
}
