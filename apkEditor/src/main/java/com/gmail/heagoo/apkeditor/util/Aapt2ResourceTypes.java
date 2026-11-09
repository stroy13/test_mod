package com.gmail.heagoo.apkeditor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * The resource types AAPT2 is willing to compile.
 *
 * <p>AAPT2 resolves a type name through a fixed table ({@code ParseResourceType}
 * in {@code tools/aapt2/Resource.cpp}); anything outside it is a hard error:
 *
 * <pre>
 *   res/values/acs.xml:3: error: unknown resource type 'ac'.
 *   res/values/public.xml:1605: error: invalid resource type 'ac' in &lt;public&gt;.
 * </pre>
 *
 * <p>Obfuscated packages routinely carry shortened type names such as {@code ac}
 * -- resource shrinkers rewrite them to save bytes. The original AAPT accepts
 * them, because it creates a type on first use instead of looking it up in a
 * table, which is why the same project builds with one tool and not the other.
 *
 * <p>So the type names are checked before AAPT2 is started, and a project that
 * cannot possibly compile is routed to the original AAPT with an explanation
 * rather than being failed several minutes into the build.
 *
 * <p>Pure Java so the table and the scan can be unit-tested.
 */
public final class Aapt2ResourceTypes {

    /**
     * Every key of {@code sResourceTypeMap} in AOSP. {@code ^attr-private} is
     * part of the table but is never a directory or a values tag on disk.
     */
    private static final Set<String> SUPPORTED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "anim", "animator", "array", "attr", "^attr-private", "bool",
                    "color", "configVarying", "dimen", "drawable", "font",
                    "fraction", "id", "integer", "interpolator", "layout",
                    "macro", "menu", "mipmap", "navigation", "plurals", "raw",
                    "string", "style", "styleable", "transition", "xml")));

    /** Values folders hold every type, so they carry no type of their own. */
    private static final String VALUES_PREFIX = "values";

    private Aapt2ResourceTypes() {
    }

    /** True when AAPT2 knows this type name. */
    public static boolean isSupported(String type) {
        return type != null && SUPPORTED.contains(type);
    }

    /**
     * True when AAPT2 accepts a {@code type="…"} written in a values file.
     *
     * <p>{@code ParseResourceNamedType} allows a qualified spelling such as
     * {@code string.foo}: the part before the first dot is looked up and the
     * rest is kept as a label. A trailing dot is not a qualifier.
     */
    public static boolean isSupportedValueType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        int dot = type.indexOf('.');
        if (dot > 0 && dot < type.length() - 1) {
            return isSupported(type.substring(0, dot));
        }
        return isSupported(type);
    }

    /**
     * True when a resource directory name is one AAPT2 can compile.
     *
     * <p>{@code drawable-hdpi} and {@code values-ru} are qualified; the type is
     * the part before the first dash. {@code valuesfoo} is a different type,
     * not a qualified values directory.
     */
    public static boolean isSupportedDirectory(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String type = directoryType(name);
        return type.equals(VALUES_PREFIX) || isSupported(type);
    }

    /** The type portion of a resource directory name. */
    public static String directoryType(String name) {
        if (name == null) {
            return "";
        }
        int dash = name.indexOf('-');
        return dash < 0 ? name : name.substring(0, dash);
    }

    /**
     * Type names in a decoded {@code res} folder that AAPT2 would reject.
     *
     * <p>Two signals are combined. Directory names cover file-backed types.
     * A renamed type that only ever held values has no directory at all, so the
     * {@code public.xml} ledgers are scanned as well -- they name the type of
     * every public resource, which is exactly where AAPT2 reported
     * {@code invalid resource type 'ac' in <public>}.
     *
     * @return sorted, de-duplicated offending type names; empty when AAPT2 is safe
     */
    public static Set<String> unsupportedIn(File resDir) {
        Set<String> bad = new TreeSet<>();
        if (resDir == null || !resDir.isDirectory()) {
            return bad;
        }
        File[] children = resDir.listFiles();
        if (children == null) {
            return bad;
        }
        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }
            String name = child.getName();
            if (!isSupportedDirectory(name)) {
                bad.add(directoryType(name));
            } else if (directoryType(child.getName()).equals(VALUES_PREFIX)) {
                collectFromPublicXml(new File(child, "public.xml"), bad);
            }
        }
        return bad;
    }

    /** At most this many bytes of a ledger are read; the scan is a hint, not a parser. */
    private static final long MAX_LEDGER_BYTES = 8L * 1024 * 1024;

    /**
     * Pulls {@code type="…"} values out of a {@code public.xml}.
     *
     * <p>Deliberately a tolerant text scan rather than an XML parse: the ledger
     * can be megabytes long, and a project that is malformed elsewhere must
     * still reach the compiler and produce a real diagnostic.
     */
    private static void collectFromPublicXml(File ledger, Set<String> bad) {
        if (!ledger.isFile() || ledger.length() > MAX_LEDGER_BYTES) {
            return;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(ledger), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                int at = 0;
                while ((at = line.indexOf(TYPE_ATTR, at)) >= 0) {
                    int start = at + TYPE_ATTR.length();
                    int end = line.indexOf('"', start);
                    if (end > start) {
                        String type = line.substring(start, end);
                        if (!isSupportedValueType(type)) {
                            bad.add(type);
                        }
                    }
                    at = end > start ? end + 1 : start;
                }
            }
        } catch (IOException e) {
            // A missing or unreadable ledger simply leaves the directory signal.
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing to recover.
                }
            }
        }
    }

    private static final String TYPE_ATTR = "type=\"";

    /**
     * True when an AAPT2 failure was caused by a type it does not know, as
     * opposed to a genuine error in the project. Used to recognise the case even
     * when the offending type only appears inside a values file.
     */
    public static boolean isUnknownTypeFailure(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("unknown resource type")
                || lower.contains("invalid resource type");
    }

    /** A short, human-readable list for the build log, e.g. {@code ac, be}. */
    public static String describe(Set<String> types) {
        if (types == null || types.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String type : types) {
            if (shown == 5) {
                sb.append(", …");
                break;
            }
            if (shown > 0) {
                sb.append(", ");
            }
            sb.append(type);
            shown++;
        }
        return sb.toString();
    }
}
