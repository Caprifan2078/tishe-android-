from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
tests = Path("app/src/test/java/ru/tishe/app")
tests.mkdir(parents=True, exist_ok=True)
for filename, folder in [
    ("DiaryHistoryFilter.kt", root),
    ("DiaryHistoryFilterTest.kt", tests)
]:
    dest = folder / filename
    if dest.exists():
        raise RuntimeError("Refusing to replace existing " + str(dest))
    shutil.copyfile(Path("scripts") / filename, dest)

ui = root / "AppUi.kt"
source = ui.read_text(encoding="utf-8")
def replace(before, after):
    global source
    count = source.count(before)
    if count != 1:
        raise RuntimeError(f"Unexpected AppUi.kt context (count={count}): {before[:100]}")
    source = source.replace(before, after)

replace(
    "import androidx.compose.foundation.verticalScroll",
    "import androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.horizontalScroll"
)
replace(
    """    val note = draft.note
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {""",
    """    val note = draft.note
    // A diary query remains in process memory only, not in Android SavedState.
    var historyQuery by remember { mutableStateOf("") }
    var historyEmotionId by rememberSaveable { mutableStateOf("") }
    var recentOnly by rememberSaveable { mutableStateOf(false) }
    val historyEmotion = Emotion.fromId(historyEmotionId)
    val after = if (recentOnly) System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 else null
    val filteredHistory = remember(entries, historyQuery, historyEmotionId, recentOnly) {
        DiaryHistoryFilter.filter(entries, historyQuery, historyEmotion, after)
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {"""
)
replace(
    """        item { Text("История", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (entries.isEmpty()) item { Text("Пока здесь пусто. Первая запись — уже начало наблюдений.", color = Muted) }
        items(entries) { entry ->""",
    """        item {
            Text("История записей", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Ищи по словам и чувствам. Поиск остаётся на этом устройстве.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = historyQuery,
                onValueChange = { historyQuery = it.take(100) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск по дневнику") },
                placeholder = { Text("Например, прогулка или утро") },
                singleLine = true,
                trailingIcon = {
                    if (historyQuery.isNotEmpty()) TextButton(onClick = { historyQuery = "" }) {
                        Text("Очистить")
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = historyEmotion == null, onClick = { historyEmotionId = "" },
                    label = { Text("Все чувства") }
                )
                Emotion.entries.forEach { option ->
                    FilterChip(
                        selected = historyEmotion == option,
                        onClick = { historyEmotionId = option.id },
                        label = { Text(option.icon + " " + option.title) }
                    )
                }
            }
            FilterChip(
                selected = recentOnly, onClick = { recentOnly = !recentOnly },
                label = { Text("За последние 30 дней") }
            )
            Text("Показано: " + filteredHistory.size + " из " + entries.size,
                color = Muted, fontSize = 12.sp)
        }
        if (entries.isEmpty()) {
            item { Text("Пока здесь пусто. Первая запись — уже начало наблюдений.", color = Muted) }
        } else if (filteredHistory.isEmpty()) {
            item {
                SoftCard(highlight = true) {
                    Text("По запросу ничего не нашлось", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Все записи сохранены. Можно убрать фильтры и посмотреть всю историю.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {
                        historyQuery = ""
                        historyEmotionId = ""
                        recentOnly = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("Показать все записи") }
                }
            }
        }
        items(filteredHistory) { entry ->"""
)
ui.write_text(source, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
config = gradle.read_text(encoding="utf-8")
if config.count("versionCode = 24") != 1 or config.count('versionName = "0.17.0"') != 1:
    raise RuntimeError("Expected v0.17.0 as base")
gradle.write_text(config.replace("versionCode = 24", "versionCode = 25")
    .replace('versionName = "0.17.0"', 'versionName = "0.18.0"'), encoding="utf-8")

assert 'DiaryHistoryFilter.filter' in source
assert "items(filteredHistory)" in source
assert 'var historyQuery by remember { mutableStateOf("") }' in source
print("Tishe v0.18.0: private diary search, emotion/date filters, empty state, tests")
