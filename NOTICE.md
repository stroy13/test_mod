# NOTICE / attribution

This repository contains a **recovered source tree** of the Android application
**APK Editor Plus** (`ru.apkeditor.plus`, version 1.10.0.175).

## Rights

- The original application and all of its code, resources and assets are the property of
  their respective authors — **SteelWorks** / the APK Editor Pro project. No license is
  granted by this repository, and none of the files here are relicensed.
- The tree was reconstructed from the upstream open copy
  ([TimScriptov/ApkEditor](https://github.com/TimScriptov/ApkEditor), v121) plus classes and
  resources decompiled from a reference APK v175 with JADX and apktool. Recovered code is
  therefore **not byte-identical** to the original sources, and decompilation introduces
  losses and artefacts.
- Third-party components keep their own licenses: smali/baksmali (BSD), dx (Apache-2.0),
  nanohttpd (BSD), gesture-imageview (Apache-2.0), libzip (BSD), and others bundled under
  `libs/` and `src/main/jni/`.

## Signing keys in the tree

`apkEditorPro/src/main/assets/testkey.pk8`, `testkey.x509.pem`, `editorkey.pk8` and
`editorkey.x509.pem` are the **public AOSP test keys** that the app itself ships to sign
patched APKs. They are not private developer keys, but you should not use them for anything
you distribute.

## Intended use

Research, interoperability and modding of the application by its owner. Do not redistribute
the resulting APK if you have no rights to the original application. No warranty of any kind.
