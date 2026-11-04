from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)

policy = root / "AssessmentResultEntryPolicy.kt"
if "fun firstUnansweredIndex" not in policy.read_text(encoding="utf-8"):
    raise RuntimeError("Requires patched assessment resume policy")

ui = root / "AssessmentUi.kt"
text = ui.read_text(encoding="utf-8")
before = '''    var index by remember(module.id) {
        mutableIntStateOf(module.questions.indexOfFirst { it.id !in answers }.takeIf { it >= 0 } ?: 0)
    }'''
after = '''    // Keep the current question across Android configuration changes.
    // For old/invalid saved values, resume at the first response that needs attention.
    var index by rememberSaveable(module.id) {
        mutableIntStateOf(AssessmentResultEntryPolicy.firstUnansweredIndex(module, answers))
    }'''
if text.count(before) != 1:
    raise RuntimeError("Unexpected questionnaire cursor implementation: " + str(text.count(before)))
ui.write_text(text.replace(before, after), encoding="utf-8")

# Preserve the *parent* navigation destination too. Saving only the child question
# index is ineffective if Activity recreation replaces the entire survey with Home.
app_ui = root / "AppUi.kt"
app = app_ui.read_text(encoding="utf-8")
for original, replacement in [
    ('var survey by remember { mutableStateOf<String?>(null) }',
     'var survey by rememberSaveable { mutableStateOf<String?>(null) }'),
    ('var journey by remember { mutableStateOf<JourneyLevel?>(null) }',
     'var journey by rememberSaveable { mutableStateOf<JourneyLevel?>(null) }'),
    ('var page by remember { mutableStateOf(Page.HOME) }',
     'var page by rememberSaveable { mutableStateOf(Page.HOME) }'),
]:
    if app.count(original) != 1:
        raise RuntimeError("Unexpected route state: " + original)
    app = app.replace(original, replacement)
app_ui.write_text(app, encoding="utf-8")


dest = tests / "AssessmentResumePositionTest.kt"
if dest.exists(): raise RuntimeError("Resume tests already installed")
shutil.copyfile("scripts/AssessmentResumePositionTest.kt", dest)

gradle = Path("app/build.gradle.kts")
s = gradle.read_text(encoding="utf-8")
if s.count('versionName = "0.26.0"') != 1 or s.count("versionCode = 33") != 1:
    raise RuntimeError("Requires v0.26.0 / code 33 baseline")
gradle.write_text(s.replace('versionName = "0.26.0"', 'versionName = "0.27.0"')
    .replace("versionCode = 33", "versionCode = 34"), encoding="utf-8")
print("v0.27.0: survey cursor survives rotation and invalid old answers are not silently skipped.")
