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

## 2026-11-10 — QA гарантия результатов всех тестов
- Продолжение Android ветки `qa/results-all-tests-20261110`, база `qa/assessment-offmain-20261110`, prior HEAD `9c9a665c6f9a26ba505df0dee81d9b5839db8245`; `main` не изменён.
- Добавлены 3 Compose инструментальных проверки: все 48 законченных модулей имеют экран с прямым выводом и шагом; каждый модуль открывает результат после последнего ответа; все 40/150/300/500 длинные маршруты открывают полный результат после последнего ответа.
- GitHub Android emulator API35 CI `42727746462` **SUCCESS**, фактический `TEST-emulator-5554 - 15-_app-.xml`: **16/16 PASS**, 0 failures/errors/skipped. Прежние 13 тестов тоже сохранились. Артефакт XML/логов `13723796402`.
- Production Kotlin код и форматы хранения в этом QA-патче не менялись; не утверждать физическую приёмку или совместимость обновления APK.
- Веб: HTML 0.2.5 отдельный проектный branch `qa/web025-results-20261110`; 52 JS results проверены в GitHub CI 42728175549 (PASS), точный проверенный index.html blob `86a573822985f1d30b496b8f081d1507dc53b34f`; опубликован на gh-pages коммит b874648. Приватный ключ, JSON/локальные ответы не публиковались.
