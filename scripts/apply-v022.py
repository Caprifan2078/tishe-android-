from pathlib import Path
import shutil

src = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
for filename, target_dir in [
    ("SuperCompassSignals.kt", src),
    ("SuperCompassOverview.kt", src),
    ("SuperCompassSignalsTest.kt", tests),
]:
    target = target_dir / filename
    if target.exists():
        raise RuntimeError("Refusing to overwrite existing source: " + str(target))
    shutil.copyfile(Path("scripts") / filename, target)

path = src / "CompassUi.kt"
source = path.read_text(encoding="utf-8")
def change(before, after):
    global source
    count = source.count(before)
    if count != 1:
        raise RuntimeError("Compass layout mismatch " + str(count) + ": " + before[:90])
    source = source.replace(before, after)

change("    var group by remember { mutableStateOf(FeelingGroup.TENSION) }",
       "    var group by remember { mutableStateOf<FeelingGroup?>(null) }")
change("    val focused = remember(group) { EmotionCompass.axes.filter { it.group == group } }",
       """    val focused = remember(group) {
        if (group == null) EmotionCompass.axes else EmotionCompass.axes.filter { it.group == group }
    }
    val groupColor = group?.let(::tone) ?: Accent""")
change("SectionHeading(\"✦ Эмоциональный компас\", \"Много чувств одновременно\", \"От 0 — не ощущаю до 5 — очень сильно. Даже противоположные чувства могут существовать вместе.\")",
       "SectionHeading(\"✦ СУПЕРКОМПАС\", \"Одна карта твоего опыта\", \"18 чувств сейчас, 6 тем из ответов и бережные подсказки. Не диагноз и не поиск травм.\")")
change("            QuestionnaireResultsCard(assessmentAnswers)",
       "            SuperCompassOverview(levels, assessmentAnswers, history, onStart)")
change("""                FeelingGroup.entries.forEach { item ->
                    FilterChip(""",
       """                FilterChip(selected = group == null, onClick = { group = null },
                    label = { Text("Все 18", fontSize = 12.sp) })
                FeelingGroup.entries.forEach { item ->
                    FilterChip(""")
change("Text(group.title, fontWeight = FontWeight.Bold, color = tone(group), fontSize = 18.sp)",
       "Text(group?.title ?: \"Все 18 чувств\", fontWeight = FontWeight.Bold, color = groupColor, fontSize = 18.sp)")
change("RadarDiagram(focused, levels, tone(group))",
       "RadarDiagram(focused, levels, groupColor)")
change("color = tone(group), fontWeight = FontWeight.Bold",
       "color = groupColor, fontWeight = FontWeight.Bold")
change("Text(\"Каждая грань — отдельное чувство, а не оценка личности.\"",
       "Text(\"Все чувства можно отметить сразу или выбрать одну группу. Это не оценка личности.\"")
path.write_text(source, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
g = gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 28") == 1
assert g.count('versionName = "0.21.0"') == 1
g = g.replace("versionCode = 28", "versionCode = 29")
g = g.replace('versionName = "0.21.0"', 'versionName = "0.22.0"')
gradle.write_text(g, encoding="utf-8")

assert "SuperCompassOverview(levels, assessmentAnswers, history, onStart)" in source
assert "group?.let(::tone) ?: Accent" in source
print("Tishe 0.22: integrated super-compass, separate survey scales, transparent reflection themes, no new personal data fields.")
