package ru.tishe.app

/**
 * The first, plain-language sentence after every test. Scores in Tishe's custom modules
 * are NOT diagnostic thresholds. A "difficulty" below means a frequently chosen
 * description in this questionnaire, not an established disorder or personality trait.
 */
internal data class DirectTestConclusion(
    val headline: String,
    val explanation: String,
    val specifics: List<String>,
    val nextStep: String
)

internal object DirectTestConclusionEngine {
    private val domains = listOf("anxiety", "anger", "sadness", "apathy", "thoughts", "stress")
    private val difficulties = mapOf(
        "anxiety" to "тревогой и беспокойством",
        "anger" to "раздражением и гневом",
        "sadness" to "грустью и подавленным настроением",
        "apathy" to "энергией и мотивацией",
        "thoughts" to "повторяющимися мыслями и проверками",
        "stress" to "стрессом и перегрузкой"
    )
    private val steps = mapOf(
        "anxiety" to "Когда усиливается беспокойство, запишите тревожащую мысль и один доступный следующий шаг.",
        "anger" to "При сильном раздражении сделайте безопасную паузу перед ответом и вернитесь к разговору спокойнее.",
        "sadness" to "Постарайтесь не оставаться без поддержки: выберите одного человека, с кем можно поговорить.",
        "apathy" to "Выберите одно небольшое посильное действие и не требуйте от себя сразу большого результата.",
        "thoughts" to "Обратите внимание, какие ситуации запускают повторные проверки, и обсудите их со специалистом, если они сильно мешают.",
        "stress" to "Посмотрите, что можно отложить, упростить или сделать с перерывом."
    )

    private data class Domain(
        val id: String,
        val answered: Int,
        val high: Int,
        val sometimes: Int,
        val mean: Double
    )

    private fun domain(id: String, questionIds: Set<String>, answers: Map<String, Int>): Domain? {
        val module = AssessmentCatalog.find(id) ?: return null
        val available = module.questions.filter { it.id in questionIds }
        val scores = available.mapNotNull { answers[it.id]?.takeIf { v -> v in 0..4 } }
        // Three of four must be supplied; omitted answers are never treated as zero.
        if (scores.size < 3 || scores.size * 10 < available.size * 7) return null
        return Domain(id, scores.size, scores.count { it >= 3 },
            scores.count { it == 2 }, scores.average())
    }

    private fun titleFor(domain: Domain): String = difficulties.getValue(domain.id)

