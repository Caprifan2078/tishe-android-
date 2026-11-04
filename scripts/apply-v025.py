from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
test_root = Path("app/src/test/java/ru/tishe/app")
for name, destination in [
    ("DirectTestConclusionEngine.kt", root),
    ("DirectTestConclusionEngineTest.kt", test_root),
]:
    target = destination / name
    if target.exists(): raise RuntimeError("Already patched: " + str(target))
    shutil.copyfile(Path("scripts") / name, target)

narrative = root / "QuestionnaireNarrativeUi.kt"
s = narrative.read_text(encoding="utf-8")
def replace_once(text, before, after):
    if text.count(before) != 1:
        raise RuntimeError("Expected exactly one match, got " + str(text.count(before)) + ": " + repr(before[:80]))
    return text.replace(before, after)
s = replace_once(s,
    "internal fun QuestionnaireNarrativeCard(result: QuestionnaireNarrative) {",
    "internal fun QuestionnaireNarrativeCard(result: QuestionnaireNarrative, conclusion: DirectTestConclusion) {")
s = replace_once(s, '''        Text(result.headline, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Spacer(Modifier.height(8.dp))
        Text(result.overview, fontSize = 14.sp, lineHeight = 21.sp)''',
    '''        Text("Итог теста", fontWeight = FontWeight.SemiBold, color = Mint, fontSize = 15.sp)
        Spacer(Modifier.height(7.dp))
        Text(conclusion.headline, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 27.sp)
        Spacer(Modifier.height(8.dp))
        Text(conclusion.explanation, fontSize = 14.sp, lineHeight = 21.sp)
        if (conclusion.specifics.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            conclusion.specifics.forEach { detail ->
                Text("• $detail", fontSize = 13.sp)
                Spacer(Modifier.height(5.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Что делать дальше", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(conclusion.nextStep, fontSize = 13.sp, lineHeight = 19.sp)''')
narrative.write_text(s, encoding="utf-8")

report_ui = root / "JourneyReportUi.kt"
s = report_ui.read_text(encoding="utf-8")
s = replace_once(s,
    "    val narrative = remember(level, answers) { QuestionnaireNarrativeEngine.forJourney(level, answers) }",
    """    val narrative = remember(level, answers) { QuestionnaireNarrativeEngine.forJourney(level, answers) }
    val direct = remember(level, answers) { DirectTestConclusionEngine.journey(level, answers) }""")
s = replace_once(s, "        item { QuestionnaireNarrativeCard(narrative) }",
    "        item { QuestionnaireNarrativeCard(narrative, direct) }")
s = replace_once(s,
    "    val narrative = remember(module, answers) { QuestionnaireNarrativeEngine.forSingle(module, answers) }",
    """    val narrative = remember(module, answers) { QuestionnaireNarrativeEngine.forSingle(module, answers) }
    val direct = remember(module, answers) { DirectTestConclusionEngine.single(module, answers) }""")
# Show the plain result first (old calculated details remain below it).
s = replace_once(s, "        QuestionnaireNarrativeCard(narrative)\n", "")
single = s.index("internal fun SingleSurveyReportScreen(")
first_card = s.index('        SoftCard(highlight = true) {', single)
s = s[:first_card] + "        QuestionnaireNarrativeCard(narrative, direct)\n" + s[first_card:]
report_ui.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
g = gradle.read_text(encoding="utf-8")
if g.count('versionName = "0.24.0"') != 1 or g.count("versionCode = 31") != 1:
    raise RuntimeError("Requires clean v0.24.0 source / versionCode 31")
gradle.write_text(g.replace('versionName = "0.24.0"', 'versionName = "0.25.0"').replace(
    "versionCode = 31", "versionCode = 32"), encoding="utf-8")

assert "QuestionnaireNarrativeCard(narrative, direct)" in s
print("Tishe 0.25.0: direct result and action first; supporting evidence remains available.")
