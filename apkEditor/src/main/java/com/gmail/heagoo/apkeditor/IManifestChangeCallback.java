package com.gmail.heagoo.apkeditor;

interface IManifestChangeCallback {
    void manifestChanged(String str);

    String tryToDeleteSection(LineRecord lineRecord);
}
