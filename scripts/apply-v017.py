from pathlib import Path
import shutil

src = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
for filename, folder in [
    ("PracticeFavoritesPolicy.kt", src),
    ("PracticeFavoritesPolicyTest.kt", tests),
]:
    target = folder / filename
    if target.exists():
        raise RuntimeError("Refusing to overwrite existing " + str(target))
    shutil.copyfile(Path("scripts") / filename, target)

ui = src / "AppUi.kt"
body = ui.read_text(encoding="utf-8")
def replace_ui(before, after):
    global body
    if body.count(before) != 1:
        raise RuntimeError("Unexpected AppUi.kt context: " + before[:80])
    body = body.replace(before, after)

replace_ui(
    "val compassHistory: List<FeelingSnapshot>, val knowledge: SelfKnowledgeState",
    "val compassHistory: List<FeelingSnapshot>, val knowledge: SelfKnowledgeState,\n    val favoritePractices: List<String>"
)
replace_ui(
    "store.loadCompassHistory(), store.loadSelfKnowledge()",
    "store.loadCompassHistory(), store.loadSelfKnowledge(), store.loadPracticeFavorites()"
)
replace_ui(
    "    var knowledge by remember(initial) { mutableStateOf(initial.knowledge) }",
    "    var knowledge by remember(initial) { mutableStateOf(initial.knowledge) }\n"
    "    var favoritePractices by remember(initial) { mutableStateOf(initial.favoritePractices) }"
)
replace_ui(
    "Page.PRACTICES -> PracticeSearchScreen(bottomSafe) { id, emotion ->",
    """Page.PRACTICES -> PracticeSearchScreen(
                            modifier = bottomSafe,
                            favorites = favoritePractices,
                            onToggleFavorite = { route ->
                                val allowedRoutes = PracticeIndex.all.map { it.route }.toSet()
                                val next = PracticeFavoritesPolicy.toggle(favoritePractices, route, allowedRoutes)
                                if (next != favoritePractices) {
                                    persist({ store.savePracticeFavorites(next) }) { favoritePractices = next }
                                }
                            }
                        ) { id, emotion ->"""
)
replace_ui(
    "knowledge = SelfKnowledgeState(); page = Page.HOME",
    "knowledge = SelfKnowledgeState(); favoritePractices = emptyList(); page = Page.HOME"
)
ui.write_text(body, encoding="utf-8")

storage = src / "SecureStorage.kt"
code = storage.read_text(encoding="utf-8")
insert_at = "    @Synchronized\n    fun loadAssessment(): AssessmentState {"
if code.count(insert_at) != 1:
    raise RuntimeError("Unexpected SecureStorage placement")
feature = """    /** Practice route IDs are added to the existing encrypted document; no new files or permissions. */
    @Synchronized
    fun loadPracticeFavorites(): List<String> {
        val items = root().optJSONArray("favoritePractices") ?: return emptyList()
        val saved = buildList {
            for (i in 0 until items.length()) {
                val route = items.optString(i)
                if (route.isNotBlank()) add(route)
            }
        }
        return PracticeFavoritesPolicy.normalize(saved, PracticeIndex.all.map { it.route }.toSet())
    }

    @Synchronized
    fun savePracticeFavorites(routes: List<String>) {
        val valid = PracticeFavoritesPolicy.normalize(routes, PracticeIndex.all.map { it.route }.toSet())
        save(root().put("favoritePractices", JSONArray().apply { valid.forEach { put(it) } }))
    }

"""
code = code.replace(insert_at, feature + insert_at)
storage.write_text(code, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
text = gradle.read_text(encoding="utf-8")
if text.count("versionCode = 23") != 1 or text.count('versionName = "0.16.0"') != 1:
    raise RuntimeError("Base version is not 0.16.0")
text = text.replace("versionCode = 23", "versionCode = 24")
text = text.replace('versionName = "0.16.0"', 'versionName = "0.17.0"')
gradle.write_text(text, encoding="utf-8")

assert "favoritePractices = emptyList()" in body
assert "savePracticeFavorites" in body
assert "loadPracticeFavorites" in code
print("Tishe 0.17: encrypted offline practice favorites, safe migration and regression tests")
