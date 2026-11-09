from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
app_file = root / "AppUi.kt"
s = app_file.read_text(encoding="utf-8")

def once(before, after, title):
    global s
    found = s.count(before)
    if found != 1:
        raise RuntimeError(f"{title}: expected 1 exact match, got {found}")
    s = s.replace(before, after)

once("import kotlinx.coroutines.withContext",
     "import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.CancellationException",
     "coroutine imports")
once("    var assessment by remember(initial) { mutableStateOf(initial.assessment) }",
     """    var assessment by remember(initial) { mutableStateOf(initial.assessment) }
    val assessmentScope = rememberCoroutineScope()
    var assessmentSaving by remember { mutableStateOf(false) }""",
     "assessment scope")

helper = '''    /**
     * Single-flight, confirmed assessment changes. Never acknowledge an answer before
     * the encrypted disk commit succeeds. No concurrent snapshots can overwrite newer
     * responses; repeat actions are temporarily blocked by the saving overlay.
     */
    fun persistAssessment(
        next: AssessmentState,
        durable: Boolean = true,
        afterSaved: () -> Unit = {}
    ) {
        if (assessmentSaving) return
        assessmentSaving = true
        assessmentScope.launch {
            try {
                withContext(Dispatchers.IO) { store.saveAssessment(next, durable = durable) }
                assessment = next
                afterSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveError = true
            } finally {
                assessmentSaving = false
            }
        }
    }

'''
once("    BackHandler(enabled = safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME) {",
     helper + "    BackHandler(enabled = assessmentSaving || safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME) {",
     "BackHandler gate")
once("    BackHandler(enabled = assessmentSaving || safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME) {\n",
     "    BackHandler(enabled = assessmentSaving || safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME) {\n"
     "        if (assessmentSaving) return@BackHandler\n",
     "block system Back during write")

oldOpen = '''                                val ready = if (level.name in assessment.journeyPositions) true else {
                                    val nextQuestion = AssessmentJourney.nextUnanswered(level, assessment.answers) ?: 0
                                    val next = assessment.copy(journeyPositions = assessment.journeyPositions + (level.name to nextQuestion))
                                    persist({ store.saveAssessment(next) }) { assessment = next }
                                }
                                if (ready) journey = level'''
newOpen = '''                                if (level.name in assessment.journeyPositions) {
                                    journey = level
                                } else {
                                    val nextQuestion = AssessmentJourney.nextUnanswered(level, assessment.answers) ?: 0
                                    val next = assessment.copy(journeyPositions = assessment.journeyPositions + (level.name to nextQuestion))
                                    // Do not open questionnaire until its first checkpoint is confirmed.
                                    persistAssessment(next) { journey = level }
                                }'''
once(oldOpen, newOpen, "first journey checkpoint")

common = 'persist({ store.saveAssessment(next) }) { assessment = next }'
count = s.count(common)
if count != 3:
    raise RuntimeError(f"Expected three answer/preference callbacks after opening rewrite; got {count}")
s = s.replace(common, 'persistAssessment(next)')
once('persist({ store.saveAssessment(next, durable = false) }) { assessment = next }',
     'persistAssessment(next, durable = false)',
     "recoverable cursor")

overlay = '''        // A save is deliberately acknowledged only when commit() completes.
        // Modal, non-dismissable feedback prevents conflicting input during IO.
        if (assessmentSaving) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Сохраняю ответ…") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Подтверждаем сохранение в защищённом хранилище.")
                    }
                },
                confirmButton = {}
            )
        }
'''
once("        if (saveError && !safetyOpen) {",overlay + "        if (saveError && !safetyOpen) {",
     "non-dismissible saving feedback")

if 'persist({ store.saveAssessment(' in s:
    raise RuntimeError("Unexpected remaining synchronous assessment callback")
if s.count('persistAssessment(next)') != 4:  # 3 edits plus the confirmed first-journey checkpoint
    raise RuntimeError("Unexpected number of standard assessment changes")
if s.count('persistAssessment(next, durable = false)') != 1:
    raise RuntimeError("Cursor write not routed through IO")

app_file.write_text(s, encoding="utf-8")

tests = Path("app/src/androidTest/java/ru/tishe/app")
if tests.is_dir():
    source = Path("scripts/AssessmentOffMainUiTest.kt")
    target = tests / source.name
    if target.exists() or not source.is_file():
        raise RuntimeError("Off-main UI test missing or already installed")
    shutil.copyfile(source, target)
    existing = tests / "QuestionnaireUiSmokeTest.kt"
    if not existing.is_file():
        raise RuntimeError("Existing UI regression file missing")
    code = existing.read_text(encoding="utf-8")
    old = '''        compose.onNodeWithText("Часто").performClick()
        compose.onNodeWithText("Отвечено: 1/40", substring = true).assertExists()'''
    new = '''        compose.onNodeWithText("Часто").performClick()
        compose.waitUntil(30000L) {
            compose.onAllNodesWithText("Отвечено: 1/40", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Отвечено: 1/40", substring = true).assertExists()'''
    if code.count(old) != 1:
        raise RuntimeError("Expected exactly one synchronous-answer UI test assertion")
    existing.write_text(code.replace(old, new), encoding="utf-8")
print("Off-main assessment persistence installed: confirmed single-flight IO + blocking overlay; existing UI regression adapted")
