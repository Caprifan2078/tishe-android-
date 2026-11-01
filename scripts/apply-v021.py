from pathlib import Path
import shutil

root = Path('app/src/main/java/ru/tishe/app')
tests = Path('app/src/test/java/ru/tishe/app')
for name, dest in [
    ('AssessmentAnswerSummary.kt', root),
    ('AssessmentResultsUi.kt', root),
    ('AssessmentAnswerSummaryTest.kt', tests),
]:
    target = dest / name
    if target.exists(): raise RuntimeError('Refusing to overwrite ' + str(target))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(Path('scripts') / name, target)

def patch(file, old, new):
    text = file.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1: raise RuntimeError(f'Patch mismatch {file} count={count}: {old[:90]}')
    file.write_text(text.replace(old, new), encoding='utf-8')

profile = root / 'AssessmentUi.kt'
patch(profile,
      '        item { Text("Четыре режима знакомства с собой", fontSize = 21.sp, fontWeight = FontWeight.Bold) }',
      '''        item { QuestionnaireResultsCard(assessment.answers) }
        item { Text("Четыре режима знакомства с собой", fontSize = 21.sp, fontWeight = FontWeight.Bold) }''')

compass = root / 'CompassUi.kt'
patch(compass,
      '''    preferences: ActivityPreferences,
    onSave: (Map<String, Int>) -> Unit,''',
      '''    preferences: ActivityPreferences,
    assessmentAnswers: Map<String, Int>,
    onSave: (Map<String, Int>) -> Unit,''')
patch(compass,
      '''            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll''',
      '''            Spacer(Modifier.height(14.dp))
            QuestionnaireResultsCard(assessmentAnswers)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll''')
patch(compass,
      'private fun RadarDiagram(axes: List<FeelingAxis>, scores: Map<String, Int>, color: Color)',
      'internal fun RadarDiagram(axes: List<FeelingAxis>, scores: Map<String, Int>, color: Color)')

app = root / 'AppUi.kt'
patch(app,
      '''                    preferences = assessment.preferences,
                    onSave = { values -> persist({''',
      '''                    preferences = assessment.preferences,
                    assessmentAnswers = assessment.answers,
                    onSave = { values -> persist({''')

gradle = Path('app/build.gradle.kts')
patch(gradle, 'versionCode = 27', 'versionCode = 28')
patch(gradle, 'versionName = "0.20.0"', 'versionName = "0.21.0"')
print('Tishe v0.21.0: visible auto-calculated 40-question scales in Profile and Compass')
