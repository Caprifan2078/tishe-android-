from pathlib import Path

ui = Path("app/src/main/java/ru/tishe/app/AppUi.kt")
s = ui.read_text(encoding="utf-8")
def change(old, new):
    global s
    n = s.count(old)
    if n != 1:
        raise RuntimeError(f"Patch mismatch ({n}): {old[:65]}")
    s = s.replace(old, new)

change('    var quickOpen by remember { mutableStateOf(false) }', '''    var quickOpen by remember { mutableStateOf(false) }
    // The app opens directly to Home; any assessment is optional.
    var onboardingOpen by remember { mutableStateOf(false) }''')
change('BackHandler(enabled = safetyOpen || quickOpen || active != null || survey != null || journey != null || page != Page.HOME)', 'BackHandler(enabled = safetyOpen || quickOpen || onboardingOpen || active != null || survey != null || journey != null || page != Page.HOME)')
change('        if (quickOpen) { quickOpen = false; return@BackHandler }', '''        if (quickOpen) { quickOpen = false; return@BackHandler }
        if (onboardingOpen) { onboardingOpen = false; return@BackHandler }''')
change('''            } else if (check == null) {
                Onboarding(onSafety = { safetyOpen = true }, onQuick = { quickOpen = true }) { result ->
                    persist({ store.saveCheckIn(result) }) { check = result }
                }''', '''            } else if (onboardingOpen) {
                Onboarding(
                    onSafety = { safetyOpen = true },
                    onQuick = { quickOpen = true },
                    onDismiss = { onboardingOpen = false }
                ) { result ->
                    persist({ store.saveCheckIn(result) }) {
                        check = result
                        onboardingOpen = false
                        page = Page.HOME
                    }
                }''')
change('''                            check!!, assessment, diary, compassHistory.firstOrNull(), bottomSafe,
                            onProfile = { page = Page.PROFILE },''', '''                            check ?: CheckIn(emptySet(), 2, 2, SupportFormat.VISUAL),
                            check != null, assessment, diary, compassHistory.firstOrNull(), bottomSafe,
                            onProfile = { page = Page.PROFILE },
                            onPersonalize = { onboardingOpen = true },
                            onPractices = { page = Page.PRACTICES },
                            onDiary = { page = Page.DIARY },''')
change('onRestart = { check = null; page = Page.HOME }', 'onRestart = { onboardingOpen = true }')
change('Triple(Page.PROFILE, R.drawable.ic_nav_profile, "Профиль")', 'Triple(Page.PROFILE, R.drawable.ic_nav_profile, "Тесты")')
change('private fun Onboarding(onSafety: () -> Unit, onQuick: () -> Unit, onFinish: (CheckIn) -> Unit)', 'private fun Onboarding(onSafety: () -> Unit, onQuick: () -> Unit, onDismiss: () -> Unit, onFinish: (CheckIn) -> Unit)')
change('''        Text("✦  Т И Ш Е", fontWeight = FontWeight.Bold, color = Mint, letterSpacing = 3.sp)
        TextButton(onClick = onSafety) { Text("Нужна срочная поддержка? Без опроса") }
        TextButton(onClick = onQuick) { Text("Мне тяжело — короткая помощь без анкеты") }''', '''        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("✦  Т И Ш Е", fontWeight = FontWeight.Bold, color = Mint, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Пропустить ✕") }
        }
        Text("Эта настройка необязательна. Все разделы доступны без анкеты.", color = Muted, fontSize = 13.sp)
        TextButton(onClick = onSafety) { Text("Нужна срочная поддержка?") }
        TextButton(onClick = onQuick) { Text("Быстрая помощь без анкеты") }''')
change('Text("Знакомство • ${step + 1} из 2", color = Muted)', 'Text("Настройка по желанию • ${step + 1} из 2", color = Muted)')
change('Text(if (step == 0) "Продолжить" else "Открыть Тише", fontWeight = FontWeight.Bold)', 'Text(if (step == 0) "Продолжить" else "Сохранить предпочтения", fontWeight = FontWeight.Bold)')
change('Text("Пройти знакомство заново")', 'Text("Настроить рекомендации (по желанию)")')
change('''private fun HomeScreen(
    check: CheckIn,
    assessment: AssessmentState,''', '''private fun HomeScreen(
    check: CheckIn,
    personalized: Boolean,
    assessment: AssessmentState,''')
change('''    onProfile: () -> Unit,
    onCompass: () -> Unit,''', '''    onProfile: () -> Unit,
    onPersonalize: () -> Unit,
    onPractices: () -> Unit,
    onDiary: () -> Unit,
    onCompass: () -> Unit,''')
