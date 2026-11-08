package com.gmail.heagoo.apkeditor;

/**
 * Colours of the manifest code view.
 *
 * <p>The dark palette reproduces the editor look of MT Manager, which is the
 * IntelliJ Darcula scheme: neutral {@code #2B2B2B} writing surface, a lighter
 * {@code #37383A} line-number gutter, gold tags, a violet namespace prefix,
 * green attribute values and grey italic comments. The values were sampled from
 * a reference screenshot, so the code view no longer inherits the bluish app
 * chrome colour.
 *
 * <p>A light variant is kept so the eleven other app palettes stay usable; it
 * follows the matching IntelliJ Light scheme.
 *
 * <p>Pure Java on purpose: the palette can be unit-tested without Android.
 */
public final class ManifestEditorPalette {

    /** Writing surface behind the code. */
    public final int surface;
    /** Background of the line-number column. */
    public final int gutter;
    /** Line number digits. */
    public final int gutterText;
    /** Hairline separating the gutter from the code. */
    public final int gutterBorder;
    /** Vertical indent guides. */
    public final int indentGuide;
    /** Collapse/expand arrow. */
    public final int arrow;
    /** Tag names and angle brackets. */
    public final int tag;
    /** Attribute names without the namespace prefix. */
    public final int attribute;
    /** The {@code android:} part of an attribute name. */
    public final int namespace;
    /** Quoted attribute values and entities. */
    public final int value;
    /** Comments (also rendered italic). */
    public final int comment;
    /** Punctuation such as {@code =} and plain text. */
    public final int text;
    public final boolean dark;

    private ManifestEditorPalette(boolean dark, int surface, int gutter, int gutterText,
                                  int gutterBorder, int indentGuide, int arrow, int tag,
                                  int attribute, int namespace, int value, int comment, int text) {
        this.dark = dark;
        this.surface = surface;
        this.gutter = gutter;
        this.gutterText = gutterText;
        this.gutterBorder = gutterBorder;
        this.indentGuide = indentGuide;
        this.arrow = arrow;
        this.tag = tag;
        this.attribute = attribute;
        this.namespace = namespace;
        this.value = value;
        this.comment = comment;
        this.text = text;
    }

    /** Darcula, as used by MT Manager. */
    public static ManifestEditorPalette dark() {
        return new ManifestEditorPalette(true,
                0xFF2B2B2B, // surface
                0xFF37383A, // gutter
                0xFF888888, // gutterText
                0xFF454648, // gutterBorder
                0xFF414244, // indentGuide
                0xFF9A9A9A, // arrow
                0xFFE8BF6A, // tag
                0xFFBABABA, // attribute
                0xFF9876AA, // namespace
                0xFF6A8759, // value
                0xFF808080, // comment
                0xFFBABABA); // text
    }

    /** IntelliJ Light, for the light app palettes. */
    public static ManifestEditorPalette light() {
        return new ManifestEditorPalette(false,
                0xFFFFFFFF, // surface
                0xFFF2F2F2, // gutter
                0xFF808080, // gutterText
                0xFFD9D9D9, // gutterBorder
                0xFFE4E4E4, // indentGuide
                0xFF707070, // arrow
                0xFF0033B3, // tag
                0xFF080808, // attribute
                0xFF660E7A, // namespace
                0xFF008000, // value
                0xFF808080, // comment
                0xFF080808); // text
    }

    public static ManifestEditorPalette forDark(boolean dark) {
        return dark ? dark() : light();
    }

    /**
     * Colour of a {@link ManifestSyntax} span.
     *
     * @param first char at the span start, used to keep {@code =} neutral while
     *              angle brackets share the tag colour, as in MT Manager.
     */
    public int spanColor(int kind, char first) {
        switch (kind) {
            case ManifestSyntax.TAG:
                return tag;
            case ManifestSyntax.ATTRIBUTE:
                return attribute;
            case ManifestSyntax.VALUE:
                return value;
            case ManifestSyntax.COMMENT:
                return comment;
            case ManifestSyntax.SYMBOL:
                return first == '=' ? text : tag;
            default:
                return text;
        }
    }

    /** Every foreground colour must stay legible on the writing surface. */
    public boolean isReadable() {
        int[] foregrounds = {gutterText, tag, attribute, namespace, value, comment, text};
        for (int color : foregrounds) {
            if (contrast(color, surface) < 3.0d) {
                return false;
            }
        }
        return contrast(gutterText, gutter) >= 3.0d;
    }

    private static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05d) / (Math.min(la, lb) + 0.05d);
    }

    private static double luminance(int color) {
        double[] c = {((color >> 16) & 0xff) / 255.0d,
                ((color >> 8) & 0xff) / 255.0d,
                (color & 0xff) / 255.0d};
        for (int i = 0; i < c.length; i++) {
            c[i] = c[i] <= 0.03928d ? c[i] / 12.92d : Math.pow((c[i] + 0.055d) / 1.055d, 2.4d);
        }
        return 0.2126d * c[0] + 0.7152d * c[1] + 0.0722d * c[2];
    }
}
