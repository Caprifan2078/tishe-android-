from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
for filename, folder in [("DocumentTransactions.kt", root), ("DocumentTransactionsTest.kt", tests)]:
    target = folder / filename
    if target.exists():
        raise RuntimeError("Unexpected pre-existing file: " + str(target))
    shutil.copyfile(Path("scripts") / filename, target)

storage = root / "SecureStorage.kt"
s = storage.read_text(encoding="utf-8")
def replace(before, after):
    global s
    occurrences = s.count(before)
    if occurrences != 1:
        raise RuntimeError("SecureStorage mismatch: " + repr(before[:65]) + " (" + str(occurrences) + ")")
    s = s.replace(before, after)

marker = '    /** Encrypt UI restoration state before Compose places it in the Android Bundle.'
assert s.count(marker) == 1
s = s.replace(marker, '''    /**
     * All persistent changes use a deep copy of the cached encrypted document.
     * Failed crypto/commit/JSON mutation cannot modify the previous cached root.
     */
    @Synchronized
    private fun editDocument(durable: Boolean = true, mutate: (JSONObject) -> Unit) {
        DocumentTransactions.commit(root(), mutate) { candidate ->
            save(candidate, durable = durable)
        }
    }

''' + marker)
replace('save(root().put("checkIn", json))', 'editDocument { it.put("checkIn", json) }')
replace('save(root().put("favoritePractices", JSONArray().apply { valid.forEach { put(it) } }))',
        'editDocument { it.put("favoritePractices", JSONArray().apply { valid.forEach { put(it) } }) }')
replace('save(root().put("assessment", JSONObject().put("answers", answers).put("preferences", preferences).put("journeyPositions", positions)), durable = false)',
        'editDocument(durable = false) { it.put("assessment", JSONObject().put("answers", answers).put("preferences", preferences).put("journeyPositions", positions)) }')
replace('save(root().put("selfKnowledge", node))', 'editDocument { it.put("selfKnowledge", node) }')

begin = s.index('    @Synchronized\n    fun addDiary(value: DiaryEntry) {')
end = s.index('    /** Practice route IDs are added', begin)
original = s[begin:end]
expected = '''    @Synchronized
    fun addDiary(value: DiaryEntry) {
        val previous = root()
        val array = previous.optJSONArray("diary") ?: JSONArray()
        array.put(
            JSONObject()
                .put("timestamp", value.timestamp)
                .put("emotion", value.emotion.id)
                .put("intensity", value.intensity)
                .put("note", value.note.take(2000))
        )
        save(previous.put("diary", array))
    }

'''
if original != expected:
    raise RuntimeError("Unexpected addDiary implementation")
s = s[:begin] + '''    @Synchronized
    fun addDiary(value: DiaryEntry) {
        require(value.intensity in 0..10 && value.timestamp > 0)
        editDocument { document ->
            val array = document.optJSONArray("diary") ?: JSONArray()
            array.put(
                JSONObject()
                    .put("timestamp", value.timestamp)
                    .put("emotion", value.emotion.id)
                    .put("intensity", value.intensity)
                    .put("note", value.note.take(2000))
            )
            document.put("diary", array)
        }
    }

''' + s[end:]

begin = s.index('    /** Replace exactly one matching entry in the original encrypted document. */')
end = s.index('    @Synchronized\n    fun loadAssessment()', begin)
block = s[begin:end]
if block.count('val document = root()') != 1 or block.count('save(document.put("diary", array))') != 1:
    raise RuntimeError("Unexpected updateDiary structure")
block = block.replace('''        val document = root()
        val array = document.optJSONArray("diary") ?: error("Diary history unavailable")''', '''        editDocument { document ->
            val array = document.optJSONArray("diary") ?: error("Diary history unavailable")''')
block = block.replace('''        save(document.put("diary", array))
    }''', '''            document.put("diary", array)
        }
    }''')
s = s[:begin] + block + s[end:]

begin = s.index('    @Synchronized\n    fun saveCompass(')
end = s.index('    /** Kept under a new optional JSON key;', begin)
block = s[begin:end]
if block.count('val document = root()') != 1 or block.count('save(document.put("compassHistory", history))') != 1:
    raise RuntimeError("Unexpected saveCompass structure")
block = block.replace('''        val document = root()
        val existing''', '''        editDocument { document ->
            val existing''')
block = block.replace('''        save(document.put("compassHistory", history))
    }''', '''            document.put("compassHistory", history)
        }
    }''')
s = s[:begin] + block + s[end:]

assert 'save(root().put' not in s
assert 'val document = root()' not in s
assert 'val previous = root()' not in s
assert s.count('editDocument') == 8, s.count('editDocument')
storage.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
g = gradle.read_text(encoding="utf-8")
if g.count('versionCode = 26') != 1 or g.count('versionName = "0.19.0"') != 1:
    raise RuntimeError("Unexpected app version; expected 0.19.0")
if g.count('testImplementation("junit:junit:4.13.2")') != 1:
    raise RuntimeError("Unexpected JUnit dependency")
g = g.replace('versionCode = 26', 'versionCode = 27')
g = g.replace('versionName = "0.19.0"', 'versionName = "0.20.0"')
g = g.replace('testImplementation("junit:junit:4.13.2")',
              'testImplementation("junit:junit:4.13.2")\n    testImplementation("org.json:json:20240303")')
gradle.write_text(g, encoding="utf-8")
print("Tishe 0.20.0: all encrypted-document mutations are copy-on-write; failure tests added")
