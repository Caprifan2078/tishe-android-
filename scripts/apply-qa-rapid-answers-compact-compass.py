from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")

def replace_once(source, old, new, name):
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"{name}: expected exactly 1 anchor, got {count}: {old[:85]!r}")
    return source.replace(old, new)

# A single answer already has a checked encrypted commit with UI-level gating.
# Add an optional fast-navigation gesture, BUT advance only when saved answers
# observed from the parent change after persistence confirmation.
journey = root / "JourneyUi.kt"
s = journey.read_text(encoding="utf-8")
s = replace_once(s, "import androidx.compose.ui.Alignment",
    "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.platform.testTag",
    "journey quick mode tag import")
s = replace_once(s,
    "    if (showResults) {\n        BackHandler { showResults = false }\n        JourneyReportScreen(",
    """    // One tap answers; navigation is triggered only after the encrypted
    // parent state reflects the confirmed write. Multiple taps on the same
    // question cannot skip multiple questions or create an unsaved checkpoint.
    var quickMode by rememberSaveable(level.name) { mutableStateOf(false) }
    var pendingAdvance by remember(level.name) { mutableStateOf<Pair<String, Int>?>(null) }
    LaunchedEffect(pendingAdvance, answers, index) {
        val target = pendingAdvance
        if (target != null && questions[index].question.id == target.first &&
            answers[target.first] == target.second) {
            pendingAdvance = null
            if (index == questions.lastIndex) showResults = true
            else onMove(index + 1)
        }
    }
    if (showResults) {
        BackHandler { showResults = false }
        JourneyReportScreen(""",
    "journey confirmed auto-advance")
s = replace_once(s,
    '        Text(level.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)',
    '''        Text(level.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Быстрые ответы · переход после сохранения", color = Muted, fontSize = 12.sp,
                modifier = Modifier.weight(1f))
            Switch(
                checked = quickMode,
                onCheckedChange = { quickMode = it; pendingAdvance = null },
                modifier = Modifier.testTag("journey_quick_mode")
            )
        }''',
    "journey quick toggle")
s = replace_once(s,
    '{ onAnswer(item.question.id, value) },',
    '''{
                        if (quickMode) pendingAdvance = item.question.id to value
                        onAnswer(item.question.id, value)
                    },''',
    "journey answer click")
# Keep a short honest caution near answers; remove repetitive multi-line blurb
# that is already documented on the result screen.
s = replace_once(s,
    '            Text("Ответы используются только для самонаблюдения и подбора занятий. Это не диагностика; длинный опрос не гарантирует точного знания личности.", fontSize = 12.sp, color = Muted)',
    '            Text("Самонаблюдение, не диагноз.", fontSize = 12.sp, color = Muted)',
    "journey recurring verbose footer")
journey.write_text(s, encoding="utf-8")

survey = root / "AssessmentUi.kt"
s = survey.read_text(encoding="utf-8")
s = replace_once(s, "import androidx.compose.ui.Alignment",
    "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.platform.testTag",
    "single quick mode tag import")
s = replace_once(s,
    "    if (showResults) {\n        BackHandler { showResults = false }\n        SingleSurveyReportScreen(",
    """    var quickMode by rememberSaveable(module.id) { mutableStateOf(false) }
    var pendingAdvance by remember(module.id) { mutableStateOf<Pair<String, Int>?>(null) }
    LaunchedEffect(pendingAdvance, answers, index) {
        val target = pendingAdvance
        if (target != null && module.questions[index].id == target.first &&
            answers[target.first] == target.second) {
            pendingAdvance = null
            if (index == module.questions.lastIndex) showResults = true
            else index++
        }
    }
    if (showResults) {
        BackHandler { showResults = false }
        SingleSurveyReportScreen(""",
    "survey confirmed auto-advance")
s = replace_once(s,
    '        Text(module.title, fontSize = 25.sp, fontWeight = FontWeight.Bold)',
    '''        Text(module.title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Быстрые ответы · переход после сохранения", color = Muted, fontSize = 12.sp,
                modifier = Modifier.weight(1f))
            Switch(
                checked = quickMode,
                onCheckedChange = { quickMode = it; pendingAdvance = null },
                modifier = Modifier.testTag("survey_quick_mode")
            )
        }''',
    "survey quick toggle")
s = replace_once(s,
    '{ onAnswer(question.id, value) },',
    '''{
                        if (quickMode) pendingAdvance = question.id to value
                        onAnswer(question.id, value)
                    },''',
    "survey answer click")
s = replace_once(s,
    '''            Text(if (module.kind == "ipip")
                "IPIP Big-Five Factor Markers: оригинальная методика открыта; русский перевод здесь НЕ валидирован. Оценка отражает ответы на вопросы, без сравнения с нормой."
            else if (module.evidenceBased)
                "PHQ-4 — скрининг симптомов, не постановка диагноза. Результат не должен использоваться для самостоятельного назначения лечения."
            else "Авторский опрос: не прошёл психометрическую валидацию. Ответы описывают твой опыт, а не «тип личности». Можно пропускать вопросы.",
                color = Muted, fontSize = 12.sp)''',
    '''            Text("Самонаблюдение, не диагноз.", color = Muted, fontSize = 12.sp)''',
    "survey repeated prose")
survey.write_text(s, encoding="utf-8")

# The six domains measured in the short questionnaire are NOT the 18 current
# feelings. Show the actual automatic six-domain graph first after completion,
# and label the unmeasured 18 feelings honestly instead of presenting an empty
# "general result" as if a calculation were broken.
compass = root / "SuperCompassOverview.kt"
s = compass.read_text(encoding="utf-8")
s = replace_once(s,
    '    val marked = EmotionCompass.axes.count { current.containsKey(it.id) }',
    '''    val marked = EmotionCompass.axes.count { current.containsKey(it.id) }
    val completedShortSurvey = remember(assessmentAnswers) {
        AssessmentAnswerSummary.firstJourneyCompleted(assessmentAnswers)
    }''',
    "completed short survey detection")
