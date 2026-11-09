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
