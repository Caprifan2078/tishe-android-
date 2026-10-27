package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class PracticeSearchTest {
    @Test fun allOriginalPracticesRemainReachable() {
        assertEquals(30, PracticeIndex.all.size)
        assertEquals(30, PracticeIndex.all.map { it.route }.distinct().size)
        assertTrue(Emotion.entries.all { e -> PracticeIndex.all.any { it.route == e.id } })
        assertTrue(EmotionSkillGames.menu.all { e -> PracticeIndex.all.any { it.route == e.first } })
        assertTrue(GentlePractices.menu.all { e -> PracticeIndex.all.any { it.route == e.first } })
    }

    @Test fun searchAndFiltersStayConsistent() {
        assertEquals(30, PracticeIndex.search(" ", null, false).size)
        assertTrue(PracticeIndex.search("ГНЕВ", null, false).any { it.emotion == Emotion.ANGER })
        assertTrue(PracticeIndex.search("границы", null, false).any { it.route == "phrase" })
        assertTrue(PracticeIndex.search("", Emotion.SADNESS, false).all { it.emotion == Emotion.SADNESS })
        assertTrue(PracticeIndex.search("", null, true).all { it.short })
        assertTrue(PracticeIndex.search("xyxyxyzznotfound", null, false).isEmpty())
    }
}
