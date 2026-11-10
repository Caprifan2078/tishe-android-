# QA REPORT — «Тише»

## Базовые доказательства v0.27 (2026-11-04)
- Preview workflow 41831297284: success, 130 JUnit tests / 0 failures (архив диагностики предыдущего аудита).
- Regular workflow 41831308379: success.
- Android emulator workflow 41831349853: success, 7 instrumented tests.
- Эти результаты относятся к базовой v0.27, а НЕ новому PHQ-4 патчу.

## QA patch (2026-11-09)
- Статическая проверка: сравнение точного выражения remember(module.id), обязательного импорта rememberSaveable и Android test anchor, fail-fast при изменённой базе.
- Добавленная регрессия: phq4AgeAcknowledgementSurvivesActivityRecreation — открытие PHQ-4, подтверждение 18+, вопрос 1, Activity recreation, отсутствие повторного возрастного экрана.
- GitHub Preview CI: PASS, 42538073673, versionName 0.27.0 / versionCode 34, package ru.tishe.app.results27, 130 JUnit / 0 failures / 0 ignored, Android Lint 0 errors / 4 warnings.
- GitHub Regular CI: PASS, 42538073711, app ru.tishe.app, compiled successfully. Signing identity is still not proven compatible with installed versions.
- GitHub emulator UI CI: PASS, 42538073966, Android API35 emulator, 8/8 instrumented cases, failures=0 errors=0 skipped=0. Evidence: app/build/outputs/androidTest-results/connected/debug/TEST-emulator-5554 - 15-_app-.xml from artifact 13640256555.
- New test phq4AgeAcknowledgementSurvivesActivityRecreation is present in the XML result, PASS. Baseline previously had 7 tests.
- QA Preview APK artifact 13640646058: SHA-256 bfa152e65dcbd44f08a8869900b778e4cabc28dc2dc3431a2f45ef56ade9e4ce, APK zip CRC PASS (AndroidManifest.xml and classes.dex present).
- All run results apply to commit b216c6d201bd82e4663563947a1efd0c49e4bd69, not the later documentation-only checkpoint.
- Физическое устройство, безопасный update, process death после force-stop, TalkBack: НЕ ПРОВЕРЕНО.

## Durable answer save — результаты 2026-11-09
- Код: `ef59c261d9a0480937cb175993e02f8e06a72ed3`, branch `qa/assessment-durable-20261109`, parent verified QA PHQ-4 61cfd824.
- Android regular: https://github.com/Caprifan2078/tishe-android-/actions/runs/42539551427 — SUCCESS, assembleDebug + testDebugUnitTest + lintDebug.
- Android preview: https://github.com/Caprifan2078/tishe-android-/actions/runs/42539551472 — SUCCESS, versionName 0.27.0, versionCode 34, `ru.tishe.app.results27`.
- Preview diagnostics artifact 13641275250: HTML JUnit report **130 tests, 0 failures, 0 ignored**; lint 0 errors, 4 warnings.
- Emulator: https://github.com/Caprifan2078/tishe-android-/actions/runs/42539551445 — SUCCESS, Android API35, 9 tests, failures 0, errors 0, skipped 0. Report artifact 13640842753, path `app/build/outputs/androidTest-results/connected/debug/TEST-emulator-5554 - 15-_app-.xml`.
- New test `AssessmentDurableDiskSmokeTest.encryptedAnswerIsOnDiskWhenSaveReturns`: PASS. Asserts encrypted XML disk value matches SharedPreferences immediately after `saveAssessment` returns, then loads the new answer via a fresh SecureStorage instance. Restores original isolated QA assessment in finally.
- APK artifact 13640389745, extracted app-debug.apk **11 099 265 bytes**, SHA-256 `c6d3c678b62f4b7293d51c44525c95469be7622b17765d33285c9fe6c104f622`; ZIP CRC PASS, AndroidManifest.xml and 4 DEX entries present.
- Proof limits: no abrupt power loss/forced kill failure injection, no real device perf/ANR test, no failure of SharedPreferences.commit simulated through real flash storage, no release signing or in-place update.

