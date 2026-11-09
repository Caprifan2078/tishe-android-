from pathlib import Path
import shutil

root = Path("app/src/androidTest/java/ru/tishe/app")
source = Path("scripts/AssessmentSaveLatencyTest.kt")
target = root / source.name
if not root.is_dir() or not (root / "QuestionnaireUiSmokeTest.kt").exists():
    raise RuntimeError("Requires existing v0.27 Android UI QA source")
if target.exists():
    raise RuntimeError("QA benchmark already installed; refusing double apply")
if not source.is_file() or source.stat().st_size < 3000:
    raise RuntimeError("Missing latency instrumentation source")
shutil.copyfile(source, target)
print("Installed QA-only on-main-thread write latency benchmark; no production code changed")
