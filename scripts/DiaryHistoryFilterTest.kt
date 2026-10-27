package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class DiaryHistoryFilterTest {
    private val entries = listOf(
        DiaryEntry(100L, Emotion.ANXIETY, 4, "Сложное УТРО в автобусе"),
        DiaryEntry(300L, Emotion.SADNESS, 6, "Мне помогла прогулка"),
        DiaryEntry(200L, Emotion.ANXIETY, 8, "Пауза и дыхание")
    )

    @Test fun matchesRussianCaseInsensitiveAndTrimsWhitespace() {
        assertEquals(listOf(100L), DiaryHistoryFilter.filter(entries, "  уТрО ", null).map { it.timestamp })
        assertEquals(listOf(300L), DiaryHistoryFilter.filter(entries, "ПРОГУЛКА", null).map { it.timestamp })
        assertEquals(listOf(200L, 100L), DiaryHistoryFilter.filter(entries, "ТРЕВОГА", null).map { it.timestamp })
    }

    @Test fun combinesEmotionAndTimeAndQuery() {
        assertEquals(listOf(200L), DiaryHistoryFilter.filter(entries, "", Emotion.ANXIETY, 150L).map { it.timestamp })
        assertTrue(DiaryHistoryFilter.filter(entries, "прогулка", Emotion.ANXIETY).isEmpty())
        assertEquals(listOf(300L), DiaryHistoryFilter.filter(entries, "", null, 250L).map { it.timestamp })
    }

    @Test fun preservesAllHistoryWhenFiltersCleared() {
        val result = DiaryHistoryFilter.filter(entries, "", null)
        assertEquals(listOf(300L, 200L, 100L), result.map { it.timestamp })
        assertEquals(3, entries.size)
    }

    @Test fun unmatchedSearchNeverDeletesEntries() {
        assertTrue(DiaryHistoryFilter.filter(entries, "неттакоготекста", null).isEmpty())
        assertEquals(3, DiaryHistoryFilter.filter(entries, "", null).size)
    }
}
