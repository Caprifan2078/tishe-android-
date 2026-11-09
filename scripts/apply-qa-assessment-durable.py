from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
storage, app = root / "SecureStorage.kt", root / "AppUi.kt"

def once(source, before, after, name):
    found = source.count(before)
    if found != 1:
        raise RuntimeError(f"{name}: expected one exact match, got {found}")
    return source.replace(before, after)

s = storage.read_text(encoding="utf-8")
a = app.read_text(encoding="utf-8")
s = once(s,
    "    // The decoded document is process-local; diary remains synchronous. Survey checkpoints use apply().",
    "    // Cached JSON is process-local. Answers and diary writes are durable.\n"
    "    // Recoverable cursor-only moves can use apply().", "storage header")
s = once(s,
    "        // frequent survey checkpoints use SharedPreferences.apply(): its in-memory\n"
    "        // value changes immediately while Android schedules the disk write.",
    "        // Only recoverable cursor updates use apply() asynchronously.\n"
    "        // Actual answers require commit() with a checked success result.", "storage write semantics")
s = once(s,
    "    fun saveAssessment(value: AssessmentState) {",
    "    /** Confirm answers on disk before reporting success; cursor-only moves may be async. */\n"
    "    fun saveAssessment(value: AssessmentState, durable: Boolean = true) {",
    "assessment signature")
s = once(s,
    '        editDocument(durable = false) { it.put("assessment", JSONObject().put("answers", answers).put("preferences", preferences).put("journeyPositions", positions)) }',
    '        editDocument(durable = durable) { it.put("assessment", JSONObject().put("answers", answers).put("preferences", preferences).put("journeyPositions", positions)) }',
    "assessment persistence")

before = '''                    }, onMove = { index ->
                        if (AssessmentChangePolicy.positionChanged(assessment.journeyPositions, mode.name, index)) {
                            val next = assessment.copy(journeyPositions = assessment.journeyPositions + (mode.name to index))
                            persist({ store.saveAssessment(next) }) { assessment = next }
                        }
                    }, onBack = { journey = null }'''
after = '''                    }, onMove = { index ->
                        if (AssessmentChangePolicy.positionChanged(assessment.journeyPositions, mode.name, index)) {
                            val next = assessment.copy(journeyPositions = assessment.journeyPositions + (mode.name to index))
                            // Question position can be reconstructed; only answers need fsync.
                            persist({ store.saveAssessment(next, durable = false) }) { assessment = next }
                        }
                    }, onBack = { journey = null }'''
a = once(a, before, after, "journey cursor")
if a.count('persist({ store.saveAssessment(next) }) { assessment = next }') < 3:
    raise RuntimeError("Answer or preference callback layout changed")
if 'editDocument(durable = false) { it.put("assessment"' in s:
    raise RuntimeError("Unprotected assessment write found")

storage.write_text(s, encoding="utf-8")
app.write_text(a, encoding="utf-8")

test_root = Path("app/src/androidTest/java/ru/tishe/app")
if test_root.is_dir():
    target = test_root / "AssessmentDurableDiskSmokeTest.kt"
    if target.exists():
        raise RuntimeError("Disk smoke test already installed")
    shutil.copyfile(Path("scripts") / target.name, target)
print("Assessment answers are synchronous; cursor-only changes remain asynchronous")
