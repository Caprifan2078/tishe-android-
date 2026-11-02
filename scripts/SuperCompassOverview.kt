package ru.tishe.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One integrated surface, two separate kinds of measurements:
 * 18 user-selected emotions now and six retrospectively answered questionnaires.
 * No trauma detection or mental illness inference.
 */
@Composable
internal fun SuperCompassOverview(
    current: Map<String, Int>,
    assessmentAnswers: Map<String, Int>,
    history: List<FeelingSnapshot>,
    onStart: (String, Emotion) -> Unit
) {
    val themes = remember(current, assessmentAnswers) {
        SuperCompassSignals.themes(current, assessmentAnswers)
    }
    val resources = remember(current) {
        SuperCompassSignals.noticedResources(current)
    }
    var showOrigins by remember { mutableStateOf(false) }
    val marked = EmotionCompass.axes.count { current.containsKey(it.id) }

    SoftCard(highlight = true) {
        Text("🧭 Общая карта чувств", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            "Все 18 чувств на одной схеме. Отмечено: $marked из 18. " +
                "Пустая грань — не ноль, а отсутствие ответа.",
            color = Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        RadarDiagram(EmotionCompass.axes, current, Accent)
        Spacer(Modifier.height(7.dp))
        FeelingGroup.entries.forEach { group ->
            val axes = EmotionCompass.axes.filter { it.group == group }
            val answered = axes.count { it.id in current }
            Text("${group.shortTitle}: $answered / ${axes.size}",
                color = when (group) {
                    FeelingGroup.TENSION -> androidx.compose.ui.graphics.Color(0xFFFFAF9C)
                    FeelingGroup.VULNERABLE -> androidx.compose.ui.graphics.Color(0xFFBAADFA)
                    FeelingGroup.RESOURCES -> androidx.compose.ui.graphics.Color(0xFF88DBC5)
                }, fontSize = 13.sp)
            Spacer(Modifier.height(3.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Диаграмма — только визуализация твоих отметок, не научный тест. " +
                "Ниже можно поставить 0–5 для каждого чувства.",
            color = Muted, fontSize = 12.sp
        )
    }

    Spacer(Modifier.height(15.dp))
    QuestionnaireResultsCard(assessmentAnswers)

    Spacer(Modifier.height(15.dp))
    Text("На что можно обратить внимание", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(
        "Это осторожные подсказки для самонаблюдения по конкретным ответам, " +
            "а не обнаруженные проблемы, причины или психологические травмы.",
        color = Muted, fontSize = 13.sp
    )
    Spacer(Modifier.height(10.dp))
    if (themes.isEmpty()) {
        SoftCard {
            Text("Пока недостаточно отметок для персональных подсказок.",
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("Можно отметить чувства ниже или ответить на вопросы в разделе «Тесты». " +
                "Это необязательно.", color = Muted, fontSize = 13.sp)
        }
    } else {
        themes.forEach { item ->
            SoftCard {
                Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(item.description, color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("Почему появилась подсказка", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(item.evidence, color = Mint, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Вопрос для себя: ${item.question}", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { onStart(item.route, item.emotion) }) {
                    Text("Попробовать бережную практику →")
                }
            }
            Spacer(Modifier.height(9.dp))
        }
    }

    if (resources.isNotEmpty()) {
        SoftCard {
            Text("🌿 Что уже поддерживает", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(resources.joinToString { "${it.emoji} ${it.title.lowercase()}" },
                color = Mint, fontSize = 14.sp)
            Text("Ресурсные чувства могут присутствовать одновременно с трудными.",
                color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
    }

    OutlinedButton(
        onClick = { showOrigins = !showOrigins },
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (showOrigins) "Скрыть вопросы о причинах" else "Как бережно исследовать возможные причины?") }
    if (showOrigins) {
        Spacer(Modifier.height(8.dp))
        SoftCard {
            Text("Опыт и прошлые события", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Никакой график не может определить, была ли у человека психологическая травма " +
                    "или что именно произошло. Если захочешь, можно подумать:",
                color = Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("• В каких ситуациях мне становится тяжелее?\n" +
                "• Что помогает снова почувствовать опору или безопасность?\n" +
                "• С кем я могу поговорить, если переживания повторяются?",
                fontSize = 13.sp)
            Spacer(Modifier.height(9.dp))
            Text(
                "Если воспоминания сильно беспокоят, мешают спать или жить привычной жизнью, " +
                    "имеет смысл обратиться к квалифицированному специалисту. " +
                    "Не нужно подробно вспоминать болезненные события ради приложения.",
                color = Muted, fontSize = 12.sp
            )
        }
    }
    Spacer(Modifier.height(9.dp))
    Text(
        "Все расчёты локальные. Сохранённые отметки: ${history.size}. " +
            "Никаких новых анкет и передачи данных в сеть.",
        color = Muted, fontSize = 12.sp
    )
}
