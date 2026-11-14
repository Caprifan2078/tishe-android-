# PROJECT STATE — «Тише»

- Обновлено: 2026-11-09. Android офлайн-приложение эмоциональной самопомощи; не диагноз.
- Репозиторий: Caprifan2078/tishe-android-. Основная ветка main оставлена без изменений.
- База: qa/v027-android-ui-smoke-20261104, SHA 857649600e862befc5f83b2ce448fb8d2d02841b, versionName 0.27.0, versionCode 34.
- Рабочая ветка: qa/phq4-rotation-20261109; QA-патч поверх v0.27, номер версии не повышен.
- Последнее известное подтверждение базы: GitHub Actions build runs 41831297284 и 41831308379 success; Android emulator run 41831349853, 7/7 UI tests success; в preview CI 130/130 JUnit.
- Реализовано: локальный дневник AES-GCM/Keystore, упражнения, тесты 40/150/300/500, результаты и Суперкомпас, навигация/восстановление экранов при Activity recreation.
- Текущая работа: PHQ-4 подтверждение 18+ не должно сбрасываться на уже открытой анкете после поворота экрана. Малый Boolean остаётся только в Android SavedState, не становится частью личного JSON.
- Проверено для QA-коммита b216c6d: Preview GitHub Actions 42538073673 SUCCESS, Regular 42538073711 SUCCESS, Android emulator 42538073966 SUCCESS; 130/130 JUnit и 8/8 UI, Lint 0 ошибок / 4 предупреждения. Физический телефон не проверен.
- P0: стабильная подпись APK, безопасное обновление без потери существующих записей — заблокировано до безопасного управления личным signing key; старую установку не удалять.
- Следующая задача: проверить workflow этой ветки, результаты UI, затем проверить сохранность ответов при нестандартном завершении процесса и TalkBack/крупные шрифты.
- Воспроизведение: выполнить GitHub CI на ветке; скрипт apply-qa-phq4-rotation.py должен запускаться ПОСЛЕ apply-v027-navigation.py; в эмуляторном workflow ПОСЛЕ apply-v027-ui-tests.py.

## Итерация: durable assessment saves (2026-11-09)
- Исходный QA-код: ef59c261d9a0480937cb175993e02f8e06a72ed3 на `qa/assessment-durable-20261109`; версия Android 0.27.0/code34, без переименования package и миграции JSON.
- Истинная проблема: `SharedPreferences.apply()` немедленно меняет RAM, но не уведомляет об ошибке записи на диск; ответы могли ошибочно считаться сохранёнными.
- Исправление: `saveAssessment` по умолчанию требует `commit()` с проверкой результата; только `onMove` (позиция вопроса) использует `durable=false`.
- Подтверждение: regular CI 42539551427 success, preview CI 42539551472 success, 130/130 JUnit, lint 0 errors / 4 warnings. Эмулятор 42539551445 success, **9/9** Android UI (новый disk smoke 1/1 PASS). Версия тестовой APK: 0.27.0/code34, `ru.tishe.app.results27`.
- Preview APK: SHA-256 `c6d3c678b62f4b7293d51c44525c95469be7622b17765d33285c9fe6c104f622`, размер 11 099 265 bytes; это QA, НЕ release.
- Доказательства: https://github.com/Caprifan2078/tishe-android-/actions/runs/42539551427, https://github.com/Caprifan2078/tishe-android-/actions/runs/42539551472, https://github.com/Caprifan2078/tishe-android-/actions/runs/42539551445.
- Следующий риск: `commit()` вместе с AES-GCM по-прежнему работает в UI-потоке; performance slow devices / ANR не измерены. Не делать слияние, пока не оценены latency и коррекция последовательных быстрых ответов. Стабильная подпись старых APK не решена.

## Подтверждённый итог 2026-11-09: хранение ответов
- Production-code QA commit `ee14d4e851148e1d6191277a3ce80fc78d65a29d`: `saveAssessment` по умолчанию durable; только cursor move остаётся async; при false/exception из save() оставляется последний подтверждённый `documentCache`, следующая durable запись вынудит новую попытку.
- QA test correction `b27ecbc81d9d1a753127159574ec06cac1ca7529`: редактор фейкового SharedPreferences возвращает сам себя после putString, гарантируя перехват commit().
- CI: regular build `42540430991` success; preview `42540430895` success; JUnit 130/130; Lint 0 errors / 4 warnings. Emulator `42540960128` success, **10/10** UI/Android tests.
- Первый emulator run `42540431052` FAILED: тестовый двойник ошибочно обходил `commit()` через `putString()`; исправлено только в тесте, новая регрессия прошла.
- QA APK `Tishe-v0.27.0-durable-and-cache-protection-QA.apk`: 11,099,265 bytes, SHA-256 `7a98b8b38094119605d9a1aeb7175ebbbe580769d1b38df7807ea6a89a14dca0`. GitHub artifact 13641006960. VersionName 0.27.0/versionCode 34, package `ru.tishe.app.results27`. APK signing cert SHA-256 `e29246dca78a43c36b70bfc9eb1920041ab4ddbf90d5a5c4952ba8722a919871`.
- В сравнении с предыдущей QA APK `v0.27 assessment durability` certificate `86de7ebdf40791ee5d06e996887b0767f9c2331a2488cfe30314144de15ba697`; сертификаты разные — нельзя обещать обновление поверх прежней установки.
- Следующая задача: замеры p95 latency и отсутствие ANR при синхронной записи на устройствах; проектирование постоянного ключа обновлений с изолированными секретами и без удаления установленных данных. Main не изменять.

