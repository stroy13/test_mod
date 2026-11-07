package com.gmail.heagoo.apkeditor;

import java.util.Map;

public interface ApkParseConsumer {
    void decodeFailed(String str);

    void decodeProgress(int i);

    void decodeProgressCount(int i, int i2);

    void resTableDecoded(boolean z);

    void resourceDecoded(Map<String, String> map);
}
