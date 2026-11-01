package ru.tishe.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Live computed feedback about the questionnaire, not a saved snapshot of how the user feels now. */
@Composable
internal fun QuestionnaireResultsCard(answers: Map<String, Int>) {
    val domains = remember(answers) { AssessmentAnswerSummary.scales(answers) }
    val filled = remember(answers) { AssessmentJourney.progress(JourneyLevel.START, answers) }
    if (domains.isEmpty() && filled.answered == 0) return

    SoftCard(highlight = true) {
        Text("📊  Шкалы по твоим ответам", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Начальный опрос: ${filled.answered} / 40. Шкал рассчитано: ${domains.size} из 6.", color = Mint)
        Spacer(Modifier.height(6.dp))
        Text(
            "Рассчитываются автоматически по уже сохранённым ответам. Это описание последних 7 дней, а не диагноз и не чувства прямо сейчас.",
            color = Muted, fontSize = 12.sp
        )
        if (domains.size == 6) {
            Spacer(Modifier.height(12.dp))
            val axes = domains.map {
                FeelingAxis(it.id, it.label, FeelingGroup.TENSION, "•")
            }
            RadarDiagram(axes, domains.associate { it.id to it.radarValue }, Mint)
            Text("Диаграмма показывает средние ответы, условно переведённые с 0–4 на 0–5.", color = Muted, fontSize = 12.sp)
        } else {
            Spacer(Modifier.height(7.dp))
            Text("Часть шкал появится после ответов на вопросы соответствующей темы (минимум два на тему).", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        domains.forEach { domain ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(domain.label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Text(String.format(Locale("ru"), "%.1f / 4", domain.average), color = Mint, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (domain.average / 4.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Ответов: ${domain.answered} / ${domain.total}", fontSize = 11.sp, color = Muted)
            Spacer(Modifier.height(9.dp))
        }
        if (filled.complete) {
            Text("✓ Все 40 ответов учтены. Повторять опрос не нужно.", color = Mint, fontWeight = FontWeight.SemiBold)
        }
        Text("18 граней эмоционального компаса — отдельная отметка текущего состояния. Они не заполняются автоматически из ответов о прошедших днях.", color = Muted, fontSize = 12.sp)
    }
}