## 2026-11-09 — UI latency measurement (QA)
- База сохранного P0 кода `bee4e9e68cf2943469bb2aa1b51f71e5ca10e556`, измерительная ветка `qa/assessment-latency-benchmark-20261109`. Production source не менялся.
- Android API35 emulator, Pixel 6 profile, 32 sync writes per size after 4 warmups: p95 in ms 40=23.053, 150=54.326, 300=31.493, 500=68.814. Median 7.565 / 29.243 / 8.976 / 12.267. Это timings одной прогонки, не hardware benchmark, не гарантия фактических FPS.
- Источник: GitHub Actions run 42545977930, 11/11 Android UI tests PASS (внутри Gradle), overall workflow FAILURE из-за `run-as: unknown package: ru.tishe.app` уже ПОСЛЕ тестов. Gradle UTP сохранил per-test logcat в artifact 13643612156; `qa-results/assessment-latency-first-run.json` извлечён из него.
- Исправлен CI extractor без повторного чтения приватных prefs: `scripts/extract-qa-assessment-latency.py` читает UTP logcat и создаёт JSON. Следующий CI запуск должен подтвердить всю цепочку.
- Следующая инженерная задача: вынести подтверждаемую запись (commit) из UI-потока, но сначала спроектировать строгую сериализацию действий, блокировку повторных тапов до подтверждения, стабильное восстановление экрана и ошибку сохранения. Нельзя возвращаться к `apply()` для ответов и нельзя терять несколько быстрых ответов.

### QA latency second run — 2026-11-09
- 42546915760: 11/11 Android emulator tests passed; p95 32.953, 58.466, 34.600, 63.899 ms at 40/150/300/500. Overall CI failed in postprocessing due multi-line shell command, not app. Evidence artifact 13643543769, machine-readable JSON in `qa-results/assessment-latency-second-run.json`.
- Corrected extraction invocation into one line. Await next CI. Production app code remains unchanged.

## Подтверждённая контрольная точка — latency CI 2026-11-09
- QA branch: `qa/assessment-latency-benchmark-20261109`; application production code не изменялся; база предыдущих исправлений `bee4e9e68cf2943469bb2aa1b51f71e5ca10e556`.
- GitHub Actions https://github.com/Caprifan2078/tishe-android-/actions/runs/42547438087 — **SUCCESS**, 11/11 Android emulator UI tests, 0 failures/errors/skips, бенчмарк+автоматический JSON extraction PASS. Artifact 13644246528.
- Заключительный p95 main-thread (ms): 40=94.983, 150=57.352, 300=68.246, 500=67.370. Эти цифры меняются между прогонами, но все три запуска фиксировали p95 больше 16.7 ms; UI jank вероятен. Не переводить ответы на apply() и не обходить проверку commit().
- Machine-readable: `qa-results/assessment-latency-ci-pass.json`.
- Next P1 engineering iteration: serial off-main assessment saving, stable screen during save, preserve acknowledged answers, fail/report on IO error, tests for rapid taps/rotation/cursor, measure responsiveness; keep QA, main untouched. P0 stable APK signing remains open.

