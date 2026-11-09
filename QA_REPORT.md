# QA REPORT — «Тише»

## Базовые доказательства v0.27 (2026-11-04)
- Preview workflow 41831297284: success, 130 JUnit tests / 0 failures (архив диагностики предыдущего аудита).
- Regular workflow 41831308379: success.
- Android emulator workflow 41831349853: success, 7 instrumented tests.
- Эти результаты относятся к базовой v0.27, а НЕ новому PHQ-4 патчу.

## QA patch (2026-11-09)
- Статическая проверка: сравнение точного выражения remember(module.id), обязательного импорта rememberSaveable и Android test anchor, fail-fast при изменённой базе.
- Добавленная регрессия: phq4AgeAcknowledgementSurvivesActivityRecreation — открытие PHQ-4, подтверждение 18+, вопрос 1, Activity recreation, отсутствие повторного возрастного экрана.
- GitHub build: ОЖИДАЕТСЯ. Android UI: ОЖИДАЕТСЯ. Не писать PASS до независимой проверки CI.
- Физическое устройство, безопасный update, process death после force-stop, TalkBack: НЕ ПРОВЕРЕНО.
