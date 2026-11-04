package ru.tishe.app

/**
 * Evidence-first descriptions of answers. Never infers disorders, trauma,
 * personality types, causes, or population-relative severity.
 * This is intentionally deterministic, offline and read-only.
 */
internal data class NarrativeEvidence(
    val title: String,
    val observation: String,
    val source: String,
    val tryNext: String?
)
internal data class QuestionnaireNarrative(
    val headline: String,
    val overview: String,
    val evidence: List<NarrativeEvidence>,
    val additionalContext: List<String>,
    val gentleSteps: List<String>,
    val limitations: List<String>
)

internal object QuestionnaireNarrativeEngine {
    private val six = setOf("anxiety", "anger", "sadness", "apathy", "thoughts", "stress")
    private val practical = mapOf(
        "anxiety" to "Заметь одну ситуацию, в которой беспокойство сильнее, и попробуй на минуту вернуться к окружающим предметам.",
        "anger" to "Вспомни безопасный способ сделать паузу перед ответом, особенно когда раздражение нарастает.",
        "sadness" to "Подумай, с кем можно поговорить и что могло бы дать немного поддержки.",
        "apathy" to "Выбери очень небольшое действие без требования сделать всё идеально; сначала оцени силы.",
        "thoughts" to "Если мысль возвращается, попробуй заметить её и переключиться на одно посильное дело, не споря с ней.",
        "stress" to "Проверь, какую одну задачу можно отложить, упростить или сделать с перерывом."
    )

    private data class Answered(val question: SurveyQuestion, val score: Int)

    private fun valid(
        questions: List<SurveyQuestion>,
        answers: Map<String, Int>,
        max: Int = 4
    ): List<Answered> = questions.mapNotNull { question ->
        answers[question.id]?.takeIf { it in 0..max }?.let { Answered(question, it) }
    }

    /** Labels are descriptions of marked options, never clinical cut-off points. */
    private fun evidenceOf(module: SurveyModule, ratings: List<Answered>): String {
        val highs = ratings.count { it.score >= 3 }
        val middles = ratings.count { it.score == 2 }
        val lows = ratings.count { it.score <= 1 }
        val prefix = when {
            highs >= 2 -> "В $highs из ${ratings.size} ответов выбрано «часто» или «почти всегда»"
            highs == 1 -> "В одном из ${ratings.size} ответов выбрано «часто» или «почти всегда»"
            middles > 0 -> "В $middles из ${ratings.size} ответов выбрано «иногда»"
            else -> "Во всех ${ratings.size} ответах выбрано «нет» или «редко»"
        }
        val diversity = if (highs > 0 && lows > 0)
            ". При этом некоторые ситуации отмечены редко — общий балл не отражает всех различий" else ""
        return "$prefix$diversity. Период: ${module.period}."
    }

    private fun prominentStatements(answers: List<Answered>, limit: Int): List<String> =
        answers.filter { it.score >= 3 }
            .sortedWith(compareByDescending<Answered> { it.score }.thenBy { it.question.id })
            .take(limit).map { it.question.text }

