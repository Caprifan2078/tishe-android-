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
