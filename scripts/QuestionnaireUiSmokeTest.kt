package ru.tishe.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Render real Compose screens in an Android emulator; no mock UI and no personal data. */
@RunWith(AndroidJUnit4::class)
class QuestionnaireUiSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completedFortyShowsReportAndAllowsReview() {
        val answers = AssessmentJourney.questions(JourneyLevel.START)
            .associate { it.question.id to 3 }
        compose.setContent {
            MaterialTheme {
                JourneyScreen(
                    level = JourneyLevel.START,
                    answers = answers,
                    savedIndex = 0,
                    onAnswer = { _, _ -> },
                    onMove = { },
                    onBack = { },
                    onCompass = { }
                )
            }
        }
        compose.onNodeWithText("Твои результаты").assertExists()
        compose.onNodeWithTag("journey_result_list")
            .performScrollToNode(hasText("Вернуться к ответам"))
        compose.onNodeWithText("Вернуться к ответам").performClick()
        compose.onNodeWithText("вопрос 1 из 40", substring = true).assertExists()
    }

    @Test fun reportsForAllFourLengthsRenderTheirOwnResult() {
        val answers = AssessmentJourney.questions(JourneyLevel.DEEP)
            .associate { it.question.id to 3 }
        val mode = mutableStateOf(JourneyLevel.START)
        compose.setContent {
            MaterialTheme {
                JourneyReportScreen(
                    level = mode.value,
                    answers = answers,
                    onReview = {},
                    onFinish = {},
                    onCompass = {}
                )
            }
        }
        for (level in JourneyLevel.entries) {
            compose.runOnIdle { mode.value = level }
            compose.onNodeWithTag("journey_result_list")
                .performScrollToNode(hasText(level.title, substring = true))
            compose.onNodeWithText(level.title, substring = true).assertExists()
            compose.onNodeWithTag("journey_result_list")
                .performScrollToNode(hasText("Итог теста"))
            compose.onNodeWithText("Итог теста").assertIsDisplayed()
        }
    }

    @Test fun completedStandaloneSurveyRendersOutcome() {
        val module = AssessmentCatalog.find("anger")!!
        val answers = module.questions.associate { it.id to 4 }
        compose.setContent {
            MaterialTheme {
                SurveyScreen(module, answers, onAnswer = { _, _ -> }, onBack = {})
            }
        }
        compose.onNodeWithText("Результат опроса").assertExists()
        compose.onNodeWithText("Итог теста").assertExists()
        compose.onNodeWithText("По вашим ответам", substring = true).assertExists()
    }

    @Test fun unfinishedSurveyProgressCountsAnswersNotQuestionIndex() {
        val module = AssessmentCatalog.find("anxiety")!!
        val answers = mapOf("anxiety.1" to 4)
        compose.setContent {
            MaterialTheme {
                SurveyScreen(module, answers, onAnswer = { _, _ -> }, onBack = {})
            }
        }
        compose.onNodeWithText("Отвечено: 1 из 4", substring = true).assertExists()
        compose.onNodeWithText("Результат опроса").assertDoesNotExist()
    }

    @Test fun completedPhq4StillRequiresAdultConsent() {
        val module = AssessmentCatalog.find("phq4")!!
        val answers = module.questions.associate { it.id to 1 }
        compose.setContent {
            MaterialTheme {
                SurveyScreen(module, answers, onAnswer = { _, _ -> }, onBack = {})
            }
        }
        compose.onNodeWithText("Мне 18 или больше, продолжить").assertIsDisplayed()
        compose.onNodeWithText("Результат опроса").assertDoesNotExist()
        compose.onNodeWithText("Мне 18 или больше, продолжить").performClick()
        compose.onNodeWithText("Результат опроса").assertExists()
        compose.onNodeWithText("По PHQ-4", substring = true).assertExists()
    }
}


/** End-to-end from real MainActivity through encrypted local assessment persistence. */
@RunWith(AndroidJUnit4::class)
class TisheEncryptedAssessmentNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun openShortJourney() {
        compose.waitUntil(30000L) {
            compose.onAllNodesWithTag("nav_profile").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_profile").performClick()
        compose.onNodeWithText("Твоя карта состояний").assertExists()
        compose.onNodeWithTag("profile_test_list")
            .performScrollToNode(hasText("Начальный · 40"))
        compose.onNodeWithText("Начальный · 40").performClick()
    }

    @Test fun realSavedAnswerSurvivesActivityRecreation() {
        openShortJourney()
        compose.onNodeWithText("Отвечено: 0/40", substring = true).assertExists()
        compose.onNodeWithText("Часто").performClick()
        compose.onNodeWithText("Отвечено: 1/40", substring = true).assertExists()
        compose.waitForIdle()
        // MainActivity recreation reloads the encrypted source of truth.
        compose.activityRule.scenario.recreate()
        openShortJourney()
        compose.onNodeWithText("Отвечено: 1/40", substring = true).assertExists()
        compose.onNodeWithText("Мои результаты").performClick()
        compose.onNodeWithText("Твои результаты").assertExists()
    }
}
