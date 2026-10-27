package ru.tishe.app

/** Pure, deterministic policy for practice bookmarks. No private notes are stored. */
internal object PracticeFavoritesPolicy {
    const val MAX_FAVORITES = 30

    fun normalize(routes: Iterable<String>, allowed: Set<String>): List<String> =
        routes.asSequence()
            .filter { it in allowed }
            .distinct()
            .take(MAX_FAVORITES)
            .toList()

    fun toggle(current: List<String>, route: String, allowed: Set<String>): List<String> {
        val normalized = normalize(current, allowed)
        if (route !in allowed) return normalized
        return if (route in normalized) normalized.filterNot { it == route }
        else normalize(normalized + route, allowed)
    }
}
