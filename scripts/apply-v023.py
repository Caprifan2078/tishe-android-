from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)

for filename, destination in [
    ("JourneyReportEngine.kt", root),
    ("JourneyReportUi.kt", root),
    ("JourneyReportEngineTest.kt", tests),
]:
    target = destination / filename
    if target.exists():
        raise RuntimeError("Unexpected pre-existing file: " + str(target))
    shutil.copyfile(Path("scripts") / filename, target)

def edit(filename, before, after, expected=1):
    path = root / filename
    data = path.read_text(encoding="utf-8")
    found = data.count(before)
    if found != expected:
        raise RuntimeError(f"{filename}: expected {expected} match(es), got {found}: {before[:70]!r}")
    path.write_text(data.replace(before, after), encoding="utf-8")

edit("JourneyUi.kt", "import androidx.compose.runtime.*",
     "import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable")
edit("JourneyUi.kt",
    "    onMove: (Int) -> Unit,\n    onBack: () -> Unit\n)",
    "    onMove: (Int) -> Unit,\n    onBack: () -> Unit,\n    onCompass: () -> Unit\n)")
edit("JourneyUi.kt",
    "    val progress = remember(level, answers, index) { AssessmentJourney.progress(level, answers, index) }",
    """    val progress = remember(level, answers, index) { AssessmentJourney.progress(level, answers, index) }
    var showResults by rememberSaveable(level.name) { mutableStateOf(false) }
    if (showResults) {
        JourneyReportScreen(level, answers, onReview = { showResults = false },
            onFinish = onBack, onCompass = onCompass)
        return
    }""")
edit("JourneyUi.kt",
    '        TextButton(onClick = onBack) { Text("←  К режимам", color = Accent) }',
    '''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("←  К режимам", color = Accent) }
            TextButton(onClick = { showResults = true }) { Text("Мои результаты", color = Mint) }
        }''')
edit("JourneyUi.kt",
     'if (index == questions.lastIndex) onBack() else onMove(index + 1)',
     'if (index == questions.lastIndex) showResults = true else onMove(index + 1)',
     expected=2)
edit("JourneyUi.kt",
     'if (next == null) onBack() else onMove(next)',
     'if (next == null) showResults = true else onMove(next)')

edit("AssessmentUi.kt", "import androidx.compose.runtime.*",
     "import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable")
edit("AssessmentUi.kt",
    '''    var index by remember(module.id) {
        mutableIntStateOf(module.questions.indexOfFirst { it.id !in answers }.takeIf { it >= 0 } ?: 0)
    }''',
    '''    var index by remember(module.id) {
        mutableIntStateOf(module.questions.indexOfFirst { it.id !in answers }.takeIf { it >= 0 } ?: 0)
    }
    var showResults by rememberSaveable(module.id) { mutableStateOf(false) }
    if (showResults) {
        SingleSurveyReportScreen(module, answers, onReview = { showResults = false }, onFinish = onBack)
        return
    }''')
edit("AssessmentUi.kt",
    '''        TextButton(onClick = onBack) { Text("←  Все опросы", color = Accent) }''',
    '''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("←  Все опросы", color = Accent) }
            TextButton(onClick = { showResults = true }) { Text("Мой результат", color = Mint) }
        }''')
edit("AssessmentUi.kt",
     'if (index == module.questions.lastIndex) onBack() else index++',
     'if (index == module.questions.lastIndex) showResults = true else index++',
     expected=2)

edit("AppUi.kt",
     '                    }, onBack = { journey = null })',
     '                    }, onBack = { journey = null },\n                    onCompass = { journey = null; page = Page.COMPASS })')

gradle = Path("app/build.gradle.kts")
content = gradle.read_text(encoding="utf-8")
if content.count("versionCode = 29") != 1 or content.count('versionName = "0.22.0"') != 1:
    raise RuntimeError("Expected v0.22 source with code 29; refusing to apply patch twice")
gradle.write_text(content.replace("versionCode = 29", "versionCode = 30")
    .replace('versionName = "0.22.0"', 'versionName = "0.23.0"'), encoding="utf-8")

assert "showResults = true" in (root / "JourneyUi.kt").read_text()
assert "JourneyReportScreen" in (root / "JourneyUi.kt").read_text()
assert "SingleSurveyReportScreen" in (root / "AssessmentUi.kt").read_text()
print("Tishe v0.23.0: questionnaire screens have actual results destinations for all routes and modules")
