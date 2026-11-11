from pathlib import Path
import shutil

root = Path("app/src/androidTest/java/ru/tishe/app")
old = root / "QuestionnaireUiSmokeTest.kt"
if not old.is_file():
    raise RuntimeError("Requires current Android questionnaire UI QA tests")
new = root / "EveryAssessmentReportUiTest.kt"
if new.exists():
    raise RuntimeError("Results catalogue regression already installed")
shutil.copyfile("scripts/EveryAssessmentReportUiTest.kt", new)
print("Added real Compose final-question -> conclusion coverage for all individual modules and four journeys")