    fun forJourney(level: JourneyLevel, answers: Map<String, Int>): QuestionnaireNarrative {
        val report = JourneyReportEngine.report(level, answers)
        if (report.answered == 0) return QuestionnaireNarrative(
            "Пока недостаточно ответов",
            "Можно вернуться к вопросам: без ответов приложение не делает выводов.",
            emptyList(), emptyList(), emptyList(),
            listOf("Никаких предположений о личности или состоянии без данных.")
        )

        val domains = report.modules.filter { it.moduleId in six && it.average != null }
            .mapNotNull { result ->
                val m = AssessmentCatalog.find(result.moduleId) ?: return@mapNotNull null
                val list = valid(m.questions, answers)
                Triple(result, m, list)
            }
            .sortedWith(compareByDescending<Triple<JourneyModuleResult, SurveyModule, List<Answered>>> {
                it.third.count { a -> a.score >= 3 }
            }.thenByDescending { it.first.average ?: 0.0 })
        val notable = domains.filter { it.third.count { a -> a.score >= 3 } >= 2 }
        val emphasized = if (notable.isNotEmpty()) notable.take(3)
            else domains.filter { it.third.any { a -> a.score >= 2 } }.take(2)

        val observations = emphasized.map { (_, module, list) ->
            NarrativeEvidence(
                module.title,
                evidenceOf(module, list),
                prominentStatements(list, 2).joinToString(" · ").ifEmpty {
                    "Отмечены отдельные ответы «иногда», но нет часто отмеченных утверждений."
                },
                practical[module.id]
            )
        }

        val overview = when {
            notable.size >= 2 ->
                "В нескольких направлениях встречаются частые переживания: ${notable.take(3).joinToString { it.second.title }}. Это описание выбранных ответов за неделю, а не свойство личности."
            notable.size == 1 ->
                "В направлении «${notable.first().second.title}» чаще других встречаются высокие отметки. Это повод присмотреться к конкретным ситуациям, а не определение твоего характера."
            domains.isNotEmpty() && emphasized.isNotEmpty() ->
                "Чаще встречаются ответы «иногда» или отдельные высокие отметки. Сильных выводов об устойчивых трудностях по этой анкете делать нельзя."
            domains.isNotEmpty() ->
                "В шести основных темах ты преимущественно выбирал невысокие отметки. Это не исключает отдельных переживаний и не доказывает отсутствие трудностей."
            else ->
                "Ответы есть, но для шести основных направлений пока недостаточно данных. Ниже показаны только непосредственно отмеченные темы."
        }

        val context = mutableListOf<String>()
        val resources = AssessmentCatalog.find("resources")?.let { valid(it.questions, answers) }.orEmpty()
        if (resources.size >= 4 && resources.count { it.score >= 3 } >= 2) {
            val q = prominentStatements(resources, 2)
            context += "В вопросах о ритме жизни отмечены возможные нагрузки: ${q.joinToString("; ")}. Это не доказывает причину переживаний."
        }
        val interests = AssessmentCatalog.find("values")?.let { valid(it.questions, answers) }.orEmpty()
        if (interests.size >= 4) {
            val q = prominentStatements(interests, 2)
            if (q.isNotEmpty())
                context += "Среди занятий, которые тебе откликаются: ${q.joinToString("; ")}. Это возможные личные опоры, а не обязательные рекомендации."
        }

        // Extra 150/300/500 items add context grounded in endorsed sentences,
        // not arbitrary diagnoses based on averages across opposite-keyed prompts.
        val extra = report.modules.filter { it.moduleId !in six &&
            it.moduleId !in setOf("resources", "values") &&
            it.kind != "ipip" && it.answered >= 3 }.mapNotNull { result ->
            val module = AssessmentCatalog.find(result.moduleId) ?: return@mapNotNull null
            val found = prominentStatements(valid(module.questions, answers), 1)
            if (found.isEmpty()) null
            else Triple(result, module, found.first())
        }.sortedWith(compareByDescending<Triple<JourneyModuleResult, SurveyModule, String>> {
            it.first.answered.toDouble() / it.first.total
        }.thenBy { it.first.moduleId }).take(2)
        context += extra.map { (_, module, statement) ->
            "В разделе «${module.title}» ты отметил: «$statement». Это тема для размышления, не объяснение причины и не диагноз."
        }

        val traits = report.modules.filter { it.kind == "ipip" && it.average != null }
        if (traits.isNotEmpty()) {
            context += "Завершено ${traits.size} направлений самоописания IPIP. Они видны отдельным слоем: без возрастных норм, сравнения с другими людьми и ярлыков."
        }

        val limitations = buildList {
            if (!report.completed)
                add("Это предварительный результат: отвечено ${report.answered} из ${report.total}. Пропущенные ответы не подставлялись.")
            if (domains.any { (_, _, list) ->
                list.any { it.score >= 3 } && list.any { it.score <= 1 }
            }) add("Внутри некоторых тем ответы различаются. Одна средняя цифра может скрывать важные ситуации.")
            add("Авторские шкалы не являются валидированным диагностическим тестом. Порог «часто» здесь означает выбор ответа 3–4, а не медицинскую границу.")
            add("Ответы о прошедшем периоде не подменяют то, что ты чувствуешь сейчас.")
        }
        val steps = emphasized.mapNotNull { practical[it.second.id] }.distinct().take(2)
        return QuestionnaireNarrative(
            "Что видно по ответам",
            overview,
            observations,
            context.take(5),
            steps.ifEmpty { listOf("Выбери один вопрос, на который хотелось бы обратить внимание, и наблюдай, когда ситуация меняется.") },
            limitations
        )
    }

