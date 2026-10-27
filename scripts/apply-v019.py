from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
for name, folder in [("DiaryEditPolicy.kt", root), ("DiaryEditPolicyTest.kt", tests)]:
    target = folder / name
    if target.exists():
        raise RuntimeError("Unexpected existing file " + str(target))
    shutil.copyfile(Path("scripts") / name, target)

ui = root / "AppUi.kt"
code = ui.read_text(encoding="utf-8")
def replace(before, after):
    global code
    if code.count(before) != 1:
        raise RuntimeError(f"Expected exactly one AppUi.kt match for: {before[:95]}; got {code.count(before)}")
    code = code.replace(before, after)

replace(
    """                        Page.DIARY -> DiaryScreen(
                            entries = diary, modifier = bottomSafe,
                            draft = diaryDraft, onDraftChange = { diaryDraft = it }
                        ) { emotion, intensity, note ->
                            val entry = DiaryEntry(System.currentTimeMillis(), emotion, intensity, note)
                            persist({ store.addDiary(entry) }) {
                                diary = (listOf(entry) + diary).sortedByDescending { it.timestamp }
                            }
                        }""",
    """                        Page.DIARY -> DiaryScreen(
                            entries = diary, modifier = bottomSafe,
                            draft = diaryDraft, onDraftChange = { diaryDraft = it },
                            onEdit = { original, updated ->
                                val next = DiaryEditPolicy.replaceOne(diary, original, updated)
                                if (next == null) {
                                    saveError = true
                                    false
                                } else {
                                    persist({ store.updateDiary(original, updated) }) { diary = next }
                                }
                            },
                            onSave = { emotion, intensity, note ->
                                val entry = DiaryEntry(System.currentTimeMillis(), emotion, intensity, note)
                                persist({ store.addDiary(entry) }) {
                                    diary = (listOf(entry) + diary).sortedByDescending { it.timestamp }
                                }
                            }
                        )"""
)
replace(
    """    onSave: (Emotion, Int, String) -> Boolean
) {
    val emotion = draft.emotion
    val strength = draft.intensity
    val note = draft.note""",
    """    onSave: (Emotion, Int, String) -> Boolean,
    onEdit: (DiaryEntry, DiaryEntry) -> Boolean
) {
    val emotion = draft.emotion
    val strength = draft.intensity
    val note = draft.note
    // Existing entry edits are only held in current UI memory; never save plaintext in a Bundle.
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingDraft by remember { mutableStateOf<DiaryDraft?>(null) }"""
)
start=code.index("        items(filteredHistory) { entry ->")
end=code.index("\n@Composable\nprivate fun InfoScreen", start)
fragment=code[start:end]
needle="""                    Text(entry.note)
                }
            }
        }"""
if fragment.count(needle)!=1:
    raise RuntimeError("Could not identify diary history card")
fragment=fragment.replace(needle, """                    Text(entry.note)
                }
                Spacer(Modifier.height(7.dp))
                TextButton(onClick = {
                    editingEntry = entry
                    editingDraft = DiaryDraft(entry.emotion, entry.intensity.toFloat(), entry.note)
                }) { Text("Изменить запись") }
            }
        }""")
closing="""    }
}
"""
if not fragment.endswith(closing):
    raise RuntimeError("Unexpected DiaryScreen ending")
editor="""    }

    val selectedEntry = editingEntry
    val changedDraft = editingDraft
    if (selectedEntry != null && changedDraft != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null; editingDraft = null },
            title = { Text("Редактировать запись", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Исходная запись сохранится без изменений, пока ты не нажмёшь «Сохранить».",
                        color = Muted, fontSize = 12.sp
                    )
                    Text("Чувство", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Emotion.entries.forEach { candidate ->
                            FilterChip(
                                selected = candidate == changedDraft.emotion,
                                onClick = { editingDraft = changedDraft.copy(emotion = candidate) },
                                label = { Text(candidate.icon + " " + candidate.title) }
                            )
                        }
                    }
                    Text("Интенсивность: " + changedDraft.intensity.toInt() + " из 10")
                    Slider(
                        value = changedDraft.intensity,
                        onValueChange = { editingDraft = changedDraft.copy(intensity = it) },
                        valueRange = 0f..10f, steps = 9
                    )
                    OutlinedTextField(
                        value = changedDraft.note,
                        onValueChange = { if (it.length <= 2000) editingDraft = changedDraft.copy(note = it) },
                        label = { Text("Текст записи") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = DiaryEntry(
                        selectedEntry.timestamp, changedDraft.emotion,
                        changedDraft.intensity.toInt(), changedDraft.note
                    )
                    if (updated == selectedEntry || onEdit(selectedEntry, updated)) {
                        editingEntry = null
                        editingDraft = null
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null; editingDraft = null }) { Text("Отмена") }
            }
        )
    }
}
"""
fragment=fragment[:-len(closing)]+editor
code=code[:start]+fragment+code[end:]
ui.write_text(code,encoding="utf-8")

storage=root / "SecureStorage.kt"
s=storage.read_text(encoding="utf-8")
marker="    @Synchronized\n    fun loadAssessment(): AssessmentState {"
if s.count(marker)!=1:
    raise RuntimeError("Unexpected storage location")
feature="""    /** Replace exactly one matching entry in the original encrypted document. */
    @Synchronized
    fun updateDiary(original: DiaryEntry, updated: DiaryEntry) {
        require(original.timestamp == updated.timestamp)
        require(updated.intensity in 0..10 && updated.note.length <= 2000)
        val document = root()
        val array = document.optJSONArray("diary") ?: error("Diary history unavailable")
        var index = -1
        for (i in 0 until array.length()) {
            val value = array.optJSONObject(i) ?: continue
            val candidate = DiaryEntry(
                value.optLong("timestamp"),
                Emotion.fromId(value.optString("emotion")) ?: continue,
                value.optInt("intensity", 5).coerceIn(0, 10),
                value.optString("note", "")
            )
            if (candidate == original) { index = i; break }
        }
        check(index >= 0) { "Original diary entry is no longer present" }
        array.put(index, JSONObject()
            .put("timestamp", updated.timestamp)
            .put("emotion", updated.emotion.id)
            .put("intensity", updated.intensity)
            .put("note", updated.note))
        save(document.put("diary", array))
    }

"""
s=s.replace(marker,feature+marker)
storage.write_text(s,encoding="utf-8")

gradle=Path("app/build.gradle.kts")
g=gradle.read_text(encoding="utf-8")
if g.count("versionCode = 25")!=1 or g.count('versionName = "0.18.0"')!=1:
    raise RuntimeError("Base version not v0.18.0")
g=g.replace("versionCode = 25","versionCode = 26").replace('versionName = "0.18.0"','versionName = "0.19.0"')
gradle.write_text(g,encoding="utf-8")
assert "items(filteredHistory)" in code
assert 'store.updateDiary(original, updated)' in code
assert 'fun updateDiary(original: DiaryEntry' in s
print("Tishe 0.19: non-destructive diary edits, protected save failures, no Bundle plaintext")
