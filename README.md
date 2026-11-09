# APK Editor Plus v1.10.0.194

[![Сборка](https://github.com/stroy13/test_mod/actions/workflows/build.yml/badge.svg)](https://github.com/stroy13/test_mod/actions/workflows/build.yml)

Полноценный форк Android-приложения **APK Editor Plus** (`ru.apkeditor.plus`)
с собственными исправлениями и развитием кодовой базы. Актуальная версия —
**194** / **1.10.0.194**, история изменений — в [`docs/CHANGELOG.md`](docs/CHANGELOG.md).

Проект собирается штатным Gradle: `:apkEditorPlus:assembleDebug` →
`apkEditorPlus/build/outputs/apk/debug/apkEditorPlus-debug.apk`.
Атрибуция оригинальных авторов и права на сторонние компоненты — в [`NOTICE.md`](NOTICE.md).

## Стек и параметры сборки

| Параметр | Значение |
|---|---|
| Package / applicationId | `ru.apkeditor.plus` |
| versionCode / versionName | `194` / `1.10.0.194` (см. [`docs/CHANGELOG.md`](docs/CHANGELOG.md)) |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| Gradle / AGP | 9.8.2 / 9.5.1 |
| JDK | 17 |
| NDK | 30.0.16248370 (ndkBuild, ABI: `armeabi-v7a`, `arm64-v8a`) |
| Язык | Java (807 файлов), нативный код C/C++ в `jni/` |

## Структура проекта

```
.
├── apkEditorPlus/     # приложение: манифест, Activity, ресурсы, jni/, jniLibs/, libs/
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
└── docs/             # документация и история изменений
```

Модули поставляются с готовыми бинарными зависимостями в `libs/` (`dx-1.10.jar`,
`android-5.1-clst-core.jar`, smali/baksmali и т.д.) и нативными библиотеками в
`apkEditorPlus/src/main/jniLibs/` — они нужны для работы редактора и лежат в репозитории
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
   ./gradlew :apkEditorPlus:assembleDebug --no-daemon --max-workers=1
   ```

   Если wrapper не подходит — системным Gradle 9.8.2:

   ```bash
   gradle :apkEditorPlus:assembleDebug --no-daemon --max-workers=1
   ```

4. Результат: `apkEditorPlus/build/outputs/apk/debug/apkEditorPlus-debug.apk`
   (подпись — debug; для релиза подписывайте своим ключом).

Совет: при нехватке RAM на dex-этапе помогает повторный запуск без `clean` и
`org.gradle.jvmargs=-Xmx900m` в `gradle.properties`.

## CI и готовый APK

`.github/workflows/build.yml` («Сборка») на каждый push прогоняет юнит-тесты шести
модулей (`_AndroidCommon`, `appListUtil`, `appDataEditor`, `httpServer`, `apkEditor`,
`apkEditorPlus`: регрессии манифеста, счётчика классов DEX, тем, сортировки, диагностики XML/smali и сравнения переводов), затем
собирает `:apkEditorPlus:assembleDebug`
(JDK 17, Android SDK platform 37.0, build-tools 37.0.0, NDK 30.0.16248370):

- APK доступен как артефакт запуска (`apkEditorPlus-debug`);
- удачная сборка публикуется в релиз
  [debug-latest](https://github.com/stroy13/test_mod/releases/tag/debug-latest)
  (`apkEditorPlus-debug.apk`, ассет перезаписывается каждой успешной сборкой);
- при падении сборки диагностика (хвост логов Gradle/SDK) публикуется комментарием
  к коммиту — логи GitHub Actions из внешней сети не всегда доступны.

Учтите: это debug-подпись, поверх релизной сборки из релиза `test` такой APK не
встанет — потребуется удалить установленное приложение.

## Список изменений

Исправления и новые возможности форка (сортировка списков, применение всех 12 тем,
прогресс DEX→smali, исправление OOM манифеста и др.) — в [`docs/CHANGELOG.md`](docs/CHANGELOG.md).

## Почему в истории был «Pro»

Оригинальное приложение называлось **APK Editor Pro** (SteelWorks), и форк унаследовал
его внутренние имена: Gradle-модуль `apkEditorPro` и Java-пакет
`com.gmail.heagoo.apkeditor.pro`. Это были технические имена, не бренд продукта:
applicationId уже давно `ru.apkeditor.plus`. Начиная с 186 модуль называется
`apkEditorPlus`, внутренний пакет — `com.gmail.heagoo.apkeditor.plus`, видимое имя —
**APK Editor Plus**. Reflection-вызовы, ProGuard, CI и тесты переименованы согласованно.
Исторические упоминания оригинального Pro сохранены только в атрибуции, CHANGELOG и
проверке установленного оригинального платного пакета (`PaidAppsChecker`).

## Источники и атрибуция

- Upstream: [TimScriptov/ApkEditor](https://github.com/TimScriptov/ApkEditor) (v121).
- Оригинальное приложение: APK Editor Pro, автор **SteelWorks** (пакеты `com.gmail.heagoo.*`).
- Ссылки из оригинального README: [RuStore](https://apps.rustore.ru/app/com.mcal.apkeditor.pro),
  [4PDA](https://4pda.to/forum/index.php?showtopic=575450),
  [Telegram](https://t.me/apkeditorproofficial).

## Лицензия

Файл лицензии не добавлялся: код производный от проприетарного приложения, и автор
репозитория не вправе выдавать на него свободную лицензию. Использование —
исключительно в исследовательских/модификационных целях, см. [`NOTICE.md`](NOTICE.md).
