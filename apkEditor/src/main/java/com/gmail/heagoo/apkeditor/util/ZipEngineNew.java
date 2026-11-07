package com.gmail.heagoo.apkeditor.util;

/* loaded from: /tmp/apkx/classes9.dex */
final class ZipEngineNew {
    static {
        System.loadLibrary("zipengine_new");
    }

    ZipEngineNew() {
    }

    static native void merge(String str, String str2, String str3, int i, String str4, int i2);

    static native void modify(String str, String str2, String str3, int i, String str4, int i2, String str5, int i3);
}
