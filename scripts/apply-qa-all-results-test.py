from pathlib import Path
import shutil

root = Path("app/src/androidTest/java/ru/tishe/app")
target = root / "AllResultsUiRegressionTest.kt"
if not (root / "QuestionnaireUiSmokeTest.kt").is_file():
    raise RuntimeError("Expected prior Android Compose smoke test")
if target.exists():
    raise RuntimeError("Completion UI regression already installed")
shutil.copyfile("scripts/AllResultsUiRegressionTest.kt", target)
print("Registered full Android Compose results verification for all modules and 4 journeys")
