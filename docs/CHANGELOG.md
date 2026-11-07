# Изменения

## 1.10.0.176 — разбор таблицы ресурсов framework (Android 16), versionCode 176

Симптом: на Android 16 (SDK 36) экран так и остаётся на «Обработка манифеста и
ресурсов…», в диагностике видно поток в состоянии RUNNABLE:

```
at java.lang.String.fillBytesLatin1(Native Method)
at java.lang.AbstractStringBuilder.append
at brut.androlib.res.decoder.ARSCDecoder.addMissingResSpecs(ARSCDecoder.java:722)
at brut.androlib.res.decoder.ARSCDecoder.readTableTypeSpec(ARSCDecoder.java:289)
at brut.androlib.res.decoder.ARSCDecoder.readTablePackage(ARSCDecoder.java:237)
at brut.androlib.res.data.ResTable.loadFrameworkPkg(ResTable.java:207)
```

Это не дедлок, а квадратичная работа при загрузке системной таблицы ресурсов:

- `ARSCDecoder.readTableType()` создаёт `mMissingResSpecs` заново для **каждого**
  `TYPE`-чанка, то есть для каждой конфигурации одного и того же типа ресурса;
- после каждого чанка вызывается `addMissingResSpecs()`, который перебирает весь
  массив (`entryCount`, часто 65536) и для каждой «отсутствующей» записи **до**
  проверки `hasResSpec()` успевал построить `ResResSpec` со склейкой строки
  `"dummy_ae_" + hex` — то есть десятки миллионов лишних объектов и конкатенаций,
  результат которых сразу выбрасывался;
- на Android 16 у framework-таблицы десятки типов и десятки конфигураций на тип,
  поэтому загрузка `framework-res` занимала минуты (в логе: `manifest: 31518 ms`,
  `Android framework: 13812 ms` и процесс продолжался).

Исправление в `ARSCDecoder`:

1. проверка `mPkg.hasResSpec(id)` выполняется до создания dummy-спеки (убрана
   конкатенация строки и аллокация на каждый промах);
2. добавлен флаг `mMissingResSpecsDone`: для каждого типа ресурса достаточно одного
   прохода — первый чанк типа уже создаёт dummy-спеки для всех отсутствующих id,
   поэтому последующие конфигурации того же типа больше не сканируются
   (флаг сбрасывается в `readSingleTableTypeSpec()`).

Поведение не меняется (набор dummy-спек тот же), ускоряется загрузка и системной
таблицы, и таблицы самого разбираемого APK.

Дополнительно в `ResTable`:

3. системная таблица грузится не более одной попытки на разбор
   (`mFrameworkLoadTried`): раньше при сбое загрузки `getPackage(1)` возвращал `null`
   и следующий `android:`-атрибут манифеста запускал полный разбор framework-таблицы
   заново — на каждый атрибут, то есть практически бесконечно;
4. `loadFrameworkPkg()` больше не падает на таблице с несколькими пакетами
   (`Arsc files with zero or multiple packages`) — выбирается пакет с нужным id
   (или пакет `android`), как у новых системных таблиц (Android 14+);
5. результат загрузки framework (из кэша / загружено N пакетов за X мс / ошибка)
   выводится в диагностике («Копировать диагностику»).

Заодно `versionCode`/`versionName` подняты до 176 / `1.10.0.176`, чтобы собранный
APK было легко отличить от релизной сборки 175.

## 1.10.0.175-fix — исправление «вечной» загрузки

Симптом: при открытии APK экран навсегда остаётся на надписи «Обработка манифеста
и ресурсов…» (индекс 2 в `ApkInfoActivity.decodeProgress`), спиннер крутится,
ошибки нет, кнопка «Повторить» не появляется.

Причина — разбор APK умирал или застревал, не сообщая об этом UI:

- `ApkParseThread.run()` ловил только `Exception`/`LinkageError`/`OutOfMemoryError`,
  а любой другой `Error` (`NoClassDefFoundError`, `StackOverflowError`, …) или ранний
  `return` при `activityRef.get() == null` оставляли экран в состоянии загрузки;
- раньше ошибка сообщалась только из конца `run()`, поэтому «тихая» смерть потока
  вообще не доходила до `decodeFailed()`.

Что сделано:

1. `ApkParseThread` — весь разбор обёрнут в `try { … } catch (Throwable)` с
   гарантированным `decodeFailed()` (`reportFailure()` вызывается ровно один раз,
   в том числе при `Error`), ранний `return` заменён на отчёт об ошибке.
2. Добавлен watchdog `apk-decode-watchdog`: если прогресс не менялся 120 с
   (для фазы 0 «жду предыдущую операцию» — 20 мин), пользователю показывается
   ошибка с текущей стадией и таймингами (`getStageTimings()`), разбор
   останавливается, кнопка «Повторить» работает как раньше.
3. `ApkDecoderMine.decodeFileResources()` и `decodeValues()` теперь сообщают
   `progressCount(done, total)` — счётчик «N / M» на экране загрузки обновляется,
   а watchdog получает признак жизни. Раньше `progressCount` не вызывался вообще,
   из-за чего экран выглядел зависшим даже во время нормального (последовательного)
   разбора большого APK.
4. `ApkInfoActivity` — счётчик показывается вместе с текущей подписью стадии;
   `prepareStringList()` больше не падает с `ClassCastException` на строковых
   ресурсах-не-скалярах (`ResFileValue`/`ResBagValue`), а его вызовы обёрнуты в
   `try/catch (Throwable)`, чтобы одна битая запись не убивала весь разбор.
5. Новые строки `lab_load_stalled` (en/ru).

Проверка: синтаксис изменённых файлов проверен парсером Java, полноценная сборка —
workflow `.github/workflows/build.yml` (собирает `:apkEditorPro:assembleDebug`).
