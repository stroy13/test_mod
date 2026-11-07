# APK Editor Plus v1.10.0.175 — исходники

[![Сборка](https://github.com/stroy13/test_mod/actions/workflows/build.yml/badge.svg)](https://github.com/stroy13/test_mod/actions/workflows/build.yml)

Восстановленное дерево исходников Android-приложения **APK Editor Plus**
(`ru.apkeditor.plus`, база — versionCode **175** / versionName **1.10.0.175**;
текущая сборка с исправлениями — **180** / **1.10.0.180**, см.
[`docs/CHANGELOG.md`](docs/CHANGELOG.md)).

Проект собирается штатным Gradle: `:apkEditorPro:assembleDebug` →
`apkEditorPro/build/outputs/apk/debug/apkEditorPro-debug.apk`.

> Это восстановленные (декомпилированные и приведённые к сборке) исходники, а не побитово
> оригинальный код разработчика. Подробности, известные расхождения и история правок —
> в [`docs/RECOVERY-STATUS.md`](docs/RECOVERY-STATUS.md) и [`docs/BUILD-ERRORS.txt`](docs/BUILD-ERRORS.txt).
> Права на оригинальное приложение принадлежат его авторам, см. [`NOTICE.md`](NOTICE.md).

## Стек и параметры сборки

| Параметр | Значение |
|---|---|
| Package / applicationId | `ru.apkeditor.plus` |
| versionCode / versionName | `180` / `1.10.0.180` (база 175 + исправления, см. [`docs/CHANGELOG.md`](docs/CHANGELOG.md)) |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| Gradle / AGP | 9.8.2 / 9.5.1 |
| JDK | 17 |
| NDK | 30.0.16248370 (ndkBuild, ABI: `armeabi-v7a`, `arm64-v8a`) |
| Язык | Java (766 файлов), нативный код C/C++ в `jni/` |

## Структура проекта

```
.
├── apkEditorPro/     # приложение: манифест, Activity, ресурсы, jni/, jniLibs/, libs/
├── apkEditor/        # ядро редактора: декомпиляция/сборка APK, патчи, smali
├── apkLibrary/       # работа с форматом APK (подпись, разбор, asn1)
├── appDataEditor/    # редактор данных приложения (SharedPreferences, БД)
├── appListUtil/      # утилиты списка установленных приложений
├── httpServer/       # встроенный HTTP-сервер (nanohttpd)
├── imageViewLib/     # виджет изображения с жестами (gesture-imageview)
├── PngEditor/        # редактор PNG (в т.ч. com.polites.android)
├── sqliteUtil/       # хелперы работы с SQLite
├── _AndroidCommon/   # общий код com.gmail.heagoo.common
├── _AndroidEditor/   # общий код редактора (com.gmail.heagoo.neweditor)
├── build.gradle      # корневой build-скрипт (AGP 9.5.1)
├── settings.gradle   # список модулей
├── gradle.properties # JVM-аргументы, AndroidX/Jetifier
├── gradlew, gradle/  # Gradle wrapper (9.8.2)
└── docs/             # статус восстановления и известные проблемы сборки
```

Модули поставляются с готовыми бинарными зависимостями в `libs/` (`dx-1.10.jar`,
`android-5.1-clst-core.jar`, smali/baksmali и т.д.) и нативными библиотеками в
`apkEditorPro/src/main/jniLibs/` — они нужны для работы редактора и лежат в репозитории
намеренно.

## Сборка

1. Установить JDK 17, Android SDK platform 37 + build-tools 37.0.0,
   NDK 30.0.16248370.
2. Создать `local.properties` (он не хранится в git):

   ```bash
   cp local.properties.example local.properties
   # затем вписать свои пути sdk.dir / ndk.dir
   ```

3. Собрать:

   ```bash
   ./gradlew :apkEditorPro:assembleDebug --no-daemon --max-workers=1
   ```

   Если wrapper не подходит — системным Gradle 9.8.2:

   ```bash
   gradle :apkEditorPro:assembleDebug --no-daemon --max-workers=1
   ```

4. Результат: `apkEditorPro/build/outputs/apk/debug/apkEditorPro-debug.apk`
   (подпись — debug; для релиза подписывайте своим ключом).

Совет из практики восстановления: при нехватке RAM на dex-этапе помогает повторный запуск
без `clean` и `org.gradle.jvmargs=-Xmx900m` в `gradle.properties`.

## CI и готовый APK

`.github/workflows/build.yml` («Сборка») на каждый push прогоняет юнит-тесты
(`:apkEditor:testDebugUnitTest` и `:apkEditorPro:testDebugUnitTest`: регрессия на OOM
в форматтере манифеста и проверка реального счётчика классов DEX) и
собирает `:apkEditorPro:assembleDebug`
(JDK 17, Android SDK platform 37.0, build-tools 37.0.0, NDK 30.0.16248370):

- APK доступен как артефакт запуска (`apkEditorPro-debug`);
- удачная сборка публикуется в релиз
  [debug-latest](https://github.com/stroy13/test_mod/releases/tag/debug-latest)
  (`apkEditorPro-debug.apk`, ассет перезаписывается каждой успешной сборкой);
- при падении сборки диагностика (хвост логов Gradle/SDK) публикуется комментарием
  к коммиту — логи GitHub Actions из внешней сети не всегда доступны.

Учтите: это debug-подпись, поверх релизной сборки из релиза `test` такой APK не
встанет — потребуется удалить установленное приложение.

## Что изменено относительно «сырого» архива исходников

Архив с исходниками лежал в релизе одним файлом со случайным именем
(`fkMIqyv9….tar.xz`) внутри папки `work175/`. Здесь он разложен как нормальный
Gradle-проект в корне репозитория:

- содержимое `work175/` поднято в корень, папка `work175/` убрана;
- добавлены `README.md`, `NOTICE.md`, `.gitignore`, `.gitattributes`,
  `local.properties.example`;
- `local.properties` (машинные пути `/home/user/android-env/...`) удалён из репозитория
  и внесён в `.gitignore`, вместо него — пример;
- `.idea/` (локальные настройки IDE) исключена;
- добавлен Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`); в исходном
  `.gitignore` каталог `gradle/` выгружался целиком, из-за чего `./gradlew` не работал;
- убраны артефакты декомпиляции: 109 «россыпью» `*.class` в `apkEditorPro/libs/`
  (они не используются сборкой — там подключаются только jar) и
  `apkEditor/src/main/jni/injector/jni/injector.c.bak`;
- отчёт о восстановлении и лог ошибок сборки перенесены в `docs/`.

## Список изменений

Правки поверх восстановленных исходников (исправление «вечной» загрузки
«Обработка манифеста и ресурсов…» и др.) — в [`docs/CHANGELOG.md`](docs/CHANGELOG.md).

## Источники и происхождение

- Upstream: [TimScriptov/ApkEditor](https://github.com/TimScriptov/ApkEditor) (v121).
- Оригинальное приложение: APK Editor Pro, автор **SteelWorks** (пакеты `com.gmail.heagoo.*`).
- Классы и ресурсы v175, восстановленные JADX/apktool из эталонного APK.
- Ссылки из оригинального README: [RuStore](https://apps.rustore.ru/app/com.mcal.apkeditor.pro),
  [4PDA](https://4pda.to/forum/index.php?showtopic=575450),
  [Telegram](https://t.me/apkeditorproofficial).

## Лицензия

Файл лицензии не добавлялся: код производный от проприетарного приложения, и автор
репозитория не вправе выдавать на него свободную лицензию. Использование —
исключительно в исследовательских/модификационных целях, см. [`NOTICE.md`](NOTICE.md).
