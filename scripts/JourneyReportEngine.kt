package ru.tishe.app

/** Exploratory self-report, never a medical diagnosis or a validated severity scale. */
internal data class JourneyModuleResult(
    val moduleId: String,
    val title: String,
    val group: String,
    val kind: String,
    val period: String,
    val answered: Int,
    val total: Int,
    val average: Double?,
    val highlightedStatements: List<String>
) {
    val complete: Boolean get() = answered == total
    val usable: Boolean get() = average != null || highlightedStatements.isNotEmpty()
}

internal data class JourneyReport(
    val level: JourneyLevel,
    val answered: Int,
    val total: Int,
    val modules: List<JourneyModuleResult>,
    val observationTitle: String,
    val observations: List<String>
) {
    val completed: Boolean get() = answered == total
    val usableModules: List<JourneyModuleResult> get() = modules.filter { it.usable }
}

internal object JourneyReportEngine {
    private val six = AssessmentAnswerSummary.domains.toSet()
    private val messages = mapOf(
        "anxiety" to "В ответах часто встречалось беспокойство. Возможно, стоит заметить, когда тревога сильнее и что помогает успокоиться.",
        "anger" to "В ответах часто встречались раздражение и гнев. Полезно замечать ситуации перед вспышкой и выбирать безопасную паузу.",
        "sadness" to "В ответах часто встречалась грусть. Можно обратить внимание на поддержку и моменты, когда становится хотя бы немного легче.",
        "apathy" to "В ответах часто встречались недостаток сил и трудности с началом дел. Стоит оценить нагрузку и доступный отдых.",
        "thoughts" to "В ответах часто возвращались нежелательные мысли или проверки. Можно исследовать, когда этот цикл отнимает больше внимания.",
        "stress" to "В ответах часто встречались стресс и перегрузка. Возможно, сейчас особенно полезны небольшие паузы и снижение нагрузки."
    )

    fun report(level: JourneyLevel, answers: Map<String, Int>): JourneyReport {
        val questions = AssessmentJourney.questions(level)
        val modules = questions.groupBy { it.module.id }.values.map { same ->
            moduleResult(same.first().module, same.map { it.question }, answers)
        }
        val count = modules.sumOf { it.answered }
        val scoredSix = modules.filter { it.moduleId in six && it.average != null }
        val notable = scoredSix.filter { it.average!! >= 3.0 }.sortedByDescending { it.average }
        val notes = when {
            notable.isNotEmpty() -> notable.take(3).map { messages.getValue(it.moduleId) }
            scoredSix.isNotEmpty() -> listOf(
                "По имеющимся ответам нет направлений с частыми отметками на уровне «часто» или «почти всегда». Это не означает, что трудностей нет."
            )
            count == 0 -> listOf("Пока нет ответов. Можно пройти несколько вопросов и вернуться к результатам.")
            else -> listOf("Ответы сохранены. Для сравнения шести направлений понадобится ещё несколько ответов; ниже уже видны отмеченные темы.")
        }
        return JourneyReport(level, count, questions.size, modules,
            if (notable.isNotEmpty()) "Что чаще встречалось в твоих ответах" else "Что можно узнать из ответов", notes)
    }

    fun single(module: SurveyModule, answers: Map<String, Int>): JourneyModuleResult =
        moduleResult(module, module.questions, answers)

    private fun moduleResult(module: SurveyModule, questions: List<SurveyQuestion>, answers: Map<String, Int>): JourneyModuleResult {
        val valid = questions.mapNotNull { q -> answers[q.id]?.takeIf { it in 0..4 }?.let { q to it } }
        val enough = valid.size >= 2 && valid.size * 10 >= questions.size * 7
        val score = when {
            module.kind == "ipip" && valid.size == questions.size && questions.size == module.questions.size ->
                valid.map { (q, v) -> if (q.reversed) 4 - v else v }.average()
            module.id in six && enough -> valid.map { it.second }.average()
            module.id in setOf("resources", "values") && enough -> valid.map { it.second }.average()
            else -> null
        }
        // Mixed-direction nonvalidated modules have no numerical 'severity':
        // only show concrete statements that the person endorsed, without inference.
        val statements = if (module.kind != "ipip" && module.id !in six && module.id !in setOf("resources", "values")) {
            valid.filter { it.second >= 3 }.sortedByDescending { it.second }
                .take(3).map { it.first.text }
        } else emptyList()
        return JourneyModuleResult(module.id, module.title, module.group, module.kind, module.period,
            valid.size, questions.size, score, statements)
    }
}
