# CHANGELOG DEV — «Тише»

## 2026-11-09 — QA PHQ-4 rotation on v0.27
- Обнаружено: SurveyScreen использовал remember(module.id) для acceptedAdultScreening; после recreation подтверждение 18+ сбрасывалось.
- Добавлен apply-qa-phq4-rotation.py: точечная замена на rememberSaveable(module.id), без изменения шифрования, ответов, JSON, идентификатора пакета или версии.
- Добавлен новый Android UI-тест в существующий пакет через тот же патч; проверяет возврат к вопросу после Activity recreation.
- Обновлены отдельные Android build CI и emulator smoke CI workflows, чтобы применяли патч после v0.27.
- Итог сборки и регрессии записывать только по фактическим CI в QA_REPORT.md.

### Проверки QA-коммита b216c6d (09.11.2026)
- Preview build + JUnit + lint: PASS, run 42538073673, 130/130 JUnit, Lint 0 errors / 4 warnings.
- Regular build: PASS, run 42538073711.
- Android emulator: PASS, run 42538073966, 8 instrumented tests / 0 errors, включая phq4AgeAcknowledgementSurvivesActivityRecreation.
- Физический Android и безопасное обновление установленного приложения не проверялись.

## 2026-11-09 — durable assessment answers (QA)
- `scripts/apply-qa-assessment-durable.py` changes only the default assessment write persistence and the cursor-only `onMove` call; original `encrypted` key, JSON schema, alias `tishe_aes_v1`, version and package remain unchanged.
- `scripts/AssessmentDurableDiskSmokeTest.kt` adds a real Android file-system regression. Existing 8 UI tests retained.
- GitHub Actions: regular 42539551427 SUCCESS; preview 42539551472 SUCCESS; emulator 42539551445 SUCCESS (9/9). JUnit 130/130.
- Remaining: avoid UI-thread fsync stalls before production; stable signing / migration and physical QA unresolved.

## 2026-11-09 — защитить cached document после неудачного commit
- Исходный catch обнулял documentCache, после чего SharedPreferences в RAM мог возвращать ещё не подтверждённый ciphertext как уже сохранённый. Теперь предыдущий изолированный объект оставляется, а следующая durable запись не пропускается.
- `FailedCommitCacheSmokeTest`: имитация Editor.commit()=false после изменения RAM, затем проверка, что SecureStorage по-прежнему выдаёт последний подтверждённый ответ.
- Первое QA падение связано с тестовой цепочкой Editor.putString; исправлена тестовая реализация (commit `b27ecbc`), повторный Android UI CI 42540960128 **10/10 PASS**.
- Приложение v0.27.0/code34; не выполнялись merge в main, миграция, изменение схемы AES-GCM или публикация ключа.

## 2026-11-09 — QA on-main write latency
- Added `AssessmentSaveLatencyTest.kt` for 128 real main-thread encrypted writes in an isolated preference namespace, without changing customer data or product APK.
- Added `scripts/extract-qa-assessment-latency.py` to collect numeric data from UTP logcat. Initial workflow failed after passing 11/11 UI because Gradle removed app before `run-as`; extraction repaired. Original evidence preserved in `qa-results/assessment-latency-first-run.json`.
- First measurement p95: 23.053 / 54.326 / 31.493 / 68.814 ms for 40/150/300/500; off-main design is the next P1 task.
