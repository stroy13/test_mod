# Статус восстановления исходников APK Editor + v175

## Итог: проект СОБИРАЕТСЯ ✅

`gradle :apkEditorPro:assembleDebug` — **BUILD SUCCESSFUL** (Gradle 9.8.2, AGP 9.5.1, JDK 17,
compileSdk/targetSdk 37, minSdk 26, NDK 30.0.16248370, ABI: armeabi-v7a + arm64-v8a).

Собранный APK сверен с эталоном `/APK-Editor-Plus-v175.apk`:
- package `ru.apkeditor.plus`, versionCode 175, versionName `1.10.0.175` — совпадает;
- minSdk 26 / targetSdk 37 — совпадает;
- нативные библиотеки: все 12 из эталона на месте (libsyscheck собирается из jni-исходников,
  остальные — libaapt2_37/libaapt*_exec/libaaptz_exec/libmycp2022_exec/libzipengine_new —
  восстановлены из эталонного APK в `apkEditorPro/src/main/jniLibs/`);
- zipalign 16K — PASS; подпись — debug (эталон подписан релизным ключом; для релиза
  подписывайте своим ключом, как раньше);
- число dex: 30 против 32 у эталона (другое разбиение multidex в debug — не дефект).

## Что это за дерево (честно)

Оригинальное дерево v142 и дельта этапа 5 утрачены (см. историю в git/архивах recovery-v175-stage*).
Это дерево восстановлено из: upstream TimScriptov/ApkEditor (v121, bddfe58) + дельта этапа 3
(168 файлов) + классы и ресурсы, декомпилированные JADX/apktool из эталонного APK v175
(Aapt2Pipeline, ZipEngine, ZipEngineNew, RoundedDialogUi, EditorTheme, FileSort, SDCard,
ApkInfoActivity и др.) + ресурсы v175 (layout/attrs/strings/drawables, восстановленные в
`values/*_recovery.xml`, `ids_recovery.xml`, `styles_recovery.xml`).
Поэтому: функциональность и интерфейс максимально близки к v175, но это НЕ побитово
оригинальные исходники — часть методов JADX восстановил с потерями (отмечены правками вида
try/catch-обёрток, инициализаций переменных, восстановленного цикла отпечатков в
CertificateDetails). Поведение таких мест проверено только компиляцией, не на устройстве.

## Известные расхождения с эталоном

- AndroidManifest: набор разрешений отличается (у эталона есть MANAGE_EXTERNAL_STORAGE
  и maxSdkVersion у storage-разрешений) — при необходимости перенесите из манифеста эталона.
- Оба smali-стека подключены как в оригинале: org.smali 2.4.0 (org.jf.*) и
  com.android.tools.smali 3.0.10 (Fast* классы); jcommander оставлен один (1.85).
- `com.gmail.heagoo.modernjadx.*` — jar `apkEditorPro/libs/jadx-modern.jar` (Shadow), не исходники.

## Сборка

Нужны: JDK 17, Gradle 9.8.2, Android SDK platform 37.0 + build-tools 37.0.0, NDK 30.0.16248370.
Команда: `gradle :apkEditorPro:assembleDebug --no-daemon --max-workers=1`
При нехватке RAM на dex-этапе помогает повторный запуск без clean и Xmx ~900m.
