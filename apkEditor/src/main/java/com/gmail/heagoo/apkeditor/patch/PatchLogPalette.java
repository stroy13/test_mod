package com.gmail.heagoo.apkeditor.patch;

/**
 * Colours of the patch log.
 *
 * <p>The log used to be two-tone: every heading in the accent colour and every
 * body line in the default text colour, so a long run looked flat and there was
 * nothing to tell a rewritten file from a phase change or a warning. Each kind of
 * line now has its own hue, and a file entry is coloured in parts: the name
 * stands out, the count reads as a success, the directory recedes.
 *
 * <p>Pure Java so the palette and its contrast can be unit-tested.
 */
public final class PatchLogPalette {

    /** Plain narrative text. */
    public static final int PLAIN = 0;
    /** "Applying rule at line N" -- the rule currently running. */
    public static final int RULE = 1;
    /** A failure. */
    public static final int ERROR = 2;
    /** "All rules applied." */
    public static final int DONE = 3;
    /** A structured per-file entry, coloured in parts. */
    public static final int FILE_ENTRY = 4;
    /** Phase notices such as "Parsing patch.txt" or "Decoding DEX". */
    public static final int PHASE = 5;

    public final int text;
    public final int rule;
    public final int phase;
    public final int error;
    public final int done;
    public final int fileName;
    public final int count;
    public final int directory;
    public final int bullet;
    public final boolean dark;

    private PatchLogPalette(boolean dark, int text, int rule, int phase, int error, int done,
                            int fileName, int count, int directory, int bullet) {
        this.dark = dark;
        this.text = text;
        this.rule = rule;
        this.phase = phase;
        this.error = error;
        this.done = done;
        this.fileName = fileName;
        this.count = count;
        this.directory = directory;
        this.bullet = bullet;
    }

    public static PatchLogPalette dark() {
        return new PatchLogPalette(true,
                0xFFD6DEEB, // text
                0xFFC792EA, // rule     -- violet
                0xFFFFCB6B, // phase    -- amber
                0xFFFF8793, // error    -- red
                0xFF7CE1A7, // done     -- green
                0xFF82E9FF, // fileName -- cyan
                0xFF9EDE7B, // count    -- light green
                0xFF8A99AD, // directory-- muted slate
                0xFFFFCB6B); // bullet  -- amber
    }

    public static PatchLogPalette light() {
        return new PatchLogPalette(false,
                0xFF1F2733, // text
                0xFF7A30B0, // rule
                0xFF8A5A00, // phase
                0xFFB32638, // error
                0xFF167044, // done
                0xFF00617A, // fileName
                0xFF2C6E1E, // count
                0xFF5A6675, // directory
                0xFF8A5A00); // bullet
    }

    public static PatchLogPalette forDark(boolean dark) {
        return dark ? dark() : light();
    }

    /** Colour of a whole-line style; {@link #FILE_ENTRY} is handled per part. */
    public int lineColor(int style) {
        switch (style) {
            case RULE:
                return rule;
            case ERROR:
                return error;
            case DONE:
                return done;
            case PHASE:
                return phase;
            default:
                return text;
        }
    }

    /** Styles rendered bold. */
    public static boolean isBold(int style) {
        return style == RULE || style == ERROR || style == DONE || style == PHASE;
    }

    /**
     * Every hue must stay readable on the dialog surface it is drawn on.
     *
     * @param surface the resolved dialog background
     */
    public boolean isReadableOn(int surface) {
        int[] colors = {text, rule, phase, error, done, fileName, count, directory, bullet};
        for (int color : colors) {
            if (contrast(color, surface) < 3.0d) {
                return false;
            }
        }
        return true;
    }

    public static double contrast(int a, int b) {
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
