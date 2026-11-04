from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
for name, folder in [
    ("QuestionnaireNarrativeEngine.kt", root),
    ("QuestionnaireNarrativeUi.kt", root),
    ("QuestionnaireNarrativeEngineTest.kt", tests)
]:
    target = folder / name
    if target.exists():
        raise RuntimeError("Source already patched: " + str(target))
    shutil.copyfile(Path("scripts") / name, target)

ui = root / "JourneyReportUi.kt"
s = ui.read_text(encoding="utf-8")
a = "    val report = remember(level, answers) { JourneyReportEngine.report(level, answers) }"
assert s.count(a) == 1
s = s.replace(a, a + "\n    val narrative = remember(level, answers) { QuestionnaireNarrativeEngine.forJourney(level, answers) }")
start = s.index('        item {\n            SoftCard(highlight = true) {\n                Text(report.observationTitle')
end = s.index('        item {\n            Text("Многослойная паутинка"', start)
s = s[:start] + "        item { QuestionnaireNarrativeCard(narrative) }\n" + s[end:]

b = "    val report = remember(module, answers) { JourneyReportEngine.single(module, answers) }"
assert s.count(b) == 1
s = s.replace(b, b + "\n    val narrative = remember(module, answers) { QuestionnaireNarrativeEngine.forSingle(module, answers) }")
anchor = '        Text("Результат меняется вместе с ответами.'
assert s.count(anchor) == 1
s = s.replace(anchor, '        QuestionnaireNarrativeCard(narrative)\n' + anchor)
ui.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
g = gradle.read_text(encoding="utf-8")
if g.count('versionName = "0.23.0"') != 1 or g.count("versionCode = 30") != 1:
    raise RuntimeError("Requires v0.23.0 code 30 as unmodified baseline")
g = g.replace('versionName = "0.23.0"', 'versionName = "0.24.0"').replace(
    "versionCode = 30", "versionCode = 31")
gradle.write_text(g, encoding="utf-8")
print("Applied v0.24 evidence-based narrative feedback; screen contains new guided conclusion cards")
