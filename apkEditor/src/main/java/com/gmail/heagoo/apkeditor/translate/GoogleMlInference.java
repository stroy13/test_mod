package com.gmail.heagoo.apkeditor.translate;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import common.types.StringItem;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** ML Kit on-device translation; missing models download after explicit user consent. */
public final class GoogleMlInference implements TranslationSession {
    private final Translator translator;
    private final AtomicBoolean cancelled;
    private final TranslationTrace trace;

    public GoogleMlInference(String source, String target, TranslationTrace trace, AtomicBoolean cancelled) throws Exception {
        this.cancelled = cancelled;
        this.trace = trace;
        String from = TranslateLanguage.fromLanguageTag(source);
        String to = TranslateLanguage.fromLanguageTag(target);
        if (from == null || to == null) throw new IOException("Unsupported ML Kit language");
        if (from.equals(to)) throw new IOException("Source and target languages must differ");
        translator = Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build());
        try {
            trace.stage(TranslationTrace.Stage.LOAD_MODEL);
            if (cancelled.get()) throw new IOException("Translation cancelled");
            Tasks.await(translator.downloadModelIfNeeded(new DownloadConditions.Builder().build()), 5, TimeUnit.MINUTES);
            if (cancelled.get()) throw new IOException("Translation cancelled");
            trace.stage(TranslationTrace.Stage.MODEL_READY);
        } catch (Exception | LinkageError e) { translator.close(); throw e; }
    }
    @Override public String translate(String value, String language, String sourceQualifier, String resourceName) throws Exception {
        if (cancelled.get()) throw new IOException("Translation cancelled");
        trace.stringStart(value.length(), 0);
        String output = Tasks.await(translator.translate(value), 2, TimeUnit.MINUTES);
        if (cancelled.get()) throw new IOException("Translation cancelled");
        trace.stage(TranslationTrace.Stage.VALIDATE);
        return TranslationOutput.clean(value, output);
    }
    @Override public List<String> translateBatch(List<StringItem> items, String language, String sourceQualifier) throws Exception {
        throw new UnsupportedOperationException("ML Kit does not use the LLM batch protocol");
    }
    @Override public void requestCancel() { cancelled.set(true); }
    @Override public void close() { translator.close(); }
}
