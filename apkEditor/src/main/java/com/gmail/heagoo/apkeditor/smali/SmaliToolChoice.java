package com.gmail.heagoo.apkeditor.smali;

import android.content.Context;
import android.preference.PreferenceManager;

/** Independent choices, defaulting to the previously used implementation. */
public final class SmaliToolChoice {
    private SmaliToolChoice() { }
    public static String decoderClass(Context context) {
        String choice = PreferenceManager.getDefaultSharedPreferences(context).getString("SmaliDecoderVersion", "modern");
        return "legacy".equals(choice) ? "com.gmail.heagoo.apkeditor.plus.LegacyDexDecoder"
                : "com.gmail.heagoo.apkeditor.plus.DexDecoder";
    }
    public static String encoderClass(Context context) {
        String choice = PreferenceManager.getDefaultSharedPreferences(context).getString("SmaliEncoderVersion", "legacy");
        return "modern".equals(choice) ? "com.gmail.heagoo.apkeditor.plus.ModernDexEncoder"
                : "com.gmail.heagoo.apkeditor.plus.DexEncoder";
    }
}
