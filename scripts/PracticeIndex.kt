package ru.tishe.app

import java.util.Locale

/** Offline index of existing exercises. No answers or personal notes are read. */
internal data class PracticeOption(
    val route: String, val title: String, val description: String,
    val emotion: Emotion, val short: Boolean = false
)

internal object PracticeIndex {
    val all: List<PracticeOption> by lazy {
        val basic = Emotion.entries.map { e ->
            PracticeOption(e.id, e.icon + " " + e.technique, e.subtitle, e, true)
        }
        val extra = listOf(
            PracticeOption("wave", "🌊 Волна паузы", "Безопасная пауза перед реакцией", Emotion.ANGER, true),
            PracticeOption("garden", "🌿 Сад доброты", "Визуальная поддержка при грусти", Emotion.SADNESS, true),
            PracticeOption("palette", "🎨 Палитра чувств", "Несколько эмоций одновременно", Emotion.STRESS),
            PracticeOption("reality", "🔎 Факты и предположения", "Замечать разницу между наблюдением и догадкой", Emotion.ANXIETY),
            PracticeOption("micro", "🌱 Первый маленький шаг", "Небольшая посильная задача", Emotion.APATHY, true),
            PracticeOption("phrase", "💬 Мастерская границ", "Бережно говорить о своих границах", Emotion.ANGER),
            PracticeOption("anchor", "🪨 Якорь внимания", "Вернуться вниманием к настоящему", Emotion.THOUGHTS, true),
            PracticeOption("kindness", "💜 Добрые слова", "Поддержать себя без критики", Emotion.SADNESS),
            PracticeOption("rest", "🌙 Пауза без задачи", "Отдохнуть без необходимости что-то доказать", Emotion.STRESS, true),
            PracticeOption("values", "🧭 Компас ценностей", "Понять, что для тебя важно", Emotion.SADNESS),
            PracticeOption("dial", "🌡️ Регулятор напряжения", "Заметить силу напряжения", Emotion.ANGER),
            PracticeOption("duo", "🎭 Два чувства одновременно", "Замечать смешанные чувства", Emotion.STRESS),
            PracticeOption("hobby_lab", "🧪 Лаборатория интересов", "Бесплатные занятия по силам", Emotion.APATHY),
            PracticeOption("stars", "🌌 Созвездие", "Мини-игра на внимание без очков", Emotion.STRESS, true)
        )
        val skills = EmotionSkillGames.menu.map { (route, title, emotion) ->
            PracticeOption(route, title, "Интерактивный тренажёр навыков", emotion, route in setOf("small_step","traffic_light"))
        }
        val gentle = GentlePractices.menu.map { (route, title, emotion) ->
            PracticeOption(route, title, "Спокойная практика без соревнования", emotion, route in setOf("color_path","three_supports","soft_rhythm"))
        }
        (basic + extra + skills + gentle).also { entries ->
            require(entries.map { it.route }.distinct().size == entries.size) { "Duplicate exercise route" }
        }
    }

    fun search(query: String, emotion: Emotion?, shortOnly: Boolean): List<PracticeOption> {
        val term = query.trim().lowercase(Locale.ROOT)
        return all.filter { item ->
            (!shortOnly || item.short) &&
            (emotion == null || item.emotion == emotion) &&
            (term.isEmpty() || listOf(item.title,item.description,item.emotion.title)
                .any { it.lowercase(Locale.ROOT).contains(term) })
        }
    }
}