## QA P1 off-main assessment writes — 2026-11-10 (GitHub CI UTC 2026-11-09)
- Рабочая ветка `qa/assessment-offmain-20261110`; production-code fix `a0ad1e4025153fa31891864b6e51db9aa8f16ec0`, основан на QA benchmark checkpoint bb0cc421653d79fbc0b5ac140c2641b50a45d94f.
- Пяти операциям с анкетами (ответ длинной анкеты, переход между вопросами, ответ отдельного опроса, первое открытие Journey и настройки) обеспечен один async UI механизм через `AssessmentAsyncWriter` с `withContext(Dispatchers.IO)`; по-прежнему сохраняет ответы с проверкой `SharedPreferences.commit()`, без перехода на `apply()`.
- UI не изменяет подтверждённые ответы/настройки до успеха записи; во время записи модальное сообщение и gate предотвращают действия над устаревшими snapshot. Новый Journey открывается только после успешного checkpoint; системный Back блокируется. Ошибка показывает старый диалог, прежние данные остаются.
- Сборки: regular https://github.com/Caprifan2078/tishe-android-/actions/runs/42723963967 PASS, preview https://github.com/Caprifan2078/tishe-android-/actions/runs/42723964171 PASS, JUnit 130/130, Android Lint 0 ошибок/4 предупреждения.
- Эмулятор Android API35 https://github.com/Caprifan2078/tishe-android-/actions/runs/42723964260 PASS, XML 13/13 tests (0 failed, 0 errors, 0 skipped): 11 существовавших + 2 новых на off-main и propagation of failed save. CI artifact 13720494652.
- QA preview APK artifact 13721079757: `Tishe-v0.27.0-offmain-safe-save-QA.apk`, 11,099,265 bytes, SHA-256 `67d68f989b35cdec31f9d3de70232a96ceb01200632c3704d9443f49e3f9704d`, Zip CRC PASS, package `ru.tishe.app.results27`, versionName 0.27.0 / code34. Новая тестовая APK не является обновлением установленного приложения; debug подписывание может не совпасть с предыдущим.
- Основной `main` не менялся, PR draft #10 в предыдущую QA ветку.
- Остаточные риски: физическое Android устройство, быстрые тап-сценарии под медленным flash, rotation/cancellation во время блокированного commit, производительность остальных синхронных сохранений (дневник, настройки компаса), стабильная подпись для апдейтов.


## QA: end-to-end results for every Android questionnaire (2026-11-11)
- Branch `qa/results-all-tests-20261111` based on `qa/assessment-offmain-20261110`. Production code unchanged; no release claim; `main` untouched.
- New Android Compose test suite `EveryAssessmentReportUiTest`: `everyStandaloneLastQuestionOpensRealConclusion` iterates all **48** SurveyModule IDs and verifies selected final answer -> user button `К результатам` -> rendered `Результат опроса`/`Итог теста` and nonempty headline/explanation/next step; adult PHQ-4 consent included. `allFourJourneyFinalQuestionsOpenRealReport` verifies 40/150/300/500 last answer -> `Твои результаты` and `Итог теста`.
- Android emulator GitHub Actions https://github.com/Caprifan2078/tishe-android-/actions/runs/43013180435 **SUCCESS**, XML artifact 13851368556 **15/15 PASS**, failures=0/errors=0/skipped=0. Previous 13 tests retained.
- First run 43012301792 failed only new test harness: first module rendered from empty answers before partial answers were injected, so `rememberSaveable` cursor remained at first question. Changed test initial state (`da52ddb...`), second run fully PASS; no production code patch required.
- HTML separately: `qa/web025-results-20261111`, commit `60c6063b...`; Web QA run 43013436451 SUCCESS. New 0.2.5 gates unconfirmed answers, gives full result after final answer; local Chromium verified 156 outcome scenarios, 48/48 module UI and 4/4 journey UI, no JS errors. This is separate storage, not synced with Android.
- Remaining P0/P1: stable Android APK signing/in-place update, real device acceptance (font sizes/TalkBack), process recreation while IO commit is blocked and actual vault backup/recovery on iPhone. Do not uninstall production installation with private data.

## QA 2026-11-15 — pending Android encrypted writes across Activity recreation
- Branch `qa/assessment-recreation-safe-20261115`, PR #14, production-code commit `31c707a0bb7926172518715c2efddbfe9535df2b`; `main` unchanged.
- Fixed accepted questionnaire write being owned by old Activity coroutine: now `AssessmentWriteCoordinator.submit` registers synchronously on main and processes saves sequentially using a process-local IO scope independent of Compose. Recreated Activity waits for all outstanding writes **before reading fresh SecureStorage**, preventing stale snapshots overwriting late-arriving answers.
- Failed commit is not treated as valid on a new instance: `awaitIdle()` refuses loading possibly unconfirmed SharedPreferences RAM; existing recovery UI used. A confirmed later explicit write resets the failure barrier.
- Standard Android GitHub Actions `43649120359`: SUCCESS. Preview `43649120309`: SUCCESS. JUnit 130/130; Android Lint 0 errors, 4 warnings.
- Instrumented Pixel 6 Android API35 `43649120280`: SUCCESS, **19/19 tests** (0 failures, 0 errors, 0 skipped), artifact `14135517510`; four new tests cover canceled waiter, sequential writes, recovery from failed write and real MainActivity recreation while IO is delayed. Previous entire 48+4 result-UI coverage still passes.
- QA APK artifact `14134839072`, `Tishe-v0.27.0-rotation-safe-QA.apk`, 11,115,649 bytes, SHA-256 `027c29dc89dbb43da324b643e66757bdb7b408281f220812b0ef2cff44626750`. Package `ru.tishe.app.results27`, version 0.27.0/code34. ZIP CRC verified.
- Release blockers remain: stable signing key/in-place upgrades with saved user data, physical devices and true low-speed flash/mid-write process death; error recovery across config change and persistent browser backup validation. No claim of store/App State equivalence between HTML and Android.