## Итерация 2 — failed commit cache isolation (2026-11-09)
- Production code `ee14d4e...`: в блоке catch после неудачного `commit()` прежний root/cache остаётся в памяти; `pendingAsyncWrite=true` заставляет следующий durable save перепроверить запись.
- CI regular 42540430991: **SUCCESS**. CI preview 42540430895: **SUCCESS**; JUnit **130 tests, 0 failures, 0 ignored**, Lint **0 errors / 4 warnings**.
- Android emulator первая попытка 42540431052: **FAILED 1/10** из-за дефекта нового тестового двойника: editor.putString возвращал реальный editor и обходил симуляцию commit false. Изменена только тестовая имитация и CI path filter, commit `b27ecbc...`; Production APK не менялась.
- Android emulator повторная попытка 42540960128: **SUCCESS 10 tests / 0 failures / 0 errors / 0 skipped**. XML artifact 13641521230, `app/build/outputs/androidTest-results/connected/debug/TEST-emulator-5554 - 15-_app-.xml`. Особенно `encryptedAnswerIsOnDiskWhenSaveReturns` PASS, `unconfirmedAnswerIsNotAdoptedAfterCommitFailure` PASS; старые 8 тестов PASS.
- QA APK GitHub artifact 13641006960: `app-debug.apk` 11,099,265 bytes, SHA-256 `7a98b8b38094119605d9a1aeb7175ebbbe580769d1b38df7807ea6a89a14dca0`, ZIP CRC PASS, AndroidManifest + 4 DEX entries. applicationId `ru.tishe.app.results27`, version 0.27.0/code34. APK signing cert SHA-256 `e29246dca78a43c36b70bfc9eb1920041ab4ddbf90d5a5c4952ba8722a919871`.
- Ограничения: ошибка коммита симулирована тестовым двойником, не реальным отказом флеш-памяти; физические устройства, power-loss, UI-latency при массовом заполнении анкет и безопасное обновление старой APK НЕ ПРОВЕРЕНЫ.

## Android performance experiment — 2026-11-09
- Source code for measurement-only QA: commit `6aafcfe5a6f58cc5acdfa652a13786cef3ac66f1` (new `AssessmentSaveLatencyTest` on isolated QA prefs; production unaffected).
- GitHub Actions run https://github.com/Caprifan2078/tishe-android-/actions/runs/42545977930: Gradle `connectedDebugAndroidTest` SUCCESS, Android API35 emulator 11 tests / 0 failures, instrumentation includes 128 measured commits and warmups.
- **Overall workflow FAILED only after successful Gradle test:** `adb shell run-as ru.tishe.app` -> unknown package; Gradle uninstalled target. Original `qa-assessment-latency.json` inside app not accessible after task. Logcat captured all 4 lines. Artifact 13643612156.
- Extracted values (milliseconds, 32 samples each, with 4 warmups):
  - 40: median 7.565; p95 23.053; max 23.668.
  - 150: median 29.243; p95 54.326; max 80.443.
  - 300: median 8.976; p95 31.493; max 62.041.
  - 500: median 12.267; p95 68.814; max 75.029.
- These are emulator-specific main-thread crypto + JSON + commit timings, not ANR frequency and not real device FPS. 150 being slower than 300 is evidence of run variability; do not infer monotonic growth.
- Resolved extraction strategy: parse UTP per-test logcat on CI runner, validate expected sizes and sample count, emit JSON independent of installed package. Check the next workflow before marking pipeline PASS.