start=s.index('        item { SectionHeading("✦  Т И Ш Е", "Ты не один со своими чувствами"')
end=s.index('        if (compass != null) {', start)
s=s[:start]+'''        item {
            SectionHeading("✦  Т И Ш Е", "Здесь можно быть собой",
                "Никаких обязательных тестов. Выбери, что поможет тебе прямо сейчас.")
        }
        item { CalmHomeHero(onCompass) }
        item {
            Button(onClick = onQuick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Мне сейчас тяжело — быстрая помощь", fontWeight = FontWeight.SemiBold)
            }
        }
        if (!personalized) {
            item {
                SoftCard(highlight = true) {
                    Text("Добро пожаловать!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Можно начать с упражнений, дневника или карты эмоций. Настройка рекомендаций — только по желанию.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onPersonalize, modifier = Modifier.fillMaxWidth()) {
                        Text("Настроить рекомендации · необязательно")
                    }
                }
            }
        }
        item {
            Text("С чего начнём?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalmShortcut("🌿", "Упражнения", "В своём темпе", onPractices, Modifier.weight(1f))
                CalmShortcut("🧭", "Карта чувств", "Понять себя", onCompass, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalmShortcut("📖", "Дневник", "Записать мысли", onDiary, Modifier.weight(1f))
                CalmShortcut("✨", "Школа эмоций", "Учиться бережно", onDiscovery, Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(onClick = onSafety, modifier = Modifier.fillMaxWidth()) {
                Text("Нужна срочная поддержка? Открыть без тестов")
            }
        }
''' + s[end:]
change('Text("РЕКОМЕНДУЕМ СЕГОДНЯ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Mint)', 'Text(if (personalized) "ПОДОБРАНО ДЛЯ ТЕБЯ" else "ПОПРОБУЙ ДЛЯ НАЧАЛА", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Mint)')
change('"Другие занятия, опросы и идеи ↓"', '"Посмотреть больше идей и занятий ↓"')
for old, new in [
    ("0xFF0F1925", "0xFF0B1523"), ("0xFF1C2B3A", "0xFF17263A"),
    ("0xFF2C4052", "0xFF2C425C"), ("0xFFBAC9FF", "0xFFCCD5FF"),
    ("0xFF9BE8D4", "0xFFA8EAD6"), ("0xFFBDCBD8", "0xFFB4C4D5")
]:
    change(old, new)
# Avoid Compose Lint ProduceStateDoesNotAssignValue false negative on value = try.
change('''        value = try {
            StartupState.Ready(withContext(Dispatchers.IO) {''', '''        val resolvedStartup: StartupState = try {
            StartupState.Ready(withContext(Dispatchers.IO) {''')
change('''        } catch (_: Exception) { StartupState.Failed }
    }
    MaterialTheme''', '''        } catch (_: Exception) { StartupState.Failed }
        value = resolvedStartup
    }
    MaterialTheme''')
ui.write_text(s, encoding="utf-8")

design=Path("app/src/main/java/ru/tishe/app/CalmDesign.kt")
d=design.read_text(encoding="utf-8")
for old,new in [
    ("194f * fontScale", "182f * fontScale"),
    ('"ТВОЯ МИНУТА ТИШИНЫ"', '"ТВОЁ ПРОСТРАНСТВО"'),
    ('"Можно чувствовать разное"', '"Любые чувства имеют место"'),
    ('"Заметь, что происходит внутри — без оценок."', '"Посмотри на эмоции без оценок и спешки."'),
    ('"Открыть карту эмоций  →"', '"Исследовать чувства  →"'),
    ("fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp", "fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp"),
    ("heightIn(min = 116.dp)", "heightIn(min = 122.dp)"),
    ('Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)', 'Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)'),
]:
    if d.count(old)!=1: raise RuntimeError("Design patch mismatch: " + old)
    d=d.replace(old,new)
design.write_text(d,encoding="utf-8")
gradle=Path("app/build.gradle.kts")
g=gradle.read_text(encoding="utf-8")
if "versionCode = 18" not in g: raise RuntimeError("Unexpected versionCode")
gradle.write_text(g.replace("versionCode = 18","versionCode = 21").replace('versionName = "0.14.4"', 'versionName = "0.15.2"'),encoding="utf-8")
assert "else if (check == null)" not in s
assert "else if (onboardingOpen)" in s
assert "check ?: CheckIn(emptySet(), 2, 2, SupportFormat.VISUAL)" in s
style_file = Path("app/src/main/res/values/styles.xml")
style_text = style_file.read_text(encoding="utf-8")
obsolete = '<item name="android:windowLightNavigationBar">false</item>'
if obsolete not in style_text: raise RuntimeError("Unexpected theme XML")
style_file.write_text(style_text.replace(obsolete, ""), encoding="utf-8")
print("Tishe v0.15.2: opt-in onboarding, refreshed UI, Android 8 theme fix")
