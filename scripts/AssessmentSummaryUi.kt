package ru.tishe.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Read-only results. Does not modify the person's current emotional compass. */
@Composable
internal fun AssessmentDerivedScales(answers: Map<String, Int>, compact: Boolean = false) {
    val scales = remember(answers) { AssessmentSummary.scales(answers) }
    val available = scales.filter { it.mean != null }
    if (available.isEmpty()) return

    SoftCard(highlight = true) {
        Text("Результаты по ответам", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            "Автоматически рассчитаны ${available.size} из 6 направлений по вопросам за последнюю неделю.",
            color = Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        available.forEach { scale ->
            val value = scale.mean ?: return@forEach
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(scale.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                Text(String.format(Locale.ROOT, "%.1f / 4", value), color = Mint, fontSize = 13.sp)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { (value / 4.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            if (scale.answered != scale.total) {
                Text("Ответов: ${scale.answered} из ${scale.total}",
                    color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            if (compact)
                "Это не текущие чувства. 18 шкал эмоционального компаса — отдельная добровольная отметка «сейчас»."
            else
                "Это описание ответов, не диагноз и не оценка нормы. Шкалы эмоционального компаса (18 чувств) не вычисляются из этого опроса.",
            color = Muted, fontSize = 12.sp
        )
    }
}

@Composable
internal fun AssessmentJourneyResults(
    level: JourneyLevel,
    answers: Map<String, Int>,
    onReview: () -> Unit,
    onBack: () -> Unit
) {
    val progress = remember(level, answers) { AssessmentJourney.progress(level, answers) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← К выбору тестов") }
        Text("Результаты теста", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("${level.title} · отвечено ${progress.answered} из ${progress.total}",
            color = Muted, fontSize = 14.sp)
        if (!progress.complete) {
            Text("Опрос заполнен не полностью: ниже показаны только направления с достаточным количеством ответов.",
                color = Muted, fontSize = 13.sp)
        }
        if (AssessmentSummary.availableCount(answers) == 0) {
            SoftCard(highlight = true) {
                Text("Пока недостаточно ответов для шкал.", color = Muted)
                Text("Ответь хотя бы на два вопроса одного направления. Данные не потеряны.", color = Muted)
            }
        } else {
            AssessmentDerivedScales(answers)
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Вернуться к тестам")
        }
        OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
            Text("Посмотреть ответы")
        }
    }
}