## Independent latency repeat — 2026-11-09
- Second emulator run 42546915760: **11/11 Android tests PASS** per XML report (0 failures, 0 errors, 0 skipped), benchmark test duration 3.101 s. Overall workflow again FAILED **only after successful Gradle**: `reactivecircus/android-emulator-runner` executes each script line separately, so line continuation `\` passed no args to Python extractor.
- Full UTP logcat artifact 13643543769 was downloaded; extractor ran locally SUCCESS. p95(ms): 40=32.953, 150=58.466, 300=34.600, 500=63.899; median(ms): 11.570/10.649/9.115/22.917. Source `qa-results/assessment-latency-second-run.json`.
- Corrected workflow to invoke `python3 scripts/extract-qa-assessment-latency.py <results-dir> <output>` on ONE script line. No production source changes. Re-run required to confirm whole CI goes green.

## Full CI latency pipeline verified — 2026-11-09
- Run https://github.com/Caprifan2078/tishe-android-/actions/runs/42547438087 — **SUCCESS**, Android emulator API35, 11/11 UI/instrumentation tests, no failures/errors/skips. JSON extraction PASS from UTP logcat after connectedDebugAndroidTest, artifact 13644246528.
- 32 warm (4 excluded) / observed (32 included) writes per questionnaire length 40,150,300,500. Third-run p95 (ms) 94.983 / 57.352 / 68.246 / 67.370; median (ms) 27.869 / 11.930 / 25.287 / 25.307.
- Three p95 results for 500: first 68.814, second 63.899, third 67.370 ms. Note emulator variability, cannot infer physical-device FPS or ANR.
- Final result file `qa-results/assessment-latency-ci-pass.json`, artifact 13644246528. No product APK was built or version changed in this test-only iteration.

## QA P1 off-main saving — 2026-11-10
- Changes in `scripts/apply-qa-assessment-offmain.py`, `scripts/AssessmentAsyncWriter.kt`, `scripts/AssessmentAsyncWriterTest.kt`, code SHA `a0ad1e4025153fa31891864b6e51db9aa8f16ec0`. User JSON/Keystore alias/package/version unchanged; previous P0 durability fix retained.
- Initial validation run 42723714076/13769/13676 failed at patch guard (expected 3 calls; four actual including initial journey checkpoint); corrected.
- Second set 42723818519/18806/18954 failed at Kotlin compile due original trailing lambda argument position; corrected AssessmentAsyncWriter constructor (first dispatcher with default, last writer callback). No security/data-format changes.
- Latest regular CI: https://github.com/Caprifan2078/tishe-android-/actions/runs/42723963967 **SUCCESS**.
- Latest preview CI: https://github.com/Caprifan2078/tishe-android-/actions/runs/42723964171 **SUCCESS**. From artifact 13721259533, `app/build/reports/tests/testDebugUnitTest/index.html`: **130 tests 0 failures 0 ignored**; `lint-results-debug.txt`: **0 errors, 4 warnings**.
- Latest emulator CI: https://github.com/Caprifan2078/tishe-android-/actions/runs/42723964260 **SUCCESS**. Downloaded artifact 13720494652, parsed Android JUnit XML `app/build/outputs/androidTest-results/connected/debug/TEST-emulator-5554 - 15-_app-.xml`: **13 tests, 0 failures, 0 errors, 0 skipped**.
- NEW `AssessmentAsyncWriterTest.slowEncryptedWriteDoesNotBlockAndroidMainAndAcknowledgesOnlyAfterCompletion`: PASS (simulates blocked slow backend, Android main callback still runs, no premature acknowledgement). NEW `failedWritePropagatesAndNeverReportsSuccess`: PASS.
- Original 11 test cases (including disk ciphertext, failed-commit cache, PHQ-4 recreation, navigation state and benchmark) all PASS.
- Preview APK `ru.tishe.app.results27` version 0.27.0/code34, artifact 13721079757, extracted APK 11,099,265 bytes SHA-256 `67d68f989b35cdec31f9d3de70232a96ceb01200632c3704d9443f49e3f9704d`, CRC PASS, manifest present, 4 DEX present.
- Limits: test's blocked backend is synthetic; actual UI E2E with slow SharedPreferences and hardware p95 not yet tested. Existing latency benchmark still measures direct synchronous SecureStorage from a test thread deliberately placed on main, not the reworked UI; do not use its metrics as post-fix UI latency.

## Full questionnaire result coverage — 2026-11-10
- GitHub Actions: https://github.com/Caprifan2078/tishe-android-/actions/runs/42727746462 **SUCCESS**, branch `qa/results-all-tests-20261110`, test-only commit `cd318232c728ecba5e7c15b7e6a2a22dfbd72fe3`.
- XML artifact ID **13723796402**: `app/build/outputs/androidTest-results/connected/debug/TEST-emulator-5554 - 15-_app-.xml`, **tests 16, failures 0, errors 0, skipped 0**.
- Added `AllResultsUiRegressionTest`: (1) `allIndividualCompletedTestsShowSubstantiveReports` — iterates all 48 modules, verifies report title, direct conclusion and next step; (2) `everyIndividualTestOpensResultAfterLastAnswer` — 48 last-answer clicks and report transitions; (3) `allFourLongModesOpenRealResultAfterLastAnswer` — 40/150/300/500 last-answer navigation and complete result.
- PHQ-4 adult confirmation preserved; 13 earlier Android instrumentation tests PASS. This supplements existing **130/130 JUnit** from 42723964171; no new production APK in this test-only branch.
- Limitations: automated Compose emulator, not physical Android device; flow tests synchronously inject answer map, while existing E2E smoke tests cover actual encrypted app and off-main IO. Not a claim of clinical validity or Android release readiness.
- Web HTML 0.2.5: 48 modules + 4 long reports verified in Node.js VM; completed-save guard and test rollback on synthetic crypto error; `qa/web025-results-20261110` GitHub CI **42728175549 SUCCESS**; publish commit b874648 (gh-pages), same Git blob SHA as local HTML.
