package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.preference.PreferenceManager;
import com.gmail.heagoo.common.ThemeRegistry;

public class ColorTheme {
    private static final int COMMENT = 6;
    private static final int DIGIT = 2;
    private static final int FUNCTION = 8;
    private static final int INVALID = 7;
    private static final int KEYWORD = 1;
    private static final int LABEL = 4;
    private static final int LITERAL = 3;
    private static final int MARKUP = 5;
    private static final int NORMAL = 0;
    private static final int OPERATOR = 9;
    private static final int TYPE = 10;
    private final int backgroundColor;
    private final int currentLineColor;
    private final int dividerColor;
    private final int[] foregroundColors;
    private final int gutterColor;
    private final int indentGuideColor;
    private final int lineNumberColor;

    public ColorTheme(Context context) {
        this(context, null);
    }

    public ColorTheme(Context context, String syntaxFileName) {
        int i;
        int i2;
        this.foregroundColors = new int[11];
        int themeId = 0;
        try {
            themeId = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("AppTheme", "0"));
        } catch (Exception e) {
        }
        boolean dark = ThemeRegistry.isDark(themeId);
        boolean smali = "smali.xml".equals(syntaxFileName);
        boolean xml = "xml.xml".equals(syntaxFileName) || "html.xml".equals(syntaxFileName);
        this.backgroundColor = dark ? -14671580 : -328966;
        this.gutterColor = dark ? -15000546 : -986638;
        this.lineNumberColor = dark ? -8025197 : -9538694;
        this.dividerColor = dark ? -13289669 : -2630946;
        this.currentLineColor = dark ? -14079185 : -1446156;
        this.indentGuideColor = dark ? -13092032 : -2301465;
        this.foregroundColors[0] = dark ? -2564375 : -14407377;
        int[] iArr = this.foregroundColors;
        int i3 = -8236833;
        if (smali) {
            i = dark ? -4929561 : -12167000;
        } else {
            i = dark ? -3832128 : -8236833;
        }
        iArr[1] = i;
        this.foregroundColors[2] = dark ? -3040666 : -6998016;
        this.foregroundColors[3] = dark ? -5716342 : -15637719;
        int[] iArr2 = this.foregroundColors;
        if (xml) {
            if (dark) {
                i3 = -3628312;
            }
        } else {
            i3 = dark ? -7554873 : -16160294;
        }
        iArr2[4] = i3;
        int[] iArr3 = this.foregroundColors;
        if (smali) {
            i2 = dark ? -3697942 : -6025606;
        } else {
            i2 = dark ? -1720197 : -6658304;
        }
        iArr3[5] = i2;
        this.foregroundColors[COMMENT] = dark ? -8420210 : -9537663;
        this.foregroundColors[INVALID] = dark ? -881790 : -3202514;
        this.foregroundColors[8] = dark ? -8213761 : -16428882;
        this.foregroundColors[OPERATOR] = this.foregroundColors[0];
        this.foregroundColors[TYPE] = dark ? -4866712 : -10061281;
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public int getGutterColor() {
        return this.gutterColor;
    }

    public int getLineNumberColor() {
        return this.lineNumberColor;
    }

    public int getDividerColor() {
        return this.dividerColor;
    }

    public int getCurrentLineColor() {
        return this.currentLineColor;
    }

    public int getIndentGuideColor() {
        return this.indentGuideColor;
    }

    public int getStatusSyntaxColor() {
        return this.foregroundColors[8];
    }

    public int getForegroundColor(Token token) {
        switch (token.id) {
            case 1:
            case 2:
            case 3:
            case 4:
                return this.foregroundColors[COMMENT];
            case 5:
                return this.foregroundColors[2];
            case COMMENT:
                return this.foregroundColors[8];
            case INVALID:
                return this.foregroundColors[INVALID];
            case 8:
            case OPERATOR:
            case 11:
                return this.foregroundColors[1];
            case TYPE:
                return this.foregroundColors[TYPE];
            case 12:
                return this.foregroundColors[4];
            case 13:
            case 16:
                return this.foregroundColors[3];
            case 14:
            case 15:
                return this.foregroundColors[TYPE];
            case 17:
                return this.foregroundColors[5];
            case 18:
                return this.foregroundColors[OPERATOR];
            default:
                return this.foregroundColors[0];
        }
    }

    public int getForeground() {
        return this.foregroundColors[0];
    }
}
