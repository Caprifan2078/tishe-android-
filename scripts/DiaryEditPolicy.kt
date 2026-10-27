package ru.tishe.app

/** Pure conflict-safe replacement of one exact entry, leaving all other records unchanged. */
internal object DiaryEditPolicy {
    fun replaceOne(
        history: List<DiaryEntry>,
        original: DiaryEntry,
        replacement: DiaryEntry
    ): List<DiaryEntry>? {
        if (replacement.timestamp != original.timestamp ||
            replacement.intensity !in 0..10 ||
            replacement.note.length > 2000
        ) return null

        val index = history.indexOf(original)
        if (index < 0) return null
        return history.toMutableList().apply { this[index] = replacement }
    }
}
