package com.gmail.heagoo.apkeditor.translate;

import java.io.IOException;

/** A single translation is unsafe to use; engine and model failures remain fatal. */
public final class TranslationRejectedException extends IOException {
    public enum Reason { EMPTY, REASONING, PLACEHOLDER, INVALID_UTF8, TOKEN_LIMIT, SOURCE_TOO_LONG }
    private final Reason reason;
    public TranslationRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
