from pathlib import Path
import shutil

tests = Path("app/src/androidTest/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
target = tests / "QuestionnaireUiSmokeTest.kt"
if target.exists():
    raise RuntimeError("UI tests already installed")
shutil.copyfile("scripts/QuestionnaireUiSmokeTest.kt", target)

build = Path("app/build.gradle.kts")
s = build.read_text(encoding="utf-8")
needle = '    testImplementation("org.json:json:20240303")'
if s.count(needle) != 1:
    raise RuntimeError("Unexpected Gradle test dependency section")
s = s.replace(needle, needle + '''
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")''')
build.write_text(s, encoding="utf-8")

ui = Path("app/src/main/java/ru/tishe/app/JourneyReportUi.kt")
code = ui.read_text(encoding="utf-8")
imp = 'import androidx.compose.ui.Modifier'
if code.count(imp) != 1: raise RuntimeError("Unexpected UI imports")
code = code.replace(imp, imp + '\nimport androidx.compose.ui.platform.testTag')
needle = '        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),'
if code.count(needle) != 1: raise RuntimeError("Result LazyColumn not found")
code = code.replace(needle,
                    '        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().testTag("journey_result_list"),', 1)
ui.write_text(code, encoding="utf-8")

# Deterministic semantic anchors for a real end-to-end encrypted-answer smoke check.
app_ui = Path("app/src/main/java/ru/tishe/app/AppUi.kt")
app = app_ui.read_text(encoding="utf-8")
imp = "import androidx.compose.ui.Modifier"
if app.count(imp) != 1: raise RuntimeError("AppUi import mismatch")
app = app.replace(imp, imp + '\nimport androidx.compose.ui.platform.testTag')
nav = "                                NavigationBarItem(\n                                    selected = page == target,"
if app.count(nav) != 1: raise RuntimeError("Bottom navigation anchor mismatch")
app = app.replace(nav, '''                                NavigationBarItem(
                                    modifier = Modifier.testTag("nav_${target.name.lowercase()}"),
                                    selected = page == target,''')
app_ui.write_text(app, encoding="utf-8")

assessment_ui = Path("app/src/main/java/ru/tishe/app/AssessmentUi.kt")
a = assessment_ui.read_text(encoding="utf-8")
if a.count(imp) != 1: raise RuntimeError("AssessmentUi import mismatch")
a = a.replace(imp, imp + '\nimport androidx.compose.ui.platform.testTag')
profile = "modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),"
if a.count(profile) != 1: raise RuntimeError("Profile LazyColumn anchor mismatch")
a = a.replace(profile, 'modifier = modifier.fillMaxSize().testTag("profile_test_list"), contentPadding = PaddingValues(20.dp),')
assessment_ui.write_text(a, encoding="utf-8")


# This is a QA-only test extension, not a new consumer release.
assert target.stat().st_size > 2000
print("Installed Android Compose instrumented tests plus stable navigation semantics for encrypted-answer E2E check.")
