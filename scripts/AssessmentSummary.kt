package ru.tishe.app

/** Six descriptive groups actually asked in the short 40-question journey.
 *  These are retrospective answers (0..4), NEVER today's 18-axis compass (0..5).
 *  No guesswork, normalization to unrelated emotions, or silently persisted feelings.
 */
internal data class AnswerScale(
    val moduleId: String,
    val title: String,
    val answered: Int,
    val total: Int,
    val mean: Double?
)

internal object AssessmentSummary {
    val shortModuleIds = listOf("anxiety", "anger", "sadness", "apathy", "thoughts", "stress")

    fun scales(answers: Map<String, Int>): List<AnswerScale> =
        shortModuleIds.map { id ->
            val module = AssessmentCatalog.find(id)
                ?: error("Missing short assessment module: $id")
            val ratings = module.questions.mapNotNull { question ->
                answers[question.id]?.takeIf { it in 0..4 }
            }
            AnswerScale(
                moduleId = module.id,
                title = module.title,
                answered = ratings.size,
                total = module.questions.size,
                mean = if (ratings.size >= 2) ratings.average() else null
            )
        }

    fun availableCount(answers: Map<String, Int>): Int =
        scales(answers).count { it.mean != null }
}
