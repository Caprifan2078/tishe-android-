from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
for filename in ["PracticeIndex.kt", "PracticeSearchScreen.kt"]:
    dest = root / filename
    if dest.exists():
        raise RuntimeError("Refusing to replace existing source file " + filename)
    shutil.copyfile(Path("scripts") / filename, dest)

test_path = Path("app/src/test/java/ru/tishe/app/PracticeSearchTest.kt")
test_path.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile("scripts/PracticeSearchTest.kt", test_path)

ui = root / "AppUi.kt"
code = ui.read_text(encoding="utf-8")
old = "Page.PRACTICES -> PracticesScreen(bottomSafe)"
assert code.count(old) == 1, "Unexpected practice routing"
code = code.replace(old, "Page.PRACTICES -> PracticeSearchScreen(bottomSafe)")
ui.write_text(code, encoding="utf-8")

gradle = Path("app/build.gradle.kts")
text = gradle.read_text(encoding="utf-8")
assert "versionCode = 22" in text and 'versionName = "0.15.3"' in text, "Unexpected base version"
text = text.replace("versionCode = 22", "versionCode = 23")
text = text.replace('versionName = "0.15.3"', 'versionName = "0.16.0"')
gradle.write_text(text, encoding="utf-8")
print("Tishe 0.16: searchable practice library ready")
