# CHANGELOG DEV — «Тише»

## 2026-11-09 — QA PHQ-4 rotation on v0.27
- Обнаружено: SurveyScreen использовал remember(module.id) для acceptedAdultScreening; после recreation подтверждение 18+ сбрасывалось.
- Добавлен apply-qa-phq4-rotation.py: точечная замена на rememberSaveable(module.id), без изменения шифрования, ответов, JSON, идентификатора пакета или версии.
- Добавлен новый Android UI-тест в существующий пакет через тот же патч; проверяет возврат к вопросу после Activity recreation.
- Обновлены отдельные Android build CI и emulator smoke CI workflows, чтобы применяли патч после v0.27.
- Итог сборки и регрессии записывать только по фактическим CI в QA_REPORT.md.
