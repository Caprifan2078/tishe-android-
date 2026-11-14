from pathlib import Path
import shutil

root = Path("app/src/main/java/ru/tishe/app")
app = root / "AppUi.kt"
coord = root / "AssessmentWriteCoordinator.kt"

def once(text, before, after, name):
    n = text.count(before)
    if n != 1:
        raise RuntimeError(f"{name}: expected one anchor, found {n}")
    return text.replace(before, after)

source = app.read_text(encoding="utf-8")
source = once(source,
    "import kotlinx.coroutines.CancellationException",
    "import kotlinx.coroutines.CancellationException\nimport android.content.Intent\nimport android.net.Uri\nimport android.provider.Settings",
    "Android recovery intent imports")
source = once(source,
    "    data object Failed : StartupState",
    "    data class Failed(val unconfirmedWrite: Boolean) : StartupState",
    "distinguish unconfirmed write failure")
source = once(source,
    "    var retryNumber by remember { mutableIntStateOf(0) }",
    "    val context = LocalContext.current\n    var retryNumber by remember { mutableIntStateOf(0) }",
    "safe Android settings context")
source = once(source,
    "        } catch (_: Exception) { StartupState.Failed }",
    "        } catch (cancelled: CancellationException) {\n            // Activity recreation must not turn canceled startup into a false disk error.\n            throw cancelled\n        } catch (error: Exception) {\n            StartupState.Failed(error is UnconfirmedAssessmentWriteException)\n        }",
    "startup failures and cancellation")
source = once(source,
    "            StartupState.Failed -> when (recoveryScreen) {",
    "            is StartupState.Failed -> when (recoveryScreen) {",
    "typed startup failure branch")
source = once(source,
    '                    Text("Не удалось прочитать личные записи", fontWeight = FontWeight.Bold, fontSize = 22.sp)',
    '''                    Text(
                        if (state.unconfirmedWrite) "Последняя запись не подтверждена"
                        else "Не удалось прочитать личные записи",
                        fontWeight = FontWeight.Bold, fontSize = 22.sp
                    )''',
    "explain unconfirmed write")
source = once(source,
    '                    Text("Данные не удалены. Не удаляй приложение и не очищай его данные: это может уничтожить записи. Можно повторить загрузку или воспользоваться помощью без личного хранилища.", color = Muted)',
    '''                    Text(
                        if (state.unconfirmedWrite)
                            "Последний ответ мог не сохраниться на диск. Другие записи не удалены. " +
                            "Чтобы не принять неподтверждённые данные за сохранённые, приложение остановило загрузку. " +
                            "Открой системные настройки приложения и выбери «Остановить» (или «Остановить принудительно»), " +
                            "затем запусти «Тише» снова. Если ошибка повторится, проверь свободное место. " +
                            "Не очищай данные и не удаляй приложение."
                        else
                            "Данные не удалены. Не удаляй приложение и не очищай его данные: это может уничтожить записи. " +
                            "Можно повторить загрузку или воспользоваться помощью без личного хранилища.",
                        color = Muted
                    )''',
    "non-destructive recovery guidance")
source = once(source,
    '                    Button(onClick = { retryNumber++ }, modifier = Modifier.fillMaxWidth()) { Text("Повторить загрузку") }',
    '''                    if (state.unconfirmedWrite) {
                        // Within this process SharedPreferences may hold RAM-only ciphertext
                        // after commit()=false. Blind retry would loop forever or show it as saved.
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + context.packageName)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Открыть настройки приложения") }
                    } else {
                        Button(onClick = { retryNumber++ }, modifier = Modifier.fillMaxWidth()) {
                            Text("Повторить загрузку")
                        }
                    }''',
    "avoid misleading retry button")
app.write_text(source, encoding="utf-8")

contents = coord.read_text(encoding="utf-8")
contents = once(contents,
    "internal object AssessmentWriteCoordinator {",
    '''/** An earlier commit was not confirmed; restarting the process clears RAM-only prefs. */
internal class UnconfirmedAssessmentWriteException :
    IllegalStateException("Assessment write did not finish durably")

internal object AssessmentWriteCoordinator {''',
    "typed conservative recovery signal")
contents = once(contents,
    '                    error("Assessment write was not confirmed; reload safely after process restart")',
    '                    throw UnconfirmedAssessmentWriteException()',
    "typed startup gate exception")
coord.write_text(contents, encoding="utf-8")

testdir = Path("app/src/androidTest/java/ru/tishe/app")
if testdir.exists():
    target = testdir / "AssessmentFailedWriteRecoveryUiTest.kt"
    if target.exists():
        raise RuntimeError("Recovery Android test already installed")
    shutil.copyfile("scripts/AssessmentFailedWriteRecoveryUiTest.kt", target)
print("QA recovery: typed unconfirmed-write state, safe Android settings route, no misleading retry")
