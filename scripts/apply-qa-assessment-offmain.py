from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
app = root / "AppUi.kt"
code = app.read_text(encoding="utf-8")

def once(before: str, after: str, label: str):
    global code
    count = code.count(before)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one anchor, got {count}: {before[:90]!r}")
    code = code.replace(before, after)

once(
    "import kotlinx.coroutines.withContext",
    "import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.CancellationException\nimport androidx.compose.ui.window.Dialog",
    "coroutine/Dialog imports"
)
once(
    "    var saveError by remember { mutableStateOf(false) }",
    """    var saveError by remember { mutableStateOf(false) }
    // Never acknowledge an answer before encrypted storage confirms it.
    // The gate is set synchronously on main, before the IO coroutine is scheduled,
    // so rapidly repeated taps cannot queue stale full-document snapshots.
    var assessmentSaving by remember { mutableStateOf(false) }
    val assessmentScope = rememberCoroutineScope()
    val assessmentWriter = remember(store) {
        AssessmentAsyncWriter { next, durable -> store.saveAssessment(next, durable) }
    }

    fun persistAssessment(
        next: AssessmentState,
        durable: Boolean = true,
        afterConfirmed: () -> Unit = {}
    ) {
        if (assessmentSaving) return
        assessmentSaving = true
        assessmentScope.launch {
            try {
                assessmentWriter.save(next, durable)
                // This runs on main only AFTER commit()/apply() returned successfully.
                assessment = next
                afterConfirmed()
            } catch (cancelled: CancellationException) {
                // Activity recreation may cancel the UI scope while an IO commit is
                // finishing; the recreated Activity reloads from encrypted storage.
                throw cancelled
            } catch (_: Exception) {
                // Do not expose a failed answer in the UI; existing recovery dialog.
                saveError = true
            } finally {
                assessmentSaving = false
            }
        }
    }""",
    "assessment lifecycle"
)
once(
    "BackHandler(enabled = safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME) {\n",
    "BackHandler(enabled = assessmentSaving || safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME) {\n        if (assessmentSaving) return@BackHandler\n",
    "Android Back while saving"
)
once(
    "                            persist({ store.saveAssessment(next, durable = false) }) { assessment = next }",
    "                            persistAssessment(next, durable = false)",
    "non-durable question cursor"
)
old_journey = '''                            onJourney = { level ->
                                // Do not open a new questionnaire if its initial checkpoint
                                // failed to save: the user would otherwise trust unsaved progress.
                                val ready = if (level.name in assessment.journeyPositions) true else {
                                    val nextQuestion = AssessmentJourney.nextUnanswered(level, assessment.answers) ?: 0
                                    val next = assessment.copy(journeyPositions = assessment.journeyPositions + (level.name to nextQuestion))
                                    persist({ store.saveAssessment(next) }) { assessment = next }
                                }
                                if (ready) journey = level
                            },'''
new_journey = '''                            onJourney = { level ->
                                // Only open a new journey after its first checkpoint is confirmed.
                                // Already checkpointed journeys need no disk write to open.
                                if (assessmentSaving) return@ProfileScreen
                                if (level.name in assessment.journeyPositions) {
                                    journey = level
                                } else {
                                    val nextQuestion = AssessmentJourney.nextUnanswered(level, assessment.answers) ?: 0
                                    val next = assessment.copy(journeyPositions = assessment.journeyPositions + (level.name to nextQuestion))
                                    persistAssessment(next) { journey = level }
                                }
                            },'''
# Avoid an invalid label return@ProfileScreen in a nested callback; use if guard instead.
new_journey = new_journey.replace(
    '                                if (assessmentSaving) return@ProfileScreen\n                                if (level.name in assessment.journeyPositions) {',
    '                                if (!assessmentSaving && level.name in assessment.journeyPositions) {'
).replace(
    '                                } else {\n                                    val nextQuestion = AssessmentJourney.nextUnanswered(level, assessment.answers) ?: 0',
    '                                } else if (!assessmentSaving) {\n                                    val nextQuestion = AssessmentJourney.nextUnanswered(level, assessment.answers) ?: 0'
)
once(old_journey, new_journey, "journey initial checkpoint")

old = "persist({ store.saveAssessment(next) }) { assessment = next }"
count = code.count(old)
if count != 3:
    raise RuntimeError(f"Expected 3 answer/preference callbacks after journey replacement, got {count}")
code = code.replace(old, "persistAssessment(next)")

if code.count("store.saveAssessment(") != 1:
    raise RuntimeError("Unexpected remaining synchronous saveAssessment call")
if code.count("persistAssessment(next)") != 3 or code.count("persistAssessment(next, durable = false)") != 1:
    raise RuntimeError("Missing replacement of answer/cursor callbacks")

once(
    "        if (saveError && !safetyOpen) {",
    '''        if (assessmentSaving) {
            // A modal input barrier is deliberate: no second answer or navigation
            // is accepted while the first encrypted snapshot is unconfirmed.
            // Cancellation/rotation cannot produce a falsely acknowledged answer.
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Panel
                ) {
                    Row(
                        Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Сохраняю изменения…", color = Accent)
                    }
                }
            }
        }
        if (saveError && !safetyOpen) {''',
    "input barrier UI"
)
app.write_text(code, encoding="utf-8")

source = Path("scripts/AssessmentAsyncWriter.kt")
dest = root / source.name
if dest.exists():
    raise RuntimeError("Writer source already present")
shutil.copyfile(source, dest)

test_folder = Path("app/src/androidTest/java/ru/tishe/app")
if test_folder.is_dir():
    target = test_folder / "AssessmentAsyncWriterTest.kt"
    if target.exists():
        raise RuntimeError("Writer Android tests already present")
    shutil.copyfile("scripts/AssessmentAsyncWriterTest.kt", target)

print("QA off-main assessment writes: synchronous UI gate, Dispatchers.IO commit, confirm-before-display")
