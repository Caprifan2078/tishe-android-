package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class DiaryEditPolicyTest {
    private val old = DiaryEntry(123L, Emotion.ANXIETY, 5, "Старая заметка")
    private val another = DiaryEntry(456L, Emotion.SADNESS, 2, "Не трогать")

    @Test fun editPreservesUnrelatedRecords() {
        val changed = old.copy(emotion = Emotion.STRESS, intensity = 7, note = "Исправленная запись")
        val initial = listOf(old, another)
        assertEquals(listOf(changed, another), DiaryEditPolicy.replaceOne(initial, old, changed))
        assertEquals(listOf(old, another), initial)
    }

    @Test fun exactMatchNeverOverwritesDifferentRecordWithSameTimestamp() {
        val other = old.copy(emotion = Emotion.ANGER, note = "Та же миллисекунда")
        val changed = old.copy(note = "Новая заметка")
        assertEquals(listOf(other, changed), DiaryEditPolicy.replaceOne(listOf(other, old), old, changed))
    }

    @Test fun rejectsMissingRecordAndChangedIdentity() {
        assertNull(DiaryEditPolicy.replaceOne(listOf(another), old, old.copy(note = "X")))
        assertNull(DiaryEditPolicy.replaceOne(listOf(old), old, old.copy(timestamp = 999L)))
    }

    @Test fun validatesLimitsAndKeepsHistoryUntouched() {
        assertNull(DiaryEditPolicy.replaceOne(listOf(old), old, old.copy(intensity = 11)))
        assertNull(DiaryEditPolicy.replaceOne(listOf(old), old, old.copy(note = "я".repeat(2001))))
        assertNotNull(DiaryEditPolicy.replaceOne(listOf(old), old, old.copy(note = "")))
    }
}
