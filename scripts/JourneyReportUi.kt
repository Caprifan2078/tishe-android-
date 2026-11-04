package ru.tishe.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

/** Results screen reached when finishing any of the four nested journeys. */
@Composable
internal fun JourneyReportScreen(
    level: JourneyLevel,
    answers: Map<String, Int>,
    onReview: () -> Unit,
    onFinish: () -> Unit,
    onCompass: () -> Unit
) {
    val report = remember(level, answers) { JourneyReportEngine.report(level, answers) }
    val six = report.modules.filter { it.moduleId in AssessmentAnswerSummary.domains && it.average != null }
    val traits = report.modules.filter { it.kind == "ipip" && it.average != null }
    val other = report.modules.filter {
        it.moduleId !in AssessmentAnswerSummary.domains && it.kind != "ipip" && it.answered > 0
    }
    val layers = buildList {
        if (six.isNotEmpty()) add("Переживания · 6")
        if (traits.isNotEmpty()) add("Особенности · ${traits.size}")
        if (other.isNotEmpty()) add("Контекст · ${other.size}")
    }
    var selectedLayer by remember(level) { mutableIntStateOf(0) }
    val selected = layers.getOrNull(selectedLayer) ?: layers.firstOrNull()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { TextButton(onClick = onFinish) { Text("← К списку тестов", color = Accent) } }
        item {
            Text("Твои результаты", fontWeight = FontWeight.Bold, fontSize = 27.sp)
            Text("${level.title} · ${report.answered} из ${report.total} ответов", color = Mint)
            Text(
                if (report.completed) "✓ Все ответы этого режима учтены."
                else "Некоторые вопросы пропущены. Показаны только данные из твоих ответов.",
                color = Muted, fontSize = 13.sp
            )
        }
        item {
            SoftCard(highlight = true) {
                Text(report.observationTitle, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Spacer(Modifier.height(9.dp))
                report.observations.forEach { note ->
                    Text("• $note", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Text("Это наблюдения по ответам, не диагноз и не медицинская норма.",
                    color = Muted, fontSize = 12.sp)
            }
        }
        item {
            Text("Многослойная паутинка", fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text("Переключай слои: переживания, особенности самоописания и жизненный контекст. Несопоставимые данные не объединяются в один балл.",
                color = Muted, fontSize = 12.sp)
        }
        item {
            if (layers.isEmpty()) {
                SoftCard { Text("Пока мало ответов для паутинки. Уже введённое не потеряно.", color = Muted) }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    layers.forEachIndexed { i, name ->
                        FilterChip(selected = name == selected, onClick = { selectedLayer = i },
                            label = { Text(name) })
                    }
                }
            }
        }
        if (selected?.startsWith("Переживания") == true) {
            item {
                ReportRadarSection(six, "Переживания за семь дней",
                    "Средние значения частоты ответов: шкала 0–4 условно показана на паутинке 0–5. Это не чувства прямо сейчас.")
            }
        } else if (selected?.startsWith("Особенности") == true) {
            item {
                ReportRadarSection(traits, "Особенности самоописания",
                    "Ответы IPIP пересчитаны с учётом обратных вопросов. Русский перевод не валидирован: без нормативов, диагнозов или типов личности.")
            }
        } else if (selected?.startsWith("Контекст") == true) {
            item {
                SoftCard {
                    Text("Контекст и жизненные ситуации", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Здесь есть вопросы разной направленности. Нельзя превращать среднее в степень «проблемы». Ниже приведены твои реальные ответы.",
                        color = Muted, fontSize = 13.sp)
                }
            }
            items(other) { entry -> ReportContextCard(entry) }
        }
        item {
            Text("Как понимать результат", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Частые отметки тревоги или гнева означают только, что эти переживания встречались в указанный период. Это не означает, что ты «тревожный человек» или что у тебя расстройство. Невысокие оценки тоже не исключают трудностей.",
                color = Muted, fontSize = 13.sp)
        }
        item {
            Button(onClick = onCompass, modifier = Modifier.fillMaxWidth()) {
                Text("Открыть компас из 18 чувств сейчас")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
                Text("Вернуться к ответам")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text("Закончить просмотр")
            }
            Text("Результаты пересчитываются локально после изменения ответов. Шкалы из теста не подменяют личную отметку чувств.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReportRadarSection(
    entries: List<JourneyModuleResult>,
    heading: String,
    explanation: String
) {
    SoftCard(highlight = true) {
        Text(heading, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        if (entries.size >= 3) {
            val axes = entries.map { FeelingAxis(it.moduleId, it.title, FeelingGroup.TENSION, "•") }
            val scores = entries.associate {
                it.moduleId to (it.average!! * 5.0 / 4.0).roundToInt().coerceIn(0, 5)
            }
            RadarDiagram(axes, scores, Mint)
        } else {
            Text("Для паутинки этого слоя нужно не меньше трёх завершённых направлений. Сейчас они представлены отдельно.",
                color = Muted, fontSize = 12.sp)
        }
        Text(explanation, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(9.dp))
        entries.forEach { entry ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.title, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Text(String.format(Locale.ROOT, "%.1f/4", entry.average), color = Mint, fontSize = 13.sp)
            }
            LinearProgressIndicator(
                progress = { ((entry.average ?: 0.0) / 4.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Ответов ${entry.answered}/${entry.total}", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ReportContextCard(entry: JourneyModuleResult) {
    SoftCard {
        Text(entry.title, fontWeight = FontWeight.SemiBold)
        Text("Отвечено ${entry.answered}/${entry.total} · ${entry.period}",
            color = Muted, fontSize = 12.sp)
        if (entry.average != null) {
            Text(String.format(Locale.ROOT, "Среднее согласие: %.1f из 4", entry.average), color = Mint)
            Text(
                if (entry.moduleId == "values") "Большее согласие означает интерес к занятиям, а не проблему."
                else "Это ответы о нагрузке и доступных ресурсах, не диагноз.",
                color = Muted, fontSize = 12.sp
            )
        }
        if (entry.highlightedStatements.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text("Часто отмечено:", color = Mint, fontSize = 13.sp)
            entry.highlightedStatements.forEach { Text("• $it", fontSize = 13.sp) }
        } else if (entry.average == null) {
            Text("Нет часто отмеченных утверждений либо пока мало данных. Это не означает, что трудностей нет.",
                color = Muted, fontSize = 12.sp)
        }
    }
}

/** The final page of an individual module, including the separately gated adult PHQ-4. */
@Composable
internal fun SingleSurveyReportScreen(
    module: SurveyModule,
    answers: Map<String, Int>,
    onReview: () -> Unit,
    onFinish: () -> Unit
) {
    val report = remember(module, answers) { JourneyReportEngine.single(module, answers) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TextButton(onClick = onFinish) { Text("← Все опросы") }
        Text("Результат опроса", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(module.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text("Отвечено ${report.answered} из ${report.total} · ${module.period}", color = Mint)
        SoftCard(highlight = true) {
            when {
                module.evidenceBased -> {
                    val total = AssessmentCatalog.phq4Total(answers)
                    if (total == null) Text("Для результата PHQ-4 нужно ответить на все четыре вопроса.")
                    else {
                        Text("PHQ-4: $total из 12", fontWeight = FontWeight.Bold)
                        val (worry, mood) = AssessmentCatalog.phq4Parts(answers)!!
                        Text("Тревожные симптомы: $worry/6 · настроение: $mood/6")
                        Text(
                            if (total >= 6 || worry >= 3 || mood >= 3)
                                "По этому скринингу стоит обсудить состояние со специалистом. Это не диагноз."
                            else "Скрининг не исключает трудностей. Помощь уместна при любом результате.",
                            color = Muted
                        )
                    }
                }
                module.kind == "ipip" -> {
                    if (report.average == null) Text("Для расчёта этого самоописания нужны все ${report.total} ответов.")
                    else Text(String.format(Locale.ROOT,
                        "Средняя оценка с учётом обратных вопросов: %.1f/4", report.average))
                    Text("IPIP — самоописание, русский перевод не валидирован. Это не диагноз и не тип личности.",
                        color = Muted)
                }
                report.highlightedStatements.isNotEmpty() -> {
                    Text("Что ты часто отмечал:", fontWeight = FontWeight.SemiBold)
                    report.highlightedStatements.forEach { Text("• $it", fontSize = 14.sp) }
                    Text("В этом опросе разные направления вопросов; их среднее не является оценкой проблемы.",
                        color = Muted)
                }
                report.average != null -> {
                    Text(String.format(Locale.ROOT, "Среднее согласие с утверждениями: %.1f/4", report.average))
                    Text(
                        if (module.id == "values") "Это интересы, а не проблемы."
                        else "Это ответы о повседневной нагрузке и ресурсах, не диагноз.",
                        color = Muted
                    )
                }
                else -> Text(
                    if (report.answered == 0) "Пока нет ответов. Можно вернуться к вопросам."
                    else "Ответы сохранены. Нет часто отмеченных утверждений; это не означает, что трудностей нет."
                )
            }
        }
        Text("Если переживания мешают жизни, можно обсудить их со специалистом независимо от результата.",
            color = Muted, fontSize = 12.sp)
        OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Изменить ответы") }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Готово") }
    }
}
