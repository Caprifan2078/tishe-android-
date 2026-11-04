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

# This is a QA-only test extension, not a new consumer release.
assert target.stat().st_size > 2000
print("Installed Android Compose instrumentation smoke tests + non-invasive list tag.")
