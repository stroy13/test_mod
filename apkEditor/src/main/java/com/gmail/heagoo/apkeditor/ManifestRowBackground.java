package com.gmail.heagoo.apkeditor;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * Writing surface of one manifest row: indent guides plus the collapse arrow.
 *
 * <p>Painted as the row background rather than inside the collapse icon, because
 * a ListView row is measured with an UNSPECIFIED height: a {@code match_parent}
 * child would not stretch to a wrapped row and the guides would come up short.
 * A background always covers the full row. Drawing here also avoids the old
 * per-row {@code Bitmap} allocation and its density scaling surprises.
 */
final class ManifestRowBackground extends Drawable {

    private final Paint fill = new Paint();
    private final Paint guide = new Paint();
    private final Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path triangle = new Path();
    private final float leftInset;
    private final float unit;
    private final float lineHeight;
    private final int indent;
    private final boolean collapsible;
    private final boolean collapsed;

    ManifestRowBackground(ManifestEditorPalette palette, float gutterWidth, float indentUnit,
                          int indent, float lineHeight, boolean collapsible, boolean collapsed) {
        this.fill.setColor(palette.surface);
        this.guide.setColor(palette.indentGuide);
        this.guide.setStrokeWidth(Math.max(1f, indentUnit / 24f));
        this.arrow.setColor(palette.arrow);
        this.leftInset = gutterWidth;
        this.unit = indentUnit;
        this.indent = Math.max(0, indent);
        this.lineHeight = lineHeight;
        this.collapsible = collapsible;
        this.collapsed = collapsed;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), fill);
        if (unit <= 0f) {
            return;
        }
        // One guide per enclosing level, as in MT Manager: a line indented by
        // one level has none, two levels have one, and so on.
        for (int i = 0; i < indent - 1; i++) {
            float x = leftInset + i * unit + unit / 2f;
            canvas.drawLine(x, getBounds().top, x, getBounds().bottom, guide);
        }
        if (!collapsible || indent <= 0) {
            return;
        }
        float cx = leftInset + (indent - 1) * unit + unit / 2f;
        float cy = getBounds().top + Math.max(lineHeight, unit) / 2f;
        float r = Math.min(unit, lineHeight) * 0.22f;
        triangle.reset();
        if (collapsed) {
            triangle.moveTo(cx - r * 0.6f, cy - r);
            triangle.lineTo(cx + r * 0.9f, cy);
            triangle.lineTo(cx - r * 0.6f, cy + r);
        } else {
            triangle.moveTo(cx - r, cy - r * 0.6f);
            triangle.lineTo(cx + r, cy - r * 0.6f);
            triangle.lineTo(cx, cy + r * 0.9f);
        }
        triangle.close();
        canvas.drawPath(triangle, arrow);
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        guide.setAlpha(alpha);
        arrow.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
        guide.setColorFilter(colorFilter);
        arrow.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
