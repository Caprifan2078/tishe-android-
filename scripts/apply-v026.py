from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
test_root = Path("app/src/test/java/ru/tishe/app")
test_root.mkdir(parents=True, exist_ok=True)

for name, dest in [
    ("AssessmentResultEntryPolicy.kt", root),
    ("AssessmentResultEntryPolicyTest.kt", test_root)
]:
    target = dest / name
    if target.exists():
        raise RuntimeError("v026 already applied: " + str(target))
    shutil.copyfile(Path("scripts") / name, target)

def exact(filename, before, after):
    target = root / filename
    text = target.read_text(encoding="utf-8")
    if text.count(before) != 1:
        raise RuntimeError(f"{filename}: expected one match, got {text.count(before)} for {before[:85]!r}")
    target.write_text(text.replace(before, after), encoding="utf-8")

# Android system Back on a report returns to its questions, not out of assessment.
for filename in ("JourneyUi.kt", "AssessmentUi.kt"):
    target = root / filename
    source = target.read_text(encoding="utf-8")
    import_line = "import androidx.compose.runtime.saveable.rememberSaveable"
    if source.count(import_line) != 1:
        raise RuntimeError("Expected saveable import in " + filename)
    source = source.replace(import_line,
        import_line + "\nimport androidx.activity.compose.BackHandler", 1)
    target.write_text(source, encoding="utf-8")

exact("JourneyUi.kt",
    '    var showResults by rememberSaveable(level.name) { mutableStateOf(false) }',
    '''    // Old completed answers must lead straight to their saved report on reopening.
    // The user can still choose "Вернуться к ответам" from the report.
    var showResults by rememberSaveable(level.name) {
        mutableStateOf(AssessmentResultEntryPolicy.completedJourney(level, answers))
    }''')
exact("JourneyUi.kt",
    '    if (showResults) {\n        JourneyReportScreen(',
    '    if (showResults) {\n        BackHandler { showResults = false }\n        JourneyReportScreen(')


exact("AssessmentUi.kt",
    '    var showResults by rememberSaveable(module.id) { mutableStateOf(false) }',
    '''    // Use the same behavior for each stand-alone module, without touching its answers.
    var showResults by rememberSaveable(module.id) {
        mutableStateOf(AssessmentResultEntryPolicy.completedModule(module, answers))
    }''')
exact("AssessmentUi.kt",
    '    if (showResults) {\n        SingleSurveyReportScreen(',
    '    if (showResults) {\n        BackHandler { showResults = false }\n        SingleSurveyReportScreen(')


exact("AssessmentUi.kt",
    '    val chosen = answers[question.id]',
    '''    val chosen = answers[question.id]
    val answeredCount = AssessmentResultEntryPolicy.answered(module, answers)''')

exact("AssessmentUi.kt",
    '        LinearProgressIndicator(progress = { (index + 1).toFloat() / module.questions.size }, modifier = Modifier.fillMaxWidth())',
    '''        LinearProgressIndicator(
            progress = { answeredCount.toFloat() / module.questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(5.dp))
        Text("Отвечено: $answeredCount из ${module.questions.size}. Пропуски не засчитываются как ответы.",
            color = Muted, fontSize = 12.sp)''')

gradle = Path("app/build.gradle.kts")
data = gradle.read_text(encoding="utf-8")
if data.count('versionName = "0.25.0"') != 1 or data.count("versionCode = 32") != 1:
    raise RuntimeError("v026 requires precisely 0.25.0/versionCode32 baseline")
gradle.write_text(data.replace('versionName = "0.25.0"', 'versionName = "0.26.0"')
    .replace("versionCode = 32", "versionCode = 33"), encoding="utf-8")
print("Tishe v0.26: completed routes and standalone surveys reopen at results; survey progress counts only actually valid answers")