    fun journey(level: JourneyLevel, answers: Map<String, Int>): DirectTestConclusion {
        val questions = AssessmentJourney.questions(level)
        val valid = questions.count { answers[it.question.id] in 0..4 }
        if (valid == 0) return DirectTestConclusion(
            "Тест ещё не пройден",
            "Вы пока не ответили на вопросы — результат рассчитывать не из чего.",
            emptyList(), "Ответьте на вопросы и вернитесь к результату."
        )
        val allowed = questions.map { it.question.id }.toSet()
        val stats = domains.mapNotNull { domain(it, allowed, answers) }
            .sortedWith(compareByDescending<Domain> { it.high }.thenByDescending { it.mean })
        val strong = stats.filter { it.high >= 3 }
        val marked = stats.filter { it.high >= 2 }
        val occasional = stats.filter { it.high == 1 || (it.high == 0 && it.sometimes > 0) }
        val significant = when {
            strong.isNotEmpty() -> strong
            marked.isNotEmpty() -> marked
            else -> occasional
        }
        val focus = significant.firstOrNull()
        val focusNames = significant.take(2).joinToString(" и ") { titleFor(it) }
        // The 40-question route also includes sleep/support and interests:
        // include those real answers instead of silently ignoring 16 questions.
        val resourceAnswers = questions.filter { it.module.id == "resources" }
            .mapNotNull { q -> answers[q.question.id]?.takeIf { it in 0..4 }?.let { q.question to it } }
        val frequentResourceIssues = resourceAnswers.filter { it.second >= 3 }
        val resourceConcern = resourceAnswers.size >= 4 && frequentResourceIssues.size >= 3
        val interests = questions.filter { it.module.id == "values" }
            .mapNotNull { q -> answers[q.question.id]?.takeIf { it in 3..4 }?.let { q.question.text } }
            .take(2)
        val headline = when {
            strong.isNotEmpty() ->
                "По вашим ответам у вас часто возникают трудности с $focusNames."
            marked.isNotEmpty() ->
                "По вашим ответам есть заметные сложности с $focusNames."
            occasional.isNotEmpty() ->
                "По вашим ответам есть отдельные сложности с $focusNames."
            resourceConcern ->
                "По вашим ответам у вас заметные трудности с восстановлением, нагрузкой или поддержкой."
            stats.size == domains.size ->
                "По тесту вы не отметили частых трудностей в шести основных темах."
            else ->
                "По части вопросов пока нельзя определить основные трудности."
        }
        val explanation = when {
            focus != null ->
                "Главная причина вывода: в теме «${AssessmentCatalog.find(focus.id)!!.title}» " +
                    "${focus.high} из ${focus.answered} ответов отмечены как «часто» или «почти всегда». " +
                    "Это ваш ответ, а не медицинский диагноз."
            resourceConcern ->
                "В разделе «Ритм жизни и опоры» ${frequentResourceIssues.size} из ${resourceAnswers.size} ответов выбраны на уровне «часто» или «почти всегда». " +
                    "Это ответы о повседневных трудностях, не диагноз."
            stats.isNotEmpty() ->
                "Ни одна из шести основных тем не набрала нескольких частых отметок. " +
                    "Это не означает, что у вас вообще нет трудностей."
            else -> "Вы уже ответили на часть теста, но основных направлений пока недостаточно для обоснованного вывода."
        }
        val concrete = mutableListOf<String>()
        for (entry in significant.take(3)) {
            concrete += "${AssessmentCatalog.find(entry.id)!!.title}: частые отметки в ${entry.high} из ${entry.answered} ответов."
        }
        if (frequentResourceIssues.isNotEmpty()) {
            concrete += "Отдых и поддержка: ${frequentResourceIssues.size} из ${resourceAnswers.size} частых отметок."
        }
        if (interests.isNotEmpty()) {
            concrete += "Вам близки: ${interests.joinToString("; ")}."
        }
        // The extra first-route context includes four feeling/life reflections.
        if (level == JourneyLevel.START) {
            val extraStart = questions.filter { it.module.id == "emotion_layers" }
                .mapNotNull { q -> answers[q.question.id]?.takeIf { it in 3..4 }?.let { q.question.text } }
                .take(1)
            concrete += extraStart.map { "Ещё вы отметили: «$it»." }
        }
        // Long questionnaires have mixed, positively and negatively keyed items.
        // Quote actual endorsed statements, never calculate a made-up 'problem' score.
        if (level != JourneyLevel.START) {
            val additional = questions.asSequence().filter { it.module.id !in domains &&
                it.module.id != "values" && it.module.kind != "ipip" }
                .mapNotNull { q -> answers[q.question.id]?.takeIf { it >= 3 && it <= 4 }?.let { q } }
                .distinctBy { it.question.id }.take(2).toList()
            concrete += additional.map { "Также вы часто отмечали: «${it.question.text}»." }
            val completedTraits = questions.filter { it.module.kind == "ipip" }.groupBy { it.module.id }
                .values.count { entries -> entries.size == 20 &&
                    entries.all { answers[it.question.id] in 0..4 } }
            if (completedTraits > 0) concrete +=
                "Есть отдельные результаты ${completedTraits} шкал самоописания IPIP — смотрите слой «Особенности»."
        }
        val prefix = if (valid < questions.size)
            "Это предварительный итог (${valid} из ${questions.size} ответов). " else ""
        return DirectTestConclusion(prefix + headline, explanation, concrete.take(6),
            focus?.let { steps.getValue(it.id) }
                ?: if (resourceConcern) "Выберите один небольшой способ отдыха или поддержки, который реально доступен вам."
                else "Если какая-то трудность мешает вам жить, помощь можно искать при любом результате теста.")
    }

