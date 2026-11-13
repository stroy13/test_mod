package com.gmail.heagoo.apkeditor.translate;

import common.types.StringItem;
import java.util.List;

/** One cancellable local translation session; translation runs off the UI thread. */
public interface TranslationSession extends AutoCloseable {
    String translate(String value, String language, String sourceQualifier, String resourceName) throws Exception;
    List<String> translateBatch(List<StringItem> items, String language, String sourceQualifier) throws Exception;
    void requestCancel();
    @Override void close();
}
