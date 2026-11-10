package com.gmail.heagoo.apkeditor;

interface IManifestChangeCallback {
    boolean manifestChanged(String str);

    String tryToDeleteSection(LineRecord lineRecord);
}
