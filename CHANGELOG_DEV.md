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

## QA 2026-11-09 — latency repeat and extractor invocation
- Second 11/11 emulator pass; main-thread 500-question p95 63.899 ms, confirming previous 68.814 ms risk.
- Fixed CI invocation: one shell line for Python logcat extractor (runner script does not support line-joined arguments). Original evidence preserved.

## 2026-11-09 — latency measurement QA signed off
- Fixed extraction pipeline to rely on Gradle per-test logcat, validated input, one-line runner command. GitHub workflow 42547438087 SUCCESS, 11/11 Android instrumentation tests, JSON evidence archived.
- Three independent repeated benchmarks show 500-question p95 ~64–69 ms; upcoming task is risk-controlled off-main storage, not reverting durable commit to apply().

## 2026-11-10 — QA off-main questionnaire save
- Added `AssessmentAsyncWriter`: secure synchronized full-document write runs on IO dispatcher; resumed Compose UI commits the answer only after checked durable persistence.
- Migrated questionnaire answers, cursor moves, first Journey checkpoint and preferences away from direct main-thread save; modal UI prevents stale repeat taps during operation, recoverable error dialog retained.
- No change to encryption, data schema, installed package identity or version number.
- GitHub CI: 42723963967 regular PASS; 42723964171 preview PASS; JUnit 130/130; Lint 0 errors/4 warnings; 42723964260 emulator 13/13 PASS. See `QA_REPORT.md` for full proof/limits.
