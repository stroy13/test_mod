package brut.androlib.res.data.value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Types for which a synthetic missing-ID value must not be boolean false. */
public final class DummyResourceTypes {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "anim", "animator", "array", "attr", "color", "dimen", "drawable", "font",
            "fraction", "integer", "interpolator", "layout", "menu", "mipmap", "navigation",
            "plurals", "raw", "style", "transition", "xml"));
    private DummyResourceTypes() { }
    public static boolean needsNullReference(String type) { return TYPES.contains(type); }
}
