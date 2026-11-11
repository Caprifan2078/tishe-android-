package ru.tishe.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose regression over the entire questionnaire catalog. */
@RunWith(AndroidJUnit4::class)
class EveryAssessmentReportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyStandaloneLastQuestionOpensRealConclusion() {
        val all = AssessmentCatalog.modules
        assertTrue("Survey catalogue unexpectedly empty", all.isNotEmpty())
        val current = mutableStateOf(all.first())
        val answerState = mutableStateOf<Map<String, Int>>(emptyMap())
        compose.setContent {
            MaterialTheme {
                SurveyScreen(
                    module = current.value,
                    answers = answerState.value,
                    onAnswer = { id, score ->
                        answerState.value = answerState.value + (id to score)
                    },
                    onBack = {}
                )
            }
        }
        for (module in all) {
            val questions = module.questions
            assertTrue(module.id + ": no questions", questions.isNotEmpty())
            val earlier = questions.dropLast(1).associate {
                it.id to if (module.evidenceBased) 2 else 3
            }
            compose.runOnIdle {
                current.value = module
                answerState.value = earlier
            }
            if (module.evidenceBased) {
                compose.onNodeWithText("Мне 18 или больше, продолжить").performClick()
            }
            val last = questions.last()
            compose.onNodeWithText(last.text).assertExists()
            val choice = when {
                module.evidenceBased -> "Почти каждый день"
                module.kind == "ipip" -> "Очень на меня похоже"
                else -> "Почти постоянно"
            }
            compose.onNodeWithText(choice).performClick()
            compose.waitForIdle()
            assertTrue(module.id + ": final answer callback did not update state",
                answerState.value.containsKey(last.id))
            compose.onNodeWithText("К результатам").performClick()
            compose.onNodeWithText("Результат опроса").assertExists()
            compose.onNodeWithText("Итог теста").assertExists()
            val conclusion = DirectTestConclusionEngine.single(module, answerState.value)
            assertTrue(module.id + ": missing conclusion", conclusion.headline.isNotBlank())
            assertTrue(module.id + ": missing explanation", conclusion.explanation.isNotBlank())
            assertTrue(module.id + ": missing action", conclusion.nextStep.isNotBlank())
        }
    }

    @Test fun allFourJourneyFinalQuestionsOpenRealReport() {
        val modes = JourneyLevel.entries
        val current = mutableStateOf(modes.first())
        val cursor = mutableIntStateOf(0)
        val answerState = mutableStateOf<Map<String, Int>>(emptyMap())
        compose.setContent {
            MaterialTheme {
                JourneyScreen(
                    level = current.value,
                    answers = answerState.value,
                    savedIndex = cursor.intValue,
                    onAnswer = { id, score ->
                        answerState.value = answerState.value + (id to score)
                    },
                    onMove = { cursor.intValue = it },
                    onBack = {},
                    onCompass = {}
                )
            }
        }
        for (mode in modes) {
            val questions = AssessmentJourney.questions(mode)
            val last = questions.last()
            compose.runOnIdle {
                current.value = mode
                cursor.intValue = questions.lastIndex
                answerState.value = questions.dropLast(1).associate { it.question.id to 3 }
            }
            compose.onNodeWithText(last.question.text).assertExists()
            val choice = if (last.module.kind == "ipip") "Очень похоже" else "Почти всегда"
            compose.onNodeWithText(choice).performClick()
            compose.waitForIdle()
            assertTrue(mode.name + ": last answer not accepted",
                answerState.value.containsKey(last.question.id))
            compose.onNodeWithText("К результатам").performClick()
            compose.onNodeWithText("Твои результаты").assertExists()
            compose.onNodeWithTag("journey_result_list")
                .performScrollToNode(hasText("Итог теста"))
            compose.onNodeWithText("Итог теста").assertExists()
            val outcome = DirectTestConclusionEngine.journey(mode, answerState.value)
            assertTrue(mode.name + ": missing headline", outcome.headline.isNotBlank())
            assertTrue(mode.name + ": missing explanation", outcome.explanation.isNotBlank())
            assertTrue(mode.name + ": missing next step", outcome.nextStep.isNotBlank())
        }
    }
}
