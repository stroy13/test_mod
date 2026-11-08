package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The editable package field must accept valid IDs regardless of the editor's own applicationId. */
public class PackageNameValidatorTest {
    @Test
    public void acceptsCommonAndUnderscoreIdentifiers() {
        assertNull(PackageNameValidator.validationError("com.example.app", "com.original.app"));
        assertNull(PackageNameValidator.validationError("com.example.my_app2", "com.original.app"));
        assertNull(PackageNameValidator.validationError("org.test_1.app", "com.original.app"));
    }

    @Test
    public void trimsAccidentalWhitespace() {
        assertEquals("com.example.app", PackageNameValidator.normalize("  com.example.app \n"));
        assertNull(PackageNameValidator.validationError("  com.example.app  ", "com.original.app"));
    }

    @Test
    public void rejectsEmptySingleSegmentAndInvalidCharacters() {
        assertNotNull(PackageNameValidator.validationError("", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError("example", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError("com.example-app", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError("com.пример.app", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError("com.example app", "com.original.app"));
    }

    @Test
    public void rejectsEmptySegmentsAndLeadingDigits() {
        assertNotNull(PackageNameValidator.validationError("com..app", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError(".com.app", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError("com.app.", "com.original.app"));
        assertNotNull(PackageNameValidator.validationError("com.1app", "com.original.app"));
        assertTrue(PackageNameValidator.validationError("com.1app", "com.original.app").contains("буквы"));
    }

    @Test
    public void enforcesResourceTableLengthLimit() {
        String longName = "com." + repeat('a', 124);
        assertEquals(128, longName.length());
        assertNotNull(PackageNameValidator.validationError(longName, "com.original.app"));
        String maxName = "com." + repeat('a', 123);
        assertEquals(127, maxName.length());
        assertNull(PackageNameValidator.validationError(maxName, "com.original.app"));
    }

    @Test
    public void unchangedLegacyIdentifierStillAllowsEditingOtherProperties() {
        assertNull(PackageNameValidator.validationError("legacy", "legacy"));
        assertNotNull(PackageNameValidator.validationError("legacy", "com.original.app"));
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
