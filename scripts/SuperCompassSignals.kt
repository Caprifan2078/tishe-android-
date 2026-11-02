package ru.tishe.app

/**
 * Local, non-clinical reflection hints. Current feelings (0..5) and retrospective
 * survey domains (0..4) NEVER get merged into a shared score.
 * No diagnosis, trauma attribution, severity score, or inferred life events.
 */
internal data class ReflectionTheme(
    val id: String,
    val title: String,
    val description: String,
    val evidence: String,
    val question: String,
    val route: String,
    val emotion: Emotion
)

internal object SuperCompassSignals {
    fun themes(
        reportedNow: Map<String, Int>,
        assessmentAnswers: Map<String, Int>
    ): List<ReflectionTheme> {
        val current = EmotionCompass.normalize(reportedNow)
        val surveys = AssessmentAnswerSummary.scales(assessmentAnswers)
            .filter { it.answered >= 3 }
            .associateBy { it.id }
        val found = mutableListOf<ReflectionTheme>()

        fun add(
            id: String,
            title: String,
            description: String,
            axisIds: List<String>,
            domainIds: List<String>,
            question: String,
            route: String,
            emotion: Emotion
        ) {
            // A high mark is a reflection prompt, not a medical threshold.
            val currentAxes = EmotionCompass.axes.filter { it.id in axisIds && (current[it.id] ?: -1) >= 4 }
            val currentEvidence = if (currentAxes.isEmpty()) null else
                "Сейчас: " + currentAxes.joinToString { "${it.title} ${current[it.id]}/5" }
            val matchingDomains = domainIds.mapNotNull { surveys[it] }.filter { it.average >= 3.0 }
            val surveyEvidence = if (matchingDomains.isEmpty()) null else
                "В ответах за 7 дней: " + matchingDomains.joinToString {
                    "${it.label} ${"%.1f".format(java.util.Locale.ROOT, it.average)}/4 (${it.answered}/${it.total})"
                }
            val evidence = listOfNotNull(currentEvidence, surveyEvidence)
            if (evidence.isNotEmpty()) {
                found += ReflectionTheme(id, title, description,
                    evidence.joinToString(" · "), question, route, emotion)
            }
        }

        add("rest", "Возможно, сейчас особенно нужен отдых",
            "Можно попробовать уменьшить нагрузку и дать себе короткую паузу.",
            listOf("overload", "fatigue"), listOf("stress"),
            "Когда напряжение сильнее: после задач, общения или недосыпа?",
            "rest", Emotion.STRESS)
        add("worry", "Стоит присмотреться к беспокойству",
            "Мягкое заземление иногда помогает переключить внимание на настоящее.",
            listOf("anxiety", "fear", "restlessness"), listOf("anxiety"),
            "Какие ситуации усиливают тревогу, а в какие моменты она ослабевает?",
            "anchor", Emotion.ANXIETY)
        add("energy", "Возможно, не хватает сил или поддержки",
            "Не обязательно требовать от себя бодрости. Начни с посильного шага.",
            listOf("sadness", "low_mood", "loneliness"), listOf("sadness", "apathy"),
            "Что хоть немного помогает чувствовать опору в течение дня?",
            "garden", Emotion.SADNESS)
        add("boundaries", "Полезно замечать моменты раздражения",
            "Злость не делает тебя плохим человеком. Пауза поможет выбрать ответ.",
            listOf("anger", "irritation"), listOf("anger"),
            "Бывают ли ситуации, где твои границы или потребности остаются незамеченными?",
            "dial", Emotion.ANGER)
        add("thoughts", "Возможно, мысли отнимают много внимания",
            "Не нужно спорить с каждой мыслью; можно потренировать дистанцию.",
            emptyList(), listOf("thoughts"),
            "В какие моменты повторяющиеся мысли мешают больше всего?",
            "anchor", Emotion.THOUGHTS)
        add("self_kindness", "Можно попробовать более бережное отношение к себе",
            "Стыд и вина бывают тяжёлыми; это не оценка твоего характера.",
            listOf("shame", "guilt"), emptyList(),
            "Как бы ты поддержал близкого человека в похожей ситуации?",
            "kindness", Emotion.SADNESS)

        // Do not overwhelm users with six simultaneously alarming suggestions.
        return found.take(3)
    }

    fun noticedResources(reportedNow: Map<String, Int>): List<FeelingAxis> {
        val current = EmotionCompass.normalize(reportedNow)
        return EmotionCompass.axes.filter {
            it.group == FeelingGroup.RESOURCES && (current[it.id] ?: -1) >= 3
        }
    }
}