    fun single(module: SurveyModule, answers: Map<String, Int>): DirectTestConclusion {
        val maximum = if (module.evidenceBased) 3 else 4
        val scored = module.questions.mapNotNull { q ->
            answers[q.id]?.takeIf { it in 0..maximum }?.let { q to it }
        }
        if (scored.isEmpty()) return DirectTestConclusion(
            "Тест ещё не пройден", "Ответов пока нет.",
            emptyList(), "Заполните вопросы, чтобы узнать результат."
        )
        if (module.evidenceBased) {
            val total = AssessmentCatalog.phq4Total(answers)
            if (total == null) return DirectTestConclusion(
                "PHQ-4 пока не завершён", "Для корректного результата нужны все четыре ответа.",
                listOf("Отвечено ${scored.size} из 4."),
                "Завершите опрос, чтобы увидеть результат скрининга."
            )
            val severity = when {
                total >= 9 -> "очень высокая частота отмеченных симптомов"
                total >= 6 -> "умеренно высокая частота отмеченных симптомов"
                total >= 3 -> "небольшая частота отмеченных симптомов"
                else -> "минимальная частота отмеченных симптомов"
            }
            return DirectTestConclusion(
                "По PHQ-4: $severity.",
                "Ваш результат — $total из 12. Это показатель краткого скрининга, не диагноз.",
                listOf("Тревожная часть: ${AssessmentCatalog.phq4Parts(answers)!!.first}/6; часть о настроении: ${AssessmentCatalog.phq4Parts(answers)!!.second}/6."),
                if (total >= 6 || AssessmentCatalog.phq4Parts(answers)!!.first >= 3 ||
                    AssessmentCatalog.phq4Parts(answers)!!.second >= 3)
                    "Обсудите результат и влияние переживаний на жизнь со специалистом."
                else "Если переживания мешают вам, помощь уместна при любом результате."
            )
        }
        if (module.kind == "ipip") {
            val all = module.questions.map { q ->
                answers[q.id]?.takeIf { it in 0..4 }?.let { if (q.reversed) 4 - it else it }
            }
            if (all.any { it == null }) return DirectTestConclusion(
                "Самоописание ещё не завершено",
                "Для сопоставления утверждений нужны все ${module.questions.size} ответов.",
                emptyList(), "Завершите этот опрос."
            )
            val mean = all.filterNotNull().average()
            val label = module.title.substringAfter("·").trim()
            val line = when {
                mean >= 3.0 -> "По вашим ответам вам близки утверждения по теме «$label»."
                mean <= 1.0 -> "По вашим ответам вам мало подходят утверждения по теме «$label»."
                else -> "У вас неоднозначное самоописание по теме «$label»."
            }
            return DirectTestConclusion(line,
                "Средняя оценка с учётом обратных утверждений: ${String.format(java.util.Locale.ROOT, "%.1f", mean)}/4. " +
                    "Это не сравнение с другими людьми и не определение типа личности.",
                emptyList(), "Посмотрите, какие конкретные утверждения отражают вас, а какие зависят от ситуации."
            )
        }
        val highs = scored.filter { it.second >= 3 }
        val texts = highs.take(3).map { it.first.text }
        if (module.id == "values") {
            return DirectTestConclusion(
                if (highs.isNotEmpty()) "По вашим ответам вам особенно близки некоторые занятия."
                else "По этому списку вы не отметили занятий с высоким интересом.",
                if (highs.isNotEmpty()) "Вам нравятся: ${texts.joinToString("; ")}."
                else "Это не означает отсутствия интересов вообще — в тесте перечислены не все занятия.",
                emptyList(), "Выберите интересное занятие, которое реально доступно и приятно вам."
            )
        }
        if (module.id == "resources") {
            val line = when {
                highs.size >= 3 -> "По вашим ответам у вас есть заметные трудности с отдыхом и восстановлением."
                highs.isNotEmpty() -> "По вашим ответам есть отдельные сложности с отдыхом и поддержкой."
                else -> "По этому опросу вы не отметили частых трудностей с восстановлением."
            }
            return DirectTestConclusion(line,
                "${highs.size} из ${scored.size} ответов выбраны на уровне «часто» или «почти всегда».",
                texts, "Проверьте, какой небольшой способ отдыха или поддержки доступен вам."
            )
        }
        if (module.id in domains) {
            val score = domain(module.id, module.questions.map { it.id }.toSet(), answers)
            if (score == null) return DirectTestConclusion(
                "По этой теме пока мало ответов",
                "Ответьте хотя бы на три из четырёх вопросов для содержательного вывода.",
                texts, "Завершите тему."
            )
            val name = titleFor(score)
            val statement = when {
                score.high >= 3 -> "По вашим ответам у вас часто возникают трудности с $name."
                score.high >= 2 -> "По вашим ответам есть заметные сложности с $name."
                score.high == 1 || score.sometimes > 0 -> "Есть отдельные сложности с $name."
                else -> "По вашим ответам частых трудностей с $name не отмечено."
            }
            return DirectTestConclusion(statement,
                "Высокие отметки: ${score.high} из ${score.answered}. Это вывод по вашим ответам, не диагноз.",
                texts, steps.getValue(module.id))
        }
        // Mixed-direction author-written modules: never label a positively worded 'yes' as a problem.
        val title = when {
            highs.size >= 2 -> "В ваших ответах особенно заметна тема «${module.title}»."
            highs.size == 1 -> "Вы выделили один важный пункт по теме «${module.title}»."
            else -> "По теме «${module.title}» частых отметок нет."
        }
        return DirectTestConclusion(title,
            if (texts.isEmpty())
                "Вы не выбирали варианты «часто» или «почти всегда». Это тоже результат, но не гарантия отсутствия трудностей."
            else "Вы часто отмечали конкретные утверждения, приведённые ниже. Среди них могут быть и полезные привычки, и трудности.",
            texts, "Выберите один ответ и оцените, насколько он помогает вам или мешает в жизни."
        )
    }
}
