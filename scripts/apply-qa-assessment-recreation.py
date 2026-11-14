from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
file = root / "AppUi.kt"
s = file.read_text(encoding="utf-8")

def once(old, new, why):
    global s
    count = s.count(old)
    if count != 1:
        raise RuntimeError(f"{why}: expected one exact anchor, got {count}")
    s = s.replace(old, new)

once(
    "            StartupState.Ready(withContext(Dispatchers.IO) {\n                StartupSnapshot(",
    "            StartupState.Ready(withContext(Dispatchers.IO) {\n"
    "                // A previous Activity may still be committing an accepted answer.\n"
    "                // Do not read stale SharedPreferences while that write is unfinished.\n"
    "                AssessmentWriteCoordinator.awaitIdle()\n"
    "                StartupSnapshot(",
    "cold-start storage barrier"
)
once(
    "    val assessmentWriter = remember(store) {\n"
    "        AssessmentAsyncWriter { next, durable -> store.saveAssessment(next, durable) }\n"
    "    }\n",
    "",
    "remove Activity-owned assessment writer"
)
once(
    "        assessmentSaving = true\n"
    "        assessmentScope.launch {\n"
    "            try {\n"
    "                assessmentWriter.save(next, durable)\n",
    "        assessmentSaving = true\n"
    "        // Register the operation NOW. Canceling this Activity later must not\n"
    "        // cancel a previously accepted disk write.\n"
    "        val pending = AssessmentWriteCoordinator.submit {\n"
    "            store.saveAssessment(next, durable)\n"
    "        }\n"
    "        assessmentScope.launch {\n"
    "            try {\n"
    "                pending.await()\n",
    "process-owned durable writer"
)
once(
    "                // Activity recreation may cancel the UI scope while an IO commit is\n"
    "                // finishing; the recreated Activity reloads from encrypted storage.\n",
    "                // The underlying coordinator continues writing; the recreated\n"
    "                // Activity awaits its result before loading any saved answers.\n",
    "updated cancellation guarantee"
)
if "assessmentWriter.save" in s or "store.saveAssessment(next, durable)" not in s:
    raise RuntimeError("Unsafe direct write remains or coordinator not installed")
file.write_text(s, encoding="utf-8")

target = root / "AssessmentWriteCoordinator.kt"
if target.exists():
    raise RuntimeError("Already installed AssessmentWriteCoordinator")
shutil.copyfile("scripts/AssessmentWriteCoordinator.kt", target)

tests = Path("app/src/androidTest/java/ru/tishe/app")
if tests.is_dir():
    dest = tests / "AssessmentWriteCoordinatorTest.kt"
    if dest.exists():
        raise RuntimeError("Coordinator tests already present")
    shutil.copyfile("scripts/AssessmentWriteCoordinatorTest.kt", dest)

print("QA: process-scoped serial writes, secure startup barrier, uncanceled fsync on Activity recreation")
