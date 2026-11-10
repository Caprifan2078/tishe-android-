package ru.tishe.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose completion screens for all questionnaires, not mock report text. */
@RunWith(AndroidJUnit4::class)
class AllResultsUiRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allIndividualCompletedTestsShowSubstantiveReports() {
        val active = mutableStateOf(AssessmentCatalog.modules.first())
        compose.setContent {
            MaterialTheme {
                val module = active.value
                val complete = module.questions.associate { it.id to if (module.evidenceBased) 3 else 4 }
                SurveyScreen(module, complete, onAnswer = { _, _ -> }, onBack = {})
            }
        }
        for (module in AssessmentCatalog.modules) {
            compose.runOnIdle { active.value = module }
            if (module.evidenceBased) {
                compose.onNodeWithText("Мне 18 или больше, продолжить").performClick()
            }
            compose.onNodeWithText("Результат опроса").assertExists()
            compose.onNodeWithText("Итог теста").assertExists()
            compose.onNodeWithText("Что делать дальше").assertExists()
        }
    }

    @Test fun everyIndividualTestOpensResultAfterLastAnswer() {
        val first = AssessmentCatalog.modules.first()
        val current = mutableStateOf(first)
        val saved = mutableStateOf(first.questions.dropLast(1).associate { it.id to 2 })
        compose.setContent {
            MaterialTheme {
                SurveyScreen(
                    module = current.value,
                    answers = saved.value,
                    onAnswer = { questionId, value -> saved.value = saved.value + (questionId to value) },
                    onBack = {}
                )
            }
        }
        for (module in AssessmentCatalog.modules) {
            compose.runOnIdle {
                current.value = module
                saved.value = module.questions.dropLast(1).associate { it.id to 2 }
            }
            if (module.evidenceBased) {
                compose.onNodeWithText("Мне 18 или больше, продолжить").performClick()
            }
            val response = when {
                module.evidenceBased -> "Почти каждый день"
                module.kind == "ipip" -> "Скорее похоже"
                else -> "Часто"
            }
            compose.onNodeWithText(response).performScrollTo().performClick()
            compose.onNodeWithText("К результатам").performClick()
            compose.onNodeWithText("Результат опроса").assertExists()
            compose.onNodeWithText("Итог теста").assertExists()
            compose.onNodeWithText("Что делать дальше").assertExists()
            assertEquals("Last answer missing: " + module.id,
                module.questions.size,
                module.questions.count { saved.value[it.id] in 0..(if (module.evidenceBased) 3 else 4) })
        }
    }

    @Test fun allFourLongModesOpenRealResultAfterLastAnswer() {
        val level = mutableStateOf(JourneyLevel.START)
        val saved = mutableStateOf(
            AssessmentJourney.questions(JourneyLevel.START).dropLast(1)
                .associate { it.question.id to 2 }
        )
        compose.setContent {
            MaterialTheme {
                val mode = level.value
                JourneyScreen(
                    level = mode,
                    answers = saved.value,
                    savedIndex = AssessmentJourney.questions(mode).lastIndex,
                    onAnswer = { questionId, value -> saved.value = saved.value + (questionId to value) },
                    onMove = {}, onBack = {}, onCompass = {}
                )
            }
        }
        for (mode in JourneyLevel.entries) {
            compose.runOnIdle {
                level.value = mode
                saved.value = AssessmentJourney.questions(mode).dropLast(1)
                    .associate { it.question.id to 2 }
            }
            compose.onNodeWithText("Часто").performScrollTo().performClick()
            compose.onNodeWithText("К результатам").performClick()
            compose.onNodeWithText("Твои результаты").assertExists()
            compose.onNodeWithTag("journey_result_list").performScrollToNode(hasText("Итог теста"))
            compose.onNodeWithText("Итог теста").assertExists()
            assertEquals(mode.count, AssessmentJourney.progress(mode, saved.value).answered)
        }
    }
}