## QA recovery from failed encrypted writes — 2026-11-15
- Branch `qa/recovery-after-failed-write-20261115`, draft PR #15; code checkpoint `9227e747e5944805364e3930b31ccf82529dc342`. Main remains unchanged.
- Fixed stuck UX after `AssessmentWriteCoordinator` detects a prior unconfirmed `SharedPreferences.commit()`: startup now retains a typed `UnconfirmedAssessmentWriteException` and shows a specific recovery screen rather than an ineffective retry button.
- The screen explains the RAM-only write risk, offers Android application settings (force-stop and reopen), keeps offline quick/emergency support accessible, and warns **not to uninstall or clear app data**. Ordinary Keystore/read failures continue offering Retry. Startup cancellation on rotation is rethrown, not turned into an error state.
- CI regular `43650120958` SUCCESS, preview `43650120961` SUCCESS; 130/130 JUnit, 0 Lint errors / 4 warnings. Emulator `43650120951` SUCCESS, **20/20 Android tests**, UTP XML 0 failures/errors/skips; new real-MainActivity failure/recreation/recovery UX test passed.
- Preview APK `Tishe-v0.27.0-recovery-safe-QA.apk`, 11,115,649 bytes, SHA-256 `48bec284821ebf51cadc5694502df06d6143feb31d7bf2cdf324e0529b30161c`, package `ru.tishe.app.results27`, version 0.27.0/code34, APK and artifact CRC PASS.
- Still blocked: stable signing identity and upgrades with existing personal data, physical Android QA, real low-storage/flash fault, process-kill/fsync edge cases, Safari backups. The UI explains safe recovery but does not guarantee that an underlying failed storage commit can be repaired without user intervention.

## QA 2026-11-15 — fast answers, concise reports, correct survey/18-compass separation
- QA code commit `8f5608df2f8906cba02c3d783823c283f7f76744`, PR #17 draft. Base `qa/recovery-after-failed-write-20261115`; `main` unchanged.
- Added optional "Быстрые ответы" Switch to all 40/150/300/500 journeys and individual survey pages. After a choice, the next question/report opens **only when the confirmed parent answer is observable**. No immediate optimistic answer, no unconfirmed result, no double skip from rapid repeated taps; durable write, input barrier, encryption and cancellation safety remain unchanged. Default OFF; opt-in avoids surprising navigation when reviewing/editing.
- Corrected confusion in compass UI: after all 40 questions, show **already-populated 6-domain questionnaire radar first** on the compass screen. The 18 axes refer to self-reported feelings now and remain **unfilled until voluntarily rated**; we do NOT infer or persist those from the past-seven-day survey. Previous 6 calculated scale values are not discarded.
- Report now presents conclusion + next step first, with additional rationale, evidence, prompts and limitations behind optional "Подробнее". Repeated question-page text and duplicated disclaimer shortened.
- Standard CI `43653818847`: SUCCESS, preview `43653818950`: SUCCESS. API35 emulator `43653818900`: SUCCESS, **25/25 Android tests**, no failures/errors/skips; 5 new UI tests for rapid/double tap, last-question report, individual quick mode, 40 survey radar and compact evidence. Existing 48 + 4 result-path regression retained.
- Artifact `14136768097`: `Tishe-v027-FAST-Compass-QA.apk`, 11,115,649 bytes, SHA256 `5a98f2f7a72b1a0a9d1512090629889ea59868f6faf6eb4e42a8de6a9e12f6fa`, package `ru.tishe.app.results27` (preview), app 0.27.0. APK ZIP integrity PASS. Exact file and zip fallback exported.
- CI `43653818900` evidence artifact `14137825860`, UTP 25/25. Initial QA commit `3039f52` failed only due patch anchor on narrative whitespace; corrected in `8f5608d`; do not cite the initial failed run as passing.
- Still P0: stable signing and non-destructive migration for installed app; real physical device/low storage/rapid physical taps; Safari backup. UI tests are simulated delayed/instant persistence callbacks, not a physical stress test. Do not uninstall any installed app with personal data.
