package ru.tishe.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The fake parent intentionally withholds its acknowledged result until signaled. */
@RunWith(AndroidJUnit4::class)
class RapidAnswerAndSurveyCompassUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickJourneyWaitsForConfirmedAnswerAndNeverDoubleSkips() {
        val mode = JourneyLevel.START
        val first = AssessmentJourney.questions(mode)[0]
        val state = mutableStateOf<Map<String, Int>>(emptyMap())
        val cursor = mutableIntStateOf(0)
        val offered = mutableStateOf<Pair<String, Int>?>(null)
        compose.setContent {
            MaterialTheme {
                JourneyScreen(mode, state.value, cursor.intValue,
                    onAnswer = { id, value -> offered.value = id to value },
                    onMove = { cursor.intValue = it },
                    onBack = {}, onCompass = {})
            }
        }
        compose.onNodeWithTag("journey_quick_mode").performClick()
        compose.onNodeWithText(first.question.text).assertExists()
        compose.onNodeWithText("Часто").performClick()
        // Simulate very rapid duplicate tap while encrypted disk commit is pending.
        compose.onNodeWithText("Часто").performClick()
        compose.waitForIdle()
        assertEquals(first.question.id to 3, offered.value)
        assertEquals("Cannot advance on a RAM-only answer", 0, cursor.intValue)
        compose.runOnIdle { state.value = state.value + offered.value!! }
        compose.waitUntil(15000L) { cursor.intValue == 1 }
        compose.onNodeWithText(AssessmentJourney.questions(mode)[1].question.text).assertExists()
        assertEquals("Double tap must not skip two questions", 1, cursor.intValue)
        assertEquals(1, AssessmentJourney.progress(mode, state.value).answered)
    }

    @Test fun lastJourneyAnswerInQuickModeOpensReportWithAllForty() {
        val mode = JourneyLevel.START
        val questions = AssessmentJourney.questions(mode)
        val cursor = mutableIntStateOf(questions.lastIndex)
        val answers = mutableStateOf(questions.dropLast(1).associate { it.question.id to 3 })
        compose.setContent {
            MaterialTheme {
                JourneyScreen(mode, answers.value, cursor.intValue,
                    onAnswer = { id, value -> answers.value = answers.value + (id to value) },
                    onMove = { cursor.intValue = it },
                    onBack = {}, onCompass = {})
            }
        }
        compose.onNodeWithTag("journey_quick_mode").performClick()
        compose.onNodeWithText(questions.last().question.text).assertExists()
        val option = if (questions.last().module.kind == "ipip") "Очень похоже" else "Часто"
        compose.onNodeWithText(option).performClick()
        compose.waitUntil(15000L) {
            compose.onAllNodesWithText("Твои результаты").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(40, AssessmentJourney.progress(mode, answers.value).answered)
        compose.onNodeWithTag("journey_result_list")
            .performScrollToNode(hasText("Итог теста"))
        compose.onNodeWithText("Итог теста").assertExists()
    }

    @Test fun singleSurveyQuickModeWaitsForConfirmation() {
        val survey = AssessmentCatalog.find("anger")!!
        val state = mutableStateOf<Map<String, Int>>(emptyMap())
        val pending = mutableStateOf<Pair<String, Int>?>(null)
        compose.setContent {
            MaterialTheme {
                SurveyScreen(survey, state.value,
                    onAnswer = { id, value -> pending.value = id to value },
                    onBack = {})
            }
        }
        compose.onNodeWithTag("survey_quick_mode").performClick()
        compose.onNodeWithText("Часто").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("вопрос 1 из 4", substring = true).assertExists()
        assertEquals(0, AssessmentResultEntryPolicy.answered(survey, state.value))
        compose.runOnIdle { state.value = state.value + pending.value!! }
        compose.waitUntil(15000L) {
            compose.onAllNodesWithText("вопрос 2 из 4", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, AssessmentResultEntryPolicy.answered(survey, state.value))
    }

    @Test fun shortJourneyDisplaysCalculatedRadarWithoutFakingPresentFeelings() {
        val answers = AssessmentJourney.questions(JourneyLevel.START).associate { it.question.id to 3 }
        assertEquals(40, answers.size)
        assertEquals(6, AssessmentAnswerSummary.scales(answers).size)
        compose.setContent {
            MaterialTheme {
                SuperCompassOverview(
                    current = emptyMap(),
                    assessmentAnswers = answers,
                    history = emptyList(),
                    onStart = { _, _ -> }
                )
            }
        }
        compose.onNodeWithText("Твой профиль по 40 ответам").assertExists()
        compose.onNodeWithText("📊  Шкалы по твоим ответам").assertExists()
        compose.onNodeWithText("🧭 18 чувств прямо сейчас").assertExists()
        compose.onNodeWithText("Текущие отметки: 0 из 18.", substring = true).assertExists()
        // The assessment never invents a current-day emotion log.
        assertEquals(0, EmotionCompass.normalize(emptyMap()).size)
    }

    @Test fun detailExplanationsAreOptionalButDirectResultRemainsVisible() {
        val answers = AssessmentJourney.questions(JourneyLevel.START).associate { it.question.id to 4 }
        val narrative = QuestionnaireNarrativeEngine.forJourney(JourneyLevel.START, answers)
        val direct = DirectTestConclusionEngine.journey(JourneyLevel.START, answers)
        compose.setContent {
            MaterialTheme { QuestionnaireNarrativeCard(narrative, direct) }
        }
        compose.onNodeWithText("Итог теста").assertExists()
        compose.onNodeWithText(direct.headline).assertExists()
        compose.onNodeWithText("Что делать дальше").assertExists()
        compose.onNodeWithText("Подробнее: почему такой результат").performClick()
        compose.onNodeWithText("Скрыть объяснения").assertExists()
    }
}