    fun forSingle(module: SurveyModule, answers: Map<String, Int>): QuestionnaireNarrative {
        val rated = valid(module.questions, answers, if (module.evidenceBased) 3 else 4)
        val total = module.questions.size
        if (rated.isEmpty()) return QuestionnaireNarrative(
            "Ответов пока нет", "Можно пройти опрос и затем увидеть результат.",
            emptyList(), emptyList(), emptyList(), emptyList()
        )
        if (module.evidenceBased) {
            val score = AssessmentCatalog.phq4Total(answers)
            val parts = AssessmentCatalog.phq4Parts(answers)
            val description = when {
                score == null -> "Опрос заполнен не полностью. Итоговый показатель PHQ-4 не рассчитывается."
                score >= 9 -> "По правилам PHQ-4 сумма относится к диапазону выраженных симптомов; это повод обсудить переживания со специалистом, а не диагноз."
                score >= 6 -> "По правилам PHQ-4 сумма относится к умеренному диапазону; можно обсудить состояние со специалистом."
                score >= 3 -> "По правилам PHQ-4 сумма относится к лёгкому диапазону; это не исключает необходимости поддержки."
                else -> "По правилам PHQ-4 суммарная частота отмеченных симптомов невысока. Это не исключает других трудностей."
            }
            return QuestionnaireNarrative(
                "PHQ-4: краткий скрининг",
                description,
                if (score != null && parts != null) listOf(NarrativeEvidence(
                    "Что показали ответы",
                    "Сумма $score/12; две первые позиции ${parts.first}/6, две следующие ${parts.second}/6. Период: ${module.period}.",
                    "Все четыре пункта должны быть заполнены значениями 0–3.", null)) else emptyList(),
                emptyList(),
                if (score != null && (score >= 6 || parts?.first ?: 0 >= 3 || parts?.second ?: 0 >= 3))
                    listOf("Обсуди результат и влияние переживаний на жизнь с квалифицированным специалистом.")
                else listOf("Если переживания мешают жить, обратиться за поддержкой можно при любом балле."),
                listOf("Это скрининг, не диагноз; русская формулировка приложения не прошла отдельную проверку психометрической эквивалентности.")
            )
        }
        if (module.kind == "ipip") {
            val result = JourneyReportEngine.single(module, answers)
            return QuestionnaireNarrative("Особенности самоописания",
                if (result.average == null) "Для описательного расчёта нужно ответить на все вопросы этой темы."
                else "После обратного подсчёта некоторых пунктов средний балл составляет ${format(result.average)}/4. Это направление самоописания, а не высокий или низкий «уровень личности».",
                emptyList(), emptyList(),
                listOf("Посмотри, какие конкретные утверждения точно описывают тебя, а какие зависят от ситуации."),
                listOf("Русская адаптация не валидирована, сравнительных норм нет; выводить тип личности или диагноз нельзя.")
            )
        }
        val selected = prominentStatements(rated, 3)
        val mixed = rated.any { it.score >= 3 } && rated.any { it.score <= 1 }
        val intro = when {
            selected.size >= 2 -> "Часто отмечены некоторые ситуации из этой темы. Это описание конкретных ответов, а не вывод о характере."
            selected.size == 1 -> "В одном пункте выбран высокий вариант; остальные ответы могут говорить о другой картине."
            rated.any { it.score == 2 } -> "Встречаются ответы «иногда». По ним нельзя делать однозначный вывод."
            else -> "Частых отметок по этой теме нет. Это не означает, что она неважна для тебя."
        }
        return QuestionnaireNarrative(
            "Что видно по этой теме", intro,
            if (selected.isEmpty()) emptyList() else listOf(NarrativeEvidence(module.title,
                "${selected.size} отмеченных утверждений (вариант 3–4). Период: ${module.period}.",
                selected.joinToString(" · "), null)),
            emptyList(),
            listOf("Вспомни одну реальную ситуацию, которая подтверждает или опровергает этот ответ."),
            buildList {
                if (rated.size < total) add("Это предварительная картина: ${rated.size}/$total пунктов, пропущенные не подставлялись.")
                if (mixed) add("Встречаются и низкие, и высокие отметки: общее среднее могло бы скрыть различия.")
                add("Авторский опрос не позволяет ставить диагноз или определять причину трудностей.")
            }
        )
    }

    private fun format(value: Double): String = String.format(java.util.Locale.ROOT, "%.1f", value)
}