s = replace_once(s,
    '''    SoftCard(highlight = true) {
        Text("🧭 Общая карта чувств", fontSize = 21.sp, fontWeight = FontWeight.Bold)''',
    '''    if (completedShortSurvey) {
        Text("Твой профиль по 40 ответам", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        QuestionnaireResultsCard(assessmentAnswers)
        Spacer(Modifier.height(15.dp))
    }

    SoftCard(highlight = !completedShortSurvey) {
        Text("🧭 18 чувств прямо сейчас", fontSize = 21.sp, fontWeight = FontWeight.Bold)''',
    "show calculated profile first, not empty present-day axes")
s = replace_once(s,
    '''            "Все 18 чувств на одной схеме. Отмечено: $marked из 18. " +
                "Пустая грань — не ноль, а отсутствие ответа.",''',
    '''            "Текущие отметки: $marked из 18. " +
                (if (completedShortSurvey) "40 вопросов уже учтены в профиле выше. Здесь отмечай, что чувствуешь сейчас."
                 else "Ответы на тест не являются измерением чувств прямо сейчас."),''',
    "explain independent current feelings")
s = replace_once(s,
    '''        Text(
            "Диаграмма — только визуализация твоих отметок, не научный тест. " +
                "Ниже можно поставить 0–5 для каждого чувства.",
            color = Muted, fontSize = 12.sp
        )''',
    '''        Text("Пустые чувства не считаются нулями. Их можно отметить ниже — это необязательно.",
            color = Muted, fontSize = 12.sp)''',
    "reduce 18 axis boilerplate")
s = replace_once(s,
    '''    Spacer(Modifier.height(15.dp))
    QuestionnaireResultsCard(assessmentAnswers)

    Spacer(Modifier.height(15.dp))
    Text("На что можно обратить внимание",''',
    '''    if (!completedShortSurvey) {
        Spacer(Modifier.height(15.dp))
        QuestionnaireResultsCard(assessmentAnswers)
    }

    Spacer(Modifier.height(15.dp))
    Text("На что можно обратить внимание",''',
    "avoid duplicate survey chart")
compass.write_text(s, encoding="utf-8")

# First screen: a short actionable conclusion, optional evidence and limitations.
narrative = root / "QuestionnaireNarrativeUi.kt"
s = narrative.read_text(encoding="utf-8")
s = replace_once(s,
    'import androidx.compose.runtime.Composable',
    'import androidx.compose.runtime.*',
    "narrative state")
s = replace_once(s,
    'internal fun QuestionnaireNarrativeCard(result: QuestionnaireNarrative, conclusion: DirectTestConclusion) {\n    SoftCard',
    'internal fun QuestionnaireNarrativeCard(result: QuestionnaireNarrative, conclusion: DirectTestConclusion) {\n    var showDetails by remember { mutableStateOf(false) }\n    SoftCard',
    "narrative fold state")
s = replace_once(s,
    '        if (result.evidence.isNotEmpty()) {',
    '''        TextButton(onClick = { showDetails = !showDetails }) {
            Text(if (showDetails) "Скрыть объяснения" else "Подробнее: почему такой результат")
        }
        if (showDetails) {
        if (result.evidence.isNotEmpty()) {''',
    "hide supporting evidence by default")
ending = "    }\n}\n"
if not s.endswith(ending):
    raise RuntimeError("Narrative end brace layout changed")
s = s[:-len(ending)] + "        }\n" + ending
narrative.write_text(s, encoding="utf-8")

# Preserve the main six-domain chart by default; shorten the duplicated
# general caveat in the result screen. Long explanatory evidence is accessible
# via the collapsed detail card above.
report = root / "JourneyReportUi.kt"
s = report.read_text(encoding="utf-8")
s = replace_once(s,
    '''            Text("Переключай слои: переживания, особенности самоописания и жизненный контекст. Несопоставимые данные не объединяются в один балл.",
                color = Muted, fontSize = 12.sp)''',
    '''            Text("Выбери слой данных для графика.", color = Muted, fontSize = 12.sp)''',
    "concise journey legend")
s = replace_once(s,
    '''        item {
            Text("Как понимать результат", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Частые отметки тревоги или гнева означают только, что эти переживания встречались в указанный период. Это не означает, что ты «тревожный человек» или что у тебя расстройство. Невысокие оценки тоже не исключают трудностей.",
                color = Muted, fontSize = 13.sp)
        }''',
    '''        item {
            Text("Это самоописание за указанный период, не диагноз.",
                color = Muted, fontSize = 12.sp)
        }''',
    "remove repeated caution")
s = replace_once(s,
    '''            Text("Результаты пересчитываются локально после изменения ответов. Шкалы из теста не подменяют личную отметку чувств.", color = Muted, fontSize = 12.sp)''',
    '''            Text("Профиль теста и 18 чувств сейчас — разные данные.", color = Muted, fontSize = 12.sp)''',
    "clarify survey/feelings")
report.write_text(s, encoding="utf-8")

tests = Path("app/src/androidTest/java/ru/tishe/app")
if tests.is_dir():
    dest = tests / "RapidAnswerAndSurveyCompassUiTest.kt"
    if dest.exists():
        raise RuntimeError("Rapid answer UI test already installed")
    shutil.copyfile("scripts/RapidAnswerAndSurveyCompassUiTest.kt", dest)

print("Rapid answer mode: advance only after saved answer, survey graph first, optional narrative details")
