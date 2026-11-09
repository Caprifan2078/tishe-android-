from pathlib import Path

# QA-only correction on the v0.27 baseline. No answer, key, or JSON schema changes.
ui = Path('app/src/main/java/ru/tishe/app/AssessmentUi.kt')
source = ui.read_text(encoding='utf-8')
old = '    var acceptedAdultScreening by remember(module.id) { mutableStateOf(false) }'
new = '''    // Retain the in-session PHQ-4 age acknowledgement across Activity recreation.
    // Only this Boolean is saved; questionnaire answers stay in encrypted storage.
    var acceptedAdultScreening by rememberSaveable(module.id) { mutableStateOf(false) }'''
if source.count(old) != 1 or source.count('import androidx.compose.runtime.saveable.rememberSaveable') != 1:
    raise RuntimeError('Expected exactly one v0.27 PHQ-4 state and saveable import')
ui.write_text(source.replace(old, new), encoding='utf-8')

tests = Path('app/src/androidTest/java/ru/tishe/app/QuestionnaireUiSmokeTest.kt')
if tests.exists():
    content = tests.read_text(encoding='utf-8')
    anchor = '    @Test fun standaloneQuestionPositionSurvivesActivityRecreation() {'
    addition = '''    @Test fun phq4AgeAcknowledgementSurvivesActivityRecreation() {
        compose.waitUntil(30000L) {
            compose.onAllNodesWithTag("nav_profile").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_profile").performClick()
        compose.onNodeWithTag("profile_test_list")
            .performScrollToNode(hasText("PHQ-4: тревога и настроение"))
        compose.onNodeWithText("PHQ-4: тревога и настроение").performClick()
        compose.onNodeWithText("Мне 18 или больше, продолжить").assertIsDisplayed()
        compose.onNodeWithText("Мне 18 или больше, продолжить").performClick()
        compose.onNodeWithText("вопрос 1 из 4", substring = true).assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(30000L) {
            compose.onAllNodesWithText("вопрос 1 из 4", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Мне 18 или больше, продолжить").assertDoesNotExist()
    }

'''
    if content.count(anchor) != 1:
        raise RuntimeError('Expected existing v0.27 Android UI smoke test anchor')
    tests.write_text(content.replace(anchor, addition + anchor), encoding='utf-8')
print('Tishe QA: PHQ-4 gate retains its Boolean on Activity recreation; optional UI regression registered')
