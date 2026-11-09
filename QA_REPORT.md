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
