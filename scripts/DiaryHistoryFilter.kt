package ru.tishe.app

import java.util.Locale

/** Local-only view filter. Reads diary entries but never writes, exports, logs or caches notes. */
internal object DiaryHistoryFilter {
    fun filter(
        entries: List<DiaryEntry>,
        query: String,
        emotion: Emotion?,
        fromTimestamp: Long? = null
    ): List<DiaryEntry> {
        val normalizedQuery = query.trim().take(100).lowercase(Locale.ROOT)
        return entries.asSequence()
            .filter { emotion == null || it.emotion == emotion }
            .filter { fromTimestamp == null || it.timestamp >= fromTimestamp }
            .filter {
                normalizedQuery.isEmpty() ||
                    it.note.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    it.emotion.title.lowercase(Locale.ROOT).contains(normalizedQuery)
            }
            .sortedByDescending { it.timestamp }
            .toList()
    }
}
