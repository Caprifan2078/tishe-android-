# TASKS — «Тише»

| Приоритет | Задача | Статус / критерий |
|---|---|---|
| P1 | PHQ-4 экран 18+ при Activity recreation | Выполнено: код + GitHub CI 42538073673 / 42538073711 + эмулятор 42538073966 8/8; физическое устройство остаётся в общей P1-приёмке |
| P0 | Постоянная подписывающая идентичность APK и миграция | Заблокировано: нельзя внедрять приватный signing key в публичный репозиторий; требуются безопасный процесс и проверка на действующей установке |
| P0 | Проверка риска потери ответов из-за SharedPreferences.apply() при аварийном завершении | Не начато; сначала воспроизведение и критерий консистентности, потом минимальный фикс |
| P1 | Android 8+, TalkBack, крупный шрифт, экранные размеры | Не проверено физическим устройством; критерий — приёмочные протоколы |
| P2 | Проверка UX формулировок тестов, особенно PHQ-4 | Требуется профильная рецензия, выводы не считать диагнозами |
| P3 | iPhone HTML v0.2.4 | Отложено до P0/P1 Android |

Не объединять PR с main автоматически.

## QA update: 2026-11-09
- **P0, silent loss of assessment answers:** реализовано и проверено в QA-коммите `ef59c261d9a0480937cb175993e02f8e06a72ed3`; 130/130 JUnit и 9/9 UI. Диск-снимок проверен сразу после возвращения saveAssessment; при ошибке commit пользователь должен получить уже существующий диалог ошибки. Не признано готовым к релизу.
- **P1, performance:** измерить типичные/p95 latency `saveAssessment` на реальном Android 8+/медленном устройстве и при 500 ответах; при неприемлемой задержке — безопасно вынести I/O с UI-потока с сериализацией изменений.
- **P0, signing:** нет доказательства upgrade-compatible key для уже установленного `ru.tishe.app`; без ключа старую установку не удалять.

## Итог P0 QA (2026-11-09)
- [x] `saveAssessment` больше не сообщает ложный успех до подтверждённой записи ответа: `commit()` проверяется.
- [x] При ложном `commit()` не использовать неподтверждённое значение из RAM SharedPreferences как новый root: подтверждено отрицательным smoke в Android эмуляторе.
- [x] Регрессии 10/10 эмуляторных Android UI и 130/130 JUnit, 2 успешные APK CI. Источник: 42540960128, 42540430991, 42540430895.
- [ ] P1: профилировать задержки главного потока на маломощном Android при 40/150/300/500 ответах; при неприемлемых задержках — последовательная очередь записи вне UI без изменения порядка ответов.
- [ ] P0 release: безопасный постоянный signing key, проверка подписи установленной APK; невозможно гарантировать upgrade от существующих ephemeral debug signatures. Никакой автоматической миграции/удаления данных.
- [ ] P1: реальный Android 8+, TalkBack, крупные шрифты, отказ физического хранилища и аварийное завершение процесса.

## P1 performance — измеренная проблема 2026-11-09
- [x] Инструментировано реальное синхронное сохранение 40/150/300/500 ответов на UI thread; 11/11 Android UI PASS, run 42545977930; p95 23.053/54.326/31.493/68.814 ms, source artifact 13643612156.
- [ ] P1: перенос AES-GCM + `commit()` с main thread при сохранении гарантий подтверждения, порядка, обработки быстрых последовательных ответов и стабильного восстановленного состояния; обязателен новый UI regression и Android perf recheck.
- [ ] Убедиться, что UI thread не блокируется при длительном сохранении; любые записи не объявлять успешными до commit.
- [ ] Физический Android / разные SDK / недорогой телефон остаются непроверенными.

## Benchmark P1 — закрытая QA-подзадача
- [x] Три независимых измерения Android API35; последний зелёный CI 42547438087 (11/11), JSON artifact 13644246528; p95 для 500 вопросов 68.814 / 63.899 / 67.370 ms.
- [x] Исправлен CI extraction: UTP logcat вместо удалённого APK/run-as, одна команда Python; локально валидированы неверные и неполные метрики.
- [ ] Следующий P1: off-main thread для saveAssessment с последовательной подтверждаемой записью, без потери ответов; обязательный regression test и повторный benchmark.

## P1 off-main saving checkpoint — 2026-11-10
- [x] Вынести пять операций `saveAssessment` из UI thread с `Dispatchers.IO`, сохранить `commit()` как подтверждение реальных ответов: commit `a0ad1e4025153fa31891864b6e51db9aa8f16ec0`, 2 APK CI PASS.
- [x] Визуальная защита от повторных тапов и Back до подтверждения, существующее диалоговое сообщение об ошибке, старое состояние при отказе.
- [x] Android emulator 13/13 PASS с новым main-thread responsiveness slow-writer test и error-propagation test; JUnit 130/130, Lint 0 errors.
- [ ] Полноценный Compose E2E со специально задержанным реальным SharedPreferences.commit(), серия быстрых тапов/Back и Activity recreation в ходе записи.
- [ ] QA на физических Android 8+, 12+, 15+ устройствах, измерение времени отклика и UX модального индикатора.
- [ ] P0: стабильная подпись APK и обновление установленной версии без удаления защищённых данных.


## QA results completeness 2026-11-11
- [x] All 48 individual modules render an actual conclusion after a final answer + Results click on Android Compose (GitHub Actions 43013180435, 15/15).
- [x] All 40/150/300/500 journey modes render a result after final question via their real button; four pathways checked in the same green run.
- [x] HTML Web 0.2.5, all 48 module UI results and four full journey UI results checked in local Chromium, plus 156 synthetic outcomes. QA branch `qa/web025-results-20261111`, GitHub CI 43013436451 SUCCESS.
- [ ] Android QA: delay the real IO commit, tap rapidly, rotate/recreate in the middle of saving; verify no acknowledged answer disappears and later action is not silently lost. Needs targeted real app regression.
- [ ] iPhone Safari: offline file vs HTTPS crypto, real password vault creation, failed storage recovery, export/import backup.
- [ ] P0 APK release signing: user-safe update with existing data, never uninstall current app for ephemeral debug build.
