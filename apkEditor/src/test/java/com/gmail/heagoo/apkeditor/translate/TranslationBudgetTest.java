package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import org.junit.Test;

public class TranslationBudgetTest {
    @Test public void limitsPerStringWork() {
        assertEquals(128, TranslationBudget.maxTokens("Hello"));
        assertEquals(296, TranslationBudget.maxTokens(new String(new char[100])));
        assertEquals(768, TranslationBudget.maxTokens(new String(new char[1000])));
    }
}
