package com.gmail.heagoo.apkeditor;

public class SmaliMethodInfo {
    public int lineIndex;
    public String methodDesc;

    public SmaliMethodInfo(int lineIndex, String method) {
        this.lineIndex = lineIndex;
        this.methodDesc = method;
    }
}
