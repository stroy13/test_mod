package com.gmail.heagoo.apkeditor.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A patched, obfuscated package stopped building:
 *
 * <pre>
 *   AAPT2 compile: res/values/acs.xml:3: error: unknown resource type 'ac'.
 *   res/values/public.xml:1605: error: invalid resource type 'ac' in &lt;public&gt;.
 * </pre>
 *
 * <p>AAPT2 looks types up in a fixed table, so a shrinker that renames a type to
 * {@code ac} puts the project outside what it can compile. The original AAPT
 * creates types on demand and accepts them, which is why the build worked
 * before AAPT2 was selected. These tests pin the table AAPT2 actually uses and
 * the scan that routes such a project back to the original AAPT.
 */
public class Aapt2ResourceTypesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** Every key of sResourceTypeMap in AOSP tools/aapt2/Resource.cpp. */
    private static final String[] AOSP_TYPES = {
            "anim", "animator", "array", "attr", "^attr-private", "bool", "color",
            "configVarying", "dimen", "drawable", "font", "fraction", "id",
            "integer", "interpolator", "layout", "macro", "menu", "mipmap",
            "navigation", "plurals", "raw", "string", "style", "styleable",
            "transition", "xml"};

    @Test
    public void acceptsEveryTypeInTheAapt2Table() {
        for (String type : AOSP_TYPES) {
            assertTrue(type, Aapt2ResourceTypes.isSupported(type));
        }
    }

    @Test
    public void rejectsTheObfuscatedTypeFromTheReport() {
        assertFalse(Aapt2ResourceTypes.isSupported("ac"));
        assertFalse(Aapt2ResourceTypes.isSupported(""));
        assertFalse(Aapt2ResourceTypes.isSupported(null));
        // Case sensitive: the AOSP map is a plain std::map<StringPiece, ...>.
        assertFalse(Aapt2ResourceTypes.isSupported("Drawable"));
    }

    @Test
    public void qualifiedValueTypesFollowThePartBeforeTheDot() {
        assertTrue(Aapt2ResourceTypes.isSupportedValueType("string.foo"));
        assertTrue(Aapt2ResourceTypes.isSupportedValueType("drawable.bar.baz"));
        assertFalse(Aapt2ResourceTypes.isSupportedValueType("ac.foo"));
        // A trailing dot is not a qualifier.
        assertFalse(Aapt2ResourceTypes.isSupportedValueType("string."));
        assertFalse(Aapt2ResourceTypes.isSupportedValueType(".string"));
    }

    @Test
    public void directoryNamesAreSplitAtTheFirstDash() {
        assertTrue(Aapt2ResourceTypes.isSupportedDirectory("drawable-hdpi"));
        assertTrue(Aapt2ResourceTypes.isSupportedDirectory("layout"));
        assertTrue(Aapt2ResourceTypes.isSupportedDirectory("values-ru-rRU"));
        assertTrue(Aapt2ResourceTypes.isSupportedDirectory("values"));
        assertFalse(Aapt2ResourceTypes.isSupportedDirectory("ac"));
        assertFalse(Aapt2ResourceTypes.isSupportedDirectory("ac-hdpi"));
        assertFalse(Aapt2ResourceTypes.isSupportedDirectory("valuesfoo"));
        assertFalse(Aapt2ResourceTypes.isSupportedDirectory("valuesfoo-ru"));
        assertEquals("drawable", Aapt2ResourceTypes.directoryType("drawable-hdpi"));
        assertEquals("ac", Aapt2ResourceTypes.directoryType("ac-v4"));
    }

    private File res() throws IOException {
        return tmp.newFolder("res");
    }

    private static void mkdir(File parent, String name) {
        assertTrue(new File(parent, name).mkdir());
    }

    private static void write(File file, String text) throws IOException {
        Files.write(Paths.get(file.getAbsolutePath()), text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void cleanProjectKeepsAapt2() throws IOException {
        File res = res();
        mkdir(res, "drawable-hdpi");
        mkdir(res, "layout");
        mkdir(res, "values");
        mkdir(res, "values-ru");
        write(new File(res, "values/public.xml"),
                "<resources>\n  <public type=\"string\" name=\"app_name\" id=\"0x7f010000\" />\n</resources>\n");
        assertTrue(Aapt2ResourceTypes.unsupportedIn(res).isEmpty());
    }

    @Test
    public void renamedDirectoryIsReported() throws IOException {
        File res = res();
        mkdir(res, "ac");
        mkdir(res, "ac-hdpi");
        mkdir(res, "layout");
        assertEquals(new HashSet<>(Arrays.asList("ac")), Aapt2ResourceTypes.unsupportedIn(res));
    }

    /**
     * The reported failure had no {@code ac} directory at all: the type only
     * ever held values, so public.xml is the only place it is named.
     */
    @Test
    public void valuesOnlyTypeIsFoundInPublicXml() throws IOException {
        File res = res();
        mkdir(res, "values");
        mkdir(res, "layout");
        write(new File(res, "values/public.xml"),
                "<resources>\n"
                        + "  <public type=\"string\" name=\"a\" id=\"0x7f010000\" />\n"
                        + "  <public type=\"ac\" name=\"b\" id=\"0x7f020000\" />\n"
                        + "  <public type=\"ac\" name=\"c\" id=\"0x7f020001\" />\n"
                        + "</resources>\n");
        assertEquals(new HashSet<>(Arrays.asList("ac")), Aapt2ResourceTypes.unsupportedIn(res));
    }

    @Test
    public void qualifiedLedgerTypesAreNotFalsePositives() throws IOException {
        File res = res();
        mkdir(res, "values");
        write(new File(res, "values/public.xml"),
                "<resources><public type=\"string.alt\" name=\"a\" id=\"0x7f010000\" /></resources>\n");
        assertTrue(Aapt2ResourceTypes.unsupportedIn(res).isEmpty());
    }

    @Test
    public void severalTypesAreSortedAndDeduplicated() throws IOException {
        File res = res();
        mkdir(res, "zz");
        mkdir(res, "ac");
        mkdir(res, "ac-v4");
        mkdir(res, "values");
        write(new File(res, "values/public.xml"),
                "<resources><public type=\"be\" name=\"a\" id=\"0x7f010000\" /></resources>\n");
        Set<String> bad = Aapt2ResourceTypes.unsupportedIn(res);
        assertEquals("[ac, be, zz]", bad.toString());
        assertEquals("ac, be, zz", Aapt2ResourceTypes.describe(bad));
    }

    @Test
    public void missingOrEmptyProjectIsNotAFailure() throws IOException {
        assertTrue(Aapt2ResourceTypes.unsupportedIn(null).isEmpty());
        assertTrue(Aapt2ResourceTypes.unsupportedIn(new File(tmp.getRoot(), "nope")).isEmpty());
        assertTrue(Aapt2ResourceTypes.unsupportedIn(res()).isEmpty());
        assertEquals("", Aapt2ResourceTypes.describe(null));
    }

    /** A malformed ledger must not decide the tool; the compiler reports it. */
    @Test
    public void brokenLedgerDoesNotThrow() throws IOException {
        File res = res();
        mkdir(res, "values");
        write(new File(res, "values/public.xml"), "<resources><public type=\"ac\"");
        assertEquals(new HashSet<>(Arrays.asList("ac")), Aapt2ResourceTypes.unsupportedIn(res));
    }

    @Test
    public void longDescriptionIsTruncated() {
        Set<String> many = new java.util.TreeSet<>(
                Arrays.asList("a1", "a2", "a3", "a4", "a5", "a6", "a7"));
        assertEquals("a1, a2, a3, a4, a5, …", Aapt2ResourceTypes.describe(many));
    }

    /** Only this failure routes to the old AAPT; real errors still fail. */
    @Test
    public void recognisesOnlyResourceTypeFailures() {
        assertTrue(Aapt2ResourceTypes.isUnknownTypeFailure(
                "AAPT2 compile: res/values/acs.xml:3: error: unknown resource type 'ac'."));
        assertTrue(Aapt2ResourceTypes.isUnknownTypeFailure(
                "res/values/public.xml:1605: error: invalid resource type 'ac' in <public>."));
        assertFalse(Aapt2ResourceTypes.isUnknownTypeFailure(
                "error: resource drawable/icon not found"));
        assertFalse(Aapt2ResourceTypes.isUnknownTypeFailure("AAPT2 timed out"));
        assertFalse(Aapt2ResourceTypes.isUnknownTypeFailure(null));
    }
}
