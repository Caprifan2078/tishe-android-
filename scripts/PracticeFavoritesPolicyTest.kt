package ru.tishe.app

import org.junit.Assert.*
import org.junit.Test

class PracticeFavoritesPolicyTest {
    private val routes = setOf("wave", "garden", "micro", "rest")

    @Test fun bookmarkIsReversible() {
        val initial = emptyList<String>()
        val saved = PracticeFavoritesPolicy.toggle(initial, "wave", routes)
        assertEquals(listOf("wave"), saved)
        assertTrue(PracticeFavoritesPolicy.toggle(saved, "wave", routes).isEmpty())
    }

    @Test fun rejectsStaleInvalidDuplicateIds() {
        assertEquals(listOf("garden", "wave"),
            PracticeFavoritesPolicy.normalize(listOf("wrong", "garden", "garden", "wave"), routes))
        assertEquals(listOf("garden"), PracticeFavoritesPolicy.toggle(listOf("garden"), "missing", routes))
    }

    @Test fun capsUntrustedInput() {
        val allowed = (1..100).map { "practice$it" }.toSet()
        val input = (1..100).map { "practice$it" }
        assertEquals(30, PracticeFavoritesPolicy.normalize(input, allowed).size)
        assertEquals(30, PracticeFavoritesPolicy.toggle(input, "practice50", allowed).size)
    }

    @Test fun survivesOrderAndRefresh() {
        val allowed = setOf("alpha", "beta", "gamma")
        assertEquals(listOf("gamma", "alpha"),
            PracticeFavoritesPolicy.normalize(listOf("gamma", "alpha", "gamma"), allowed))
        assertEquals(listOf("alpha"), PracticeFavoritesPolicy.toggle(listOf("gamma", "alpha"), "gamma", allowed))
    }
}
