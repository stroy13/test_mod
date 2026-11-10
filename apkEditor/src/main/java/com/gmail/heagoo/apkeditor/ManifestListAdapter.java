package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.gmail.heagoo.apkeditor.ManifestDisplayFormatter;
import com.gmail.heagoo.apkeditor.ManifestSyntax;
import com.gmail.heagoo.apkeditor.XmlLineDialog;
import com.gmail.heagoo.apkeditor.base.R;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

class ManifestListAdapter extends BaseAdapter implements IManifestChangeCallback, XmlLineDialog.IXmlLineChanged, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static final String hanging = "\t";
    private WeakReference<Activity> activityRef;
    private IManifestChangeCallback callback;
    private String xmlPath;
    private List<LineRecord> allXmlLines = new ArrayList();
    private final List<LineRecord> xmlLines = new ArrayList();
    private int layoutId = R.layout.item_manifestline;
    private ManifestEditorPalette palette;
    private int gutterWidth;

    ManifestListAdapter(Activity activity, String xmlPath, IManifestChangeCallback callback) {
        this.activityRef = new WeakReference<>(activity);
        this.callback = callback;
        this.xmlPath = xmlPath;
        initData();
    }

    public static void showPromoteDialog(final Context ctx) {
        new AlertDialog.Builder(ctx).setTitle(R.string.not_available).setMessage(R.string.promote_msg).setPositiveButton(R.string.view_pro_version, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialoginterface, int i) {
                ManifestListAdapter.viewProVersion(ctx);
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    public static void viewProVersion(Context ctx) {
        String pkgName = ctx.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent("android.intent.action.VIEW", uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=" + pkgName)));
        }
    }

    private void initData() {
        StringBuilder source = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(this.xmlPath));
            String data;
            while ((data = br.readLine()) != null) {
                source.append(data).append('\n');
            }
        } catch (Exception e) {
            // fall through with whatever was read
        } finally {
            closeQuietly(br);
        }
        // The decoder puts a tag and all its attributes on one physical line.
        // Split them so every attribute gets its own number, as in MT Manager.
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(source.toString());
        if (lines.isEmpty() && source.length() > 0) {
            // Never show an empty editor if the printer could not make sense of
            // the file: fall back to the raw physical lines.
            for (String raw : source.toString().split("\n")) {
                lines.add(new ManifestPrettyPrinter.Line(raw, 0, -1));
            }
        }
        int index = 0;
        for (ManifestPrettyPrinter.Line line : lines) {
            LineRecord rec = new LineRecord(index, line.text, hanging);
            rec.indent = line.indent;
            rec.owner = line.owner;
            rec.nonElement = line.nonElement;
            this.allXmlLines.add(rec);
            this.xmlLines.add(rec);
            index++;
        }
        initXmlLines();
        refreshSyntax();
    }

    private static void closeQuietly(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Rebuilds the file text. Indentation is restored here: {@link LineRecord}
     * strips it for display, and the old code wrote the stripped text back, so
     * any edit flattened the whole manifest to column zero.
     */
    private String buildContent() {
        StringBuilder sb = new StringBuilder();
        for (LineRecord rec : this.allXmlLines) {
            if (rec.deleted) {
                continue;
            }
            for (int i = 0; i < rec.indent; i++) {
                sb.append("    ");
            }
            sb.append(rec.lineData).append('\n');
        }
        return sb.toString();
    }

    private void initXmlLines() {
        try {
            Stack<LineRecord> stack = new Stack<>();
            for (int i = 0; i < this.xmlLines.size(); i++) {
                LineRecord lr = this.xmlLines.get(i);
                if (lr.nonElement) {
                    // A comment or the XML declaration is not an element. The
                    // old text-only checks pushed multi-line comments onto the
                    // section stack, which later threw EmptyStackException and
                    // flattened every indent in the file.
                    lr.sectionStart = lr.lineIndex;
                    lr.sectionEnd = lr.lineIndex;
                    continue;
                }
                if (lr.owner >= 0) {
                    // An attribute line is not an element: it must never be
                    // pushed as a section, or the stack desynchronises and every
                    // indent gets reset by the EmptyStackException handler.
                    LineRecord owner = this.allXmlLines.get(lr.owner);
                    lr.sectionStart = owner.lineIndex;
                    lr.sectionEnd = owner.lineIndex;
                    if (lr.lineData.endsWith("/>")) {
                        if (!stack.isEmpty() && stack.peek() == owner) {
                            stack.pop();
                        }
                        owner.sectionStart = owner.lineIndex;
                        owner.sectionEnd = lr.lineIndex;
                    }
                    continue;
                }
                if (lr.lineData.startsWith("<") && !lr.lineData.startsWith("</")
                        && lr.lineData.indexOf('>') < 0) {
                    // Start tag whose attributes follow on their own lines; the
                    // closing '>' arrives later, so just open the section.
                    lr.sectionStart = lr.lineIndex;
                    lr.sectionEnd = lr.lineIndex;
                    stack.push(lr);
                    continue;
                }
                // End tags must be handled at any indent: </manifest> sits at
                // column zero, and the old `indent > 0` guard skipped it, leaving
                // the root element permanently on the stack.
                if (lr.lineData.startsWith("</")) {
                    if (!stack.isEmpty()) {
                        LineRecord startLine = stack.pop();
                        startLine.sectionEnd = lr.lineIndex;
                        lr.sectionStart = startLine.lineIndex;
                        lr.sectionEnd = lr.lineIndex;
                    }
                    continue;
                }
                if (lr.indent > 0) {
                    if (lr.lineData.endsWith("/>")) {
                        lr.sectionStart = lr.lineIndex;
                        lr.sectionEnd = lr.lineIndex;
                    } else {
                        lr.sectionStart = lr.lineIndex;
                        stack.push(lr);
                    }
                }
            }
        } catch (EmptyStackException e) {
            for (int i2 = 0; i2 < this.xmlLines.size(); i2++) {
                this.xmlLines.get(i2).indent = 0;
            }
        }
    }

    @Override
    public int getCount() {
        return this.xmlLines.size();
    }

    @Override
    public Object getItem(int arg0) {
        return this.xmlLines.get(arg0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LineRecord rec;
        ViewHolder viewHolder;
        synchronized (this.xmlLines) {
            rec = this.xmlLines.get(position);
        }
        Activity activity = this.activityRef.get();
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(this.layoutId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.collapseImage = (ImageView) convertView.findViewById(R.id.collapse_icon);
            viewHolder.lineNumber = (TextView) convertView.findViewById(R.id.line_number);
            viewHolder.lineData = (TextView) convertView.findViewById(R.id.line_data);
            applyEditorTypeface(viewHolder);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ManifestEditorPalette colors = palette(activity, viewHolder.lineData);
        int availableWidth = parent.getWidth();
        if (availableWidth <= 0) {
            availableWidth = parent.getMeasuredWidth();
        }
        if (availableWidth <= 0) {
            availableWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }
        int availableWidth2 = availableWidth - (parent.getPaddingLeft() + parent.getPaddingRight());

        // Fixed-width line-number gutter, so digits never shift the code column.
        int gutter = gutterWidth(viewHolder.lineNumber);
        if (viewHolder.lineNumber != null) {
            viewHolder.lineNumber.setWidth(gutter);
            viewHolder.lineNumber.setTextColor(colors.gutterText);
            viewHolder.lineNumber.setText(String.valueOf(rec.lineIndex + 1));
        }
        availableWidth2 -= gutter;

        // Indent guides and the collapse arrow are painted by the row background;
        // the icon is only a click target sized to the indent.
        float unit = viewHolder.lineData.getPaint().measureText("    ");
        float lineHeight = viewHolder.lineData.getPaint().getFontSpacing();
        int maxIndent = unit > 0.0f ? Math.max(1, (int) (availableWidth2 / 3.0f / unit)) : 0;
        int indent = Math.min(rec.indent, maxIndent);
        int indentWidth = Math.round(unit * indent);
        if (indentWidth > 0) {
            viewHolder.collapseImage.setVisibility(0);
            viewHolder.collapseImage.setImageDrawable(null);
            ViewGroup.LayoutParams lp = viewHolder.collapseImage.getLayoutParams();
            lp.width = indentWidth;
            lp.height = Math.max(1, Math.round(lineHeight));
            viewHolder.collapseImage.setLayoutParams(lp);
            setCollapsable(viewHolder.collapseImage, rec);
        } else {
            viewHolder.collapseImage.setVisibility(8);
        }
        availableWidth2 -= indentWidth;
        convertView.setBackground(new ManifestRowBackground(colors, gutter, unit, indent,
                lineHeight, rec.sectionEnd != rec.lineIndex, rec.collapsed));
        viewHolder.lineData.setTextColor(colors.text);
        viewHolder.lineData.setText(formatManifestLine(rec, viewHolder.lineData, availableWidth2 - (viewHolder.lineData.getCompoundPaddingLeft() + viewHolder.lineData.getCompoundPaddingRight())));
        return convertView;
    }

    /** Writing surface colour, so the list background matches the rows. */
    int editorSurface() {
        return palette(this.activityRef.get(), null).surface;
    }

    /** Palette of the code view, for the header bar above the list. */
    ManifestEditorPalette editorPalette() {
        return palette(this.activityRef.get(), null);
    }

    /** Monospace, dense rows -- the code view reads like MT Manager's editor. */
    private static void applyEditorTypeface(ViewHolder holder) {
        holder.lineData.setTypeface(Typeface.MONOSPACE);
        holder.lineData.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        holder.lineData.setIncludeFontPadding(false);
        if (holder.lineNumber != null) {
            holder.lineNumber.setTypeface(Typeface.MONOSPACE);
            holder.lineNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
            holder.lineNumber.setIncludeFontPadding(false);
        }
    }

    private int gutterWidth(TextView gutter) {
        if (gutter == null) {
            return 0;
        }
        Activity activity = this.activityRef.get();
        float density = activity == null ? 1.0f : activity.getResources().getDisplayMetrics().density;
        // Padding is per-view: recycled rows keep it, but every freshly inflated
        // row needs it applied too, otherwise its digits sit flush against the code.
        gutter.setPadding(Math.round(6.0f * density), gutter.getPaddingTop(),
                Math.round(10.0f * density), gutter.getPaddingBottom());
        if (this.gutterWidth <= 0) {
            int digits = Math.max(2, String.valueOf(Math.max(1, this.allXmlLines.size())).length());
            this.gutterWidth = Math.round(gutter.getPaint().measureText("0") * digits)
                    + Math.round(16.0f * density);
        }
        return this.gutterWidth;
    }

    /**
     * The code view keeps its own editor surface instead of inheriting the app
     * chrome colour, exactly as MT Manager does. Light app palettes get the
     * matching light editor scheme.
     */
    private ManifestEditorPalette palette(Activity activity, TextView view) {
        if (this.palette == null) {
            boolean dark = true;
            TypedValue surface = new TypedValue();
            if (activity != null && activity.getTheme().resolveAttribute(R.attr.labSurface, surface, true)) {
                int color = surface.resourceId != 0
                        ? ContextCompat.getColor(activity, surface.resourceId) : surface.data;
                dark = ColorUtils.calculateLuminance(color) <= 0.45d;
            } else if (view != null) {
                dark = ColorUtils.calculateLuminance(view.getCurrentTextColor()) <= 0.45d;
            }
            this.palette = ManifestEditorPalette.forDark(dark);
        }
        return this.palette;
    }

    private CharSequence formatManifestLine(LineRecord rec, final TextView view, int availableWidth) {
        CharSequence colored = highlightManifestLine(rec, view);
        ManifestDisplayFormatter.Plan plan = ManifestDisplayFormatter.plan(rec.lineData, rec.syntax, Math.max(1, availableWidth - 2), new ManifestDisplayFormatter.Width() {
            @Override
            public final float measure(String str, int i, int i2) {
                float measureText;
                measureText = view.getPaint().measureText(str, i, i2);
                return measureText;
            }
        });
        SpannableStringBuilder display = new SpannableStringBuilder(colored);
        Iterator<Integer> it = plan.noBreakSpaces.iterator();
        while (it.hasNext()) {
            int i = it.next().intValue();
            display.replace(i, i + 1, (CharSequence) " ");
        }
        Collections.sort(plan.insertions, new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                int compare;
                compare = Integer.compare(((ManifestDisplayFormatter.Insertion) obj2).offset, ((ManifestDisplayFormatter.Insertion) obj).offset);
                return compare;
            }
        });
        for (ManifestDisplayFormatter.Insertion insert : plan.insertions) {
            display.insert(insert.offset, (CharSequence) insert.text);
        }
        return display;
    }

    private void refreshSyntax() {
        ManifestSyntax.State state = new ManifestSyntax.State();
        for (LineRecord rec : this.allXmlLines) {
            rec.syntax = rec.deleted ? Collections.emptyList() : ManifestSyntax.tokenize(rec.lineData, state);
        }
    }

    private CharSequence highlightManifestLine(LineRecord rec, TextView view) {
        SpannableString result = new SpannableString(rec.lineData);
        ManifestEditorPalette colors = palette(this.activityRef.get(), view);
        for (ManifestSyntax.Span span : rec.syntax) {
            if (span.start < 0 || span.end > result.length() || span.end <= span.start) {
                continue;
            }
            if (span.kind == ManifestSyntax.ATTRIBUTE) {
                // MT Manager paints the namespace, not merely the text before the
                // colon: "android:name" -> violet "android", plain "name", while
                // "xmlns:android" -> plain "xmlns", violet "android".
                int colon = rec.lineData.indexOf(':', span.start);
                if (colon > span.start && colon < span.end - 1) {
                    boolean declaration = rec.lineData.startsWith("xmlns", span.start);
                    int prefixColor = declaration ? colors.attribute : colors.namespace;
                    int localColor = declaration ? colors.namespace : colors.attribute;
                    result.setSpan(new ForegroundColorSpan(prefixColor), span.start, colon, 33);
                    result.setSpan(new ForegroundColorSpan(localColor), colon + 1, span.end, 33);
                    continue;
                }
            }
            char first = rec.lineData.charAt(span.start);
            result.setSpan(new ForegroundColorSpan(colors.spanColor(span.kind, first)),
                    span.start, span.end, 33);
            if (span.kind == ManifestSyntax.COMMENT) {
                result.setSpan(new StyleSpan(Typeface.ITALIC), span.start, span.end, 33);
            }
        }
        return result;
    }

    private void setCollapsable(ImageView collapseImage, final LineRecord lineRec) {
        collapseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManifestListAdapter.this.collapseOrExpand(lineRec);
            }
        });
    }

    public void collapseOrExpand(LineRecord lineRec) {
        synchronized (this.xmlLines) {
            lineRec.collapsed = !lineRec.collapsed;
            updateDisplayLineData();
        }
        notifyDataSetChanged();
    }

    private void updateDisplayLineData() {
        this.xmlLines.clear();
        int i = 0;
        while (i < this.allXmlLines.size()) {
            LineRecord rec = this.allXmlLines.get(i);
            if (!rec.deleted) {
                this.xmlLines.add(rec);
                if (rec.collapsed && rec.sectionEnd > i) {
                    i = rec.sectionEnd;
                }
            }
            i++;
        }
    }

    @Override
    public String tryToDeleteSection(LineRecord lineRec) {
        if (!isSectionDeletable(lineRec)) {
            return this.activityRef.get().getResources().getString(R.string.section_undeletable);
        }
        if (!isProVersion()) {
            showPromoteDialog(this.activityRef.get());
            return "";
        }
        int startLine = lineRec.sectionStart;
        int endLine = lineRec.sectionEnd;
        String content;
        synchronized (this.xmlLines) {
            for (int i = 0; i < this.allXmlLines.size(); i++) {
                LineRecord rec = this.allXmlLines.get(i);
                if (rec.lineIndex >= startLine && rec.lineIndex <= endLine) {
                    rec.deleted = true;
                }
            }
            content = buildContent();
            updateDisplayLineData();
            refreshSyntax();
        }
        notifyDataSetChanged();
        if (this.callback != null) {
            if (!this.callback.manifestChanged(content)) {
                reload();
                return this.activityRef.get().getString(R.string.lab_manifest_save_failed);
            }
            reload();
        }
        return null;
    }

    private boolean isProVersion() {
        return true;
    }

    private boolean isSectionDeletable(LineRecord lineRec) {
        String tag = lineRec.getSectionTag();
        if ("manifest".equals(tag) || "application".equals(tag)) {
            return false;
        }
        if ("activity".equals(tag) || "intent-filter".equals(tag)) {
            return !containMainAction(lineRec);
        }
        if ("action".equals(tag)) {
            return !sectionContains(lineRec, "android.intent.action.MAIN");
        }
        if ("category".equals(tag)) {
            return !sectionContains(lineRec, "android.intent.category.LAUNCHER");
        }
        return true;
    }

    private boolean containMainAction(LineRecord lineRec) {
        return sectionContains(lineRec, "android.intent.action.MAIN");
    }

    private boolean sectionContains(LineRecord lineRec, String value) {
        int start = Math.max(0, lineRec.sectionStart);
        int end = Math.min(this.allXmlLines.size() - 1, lineRec.sectionEnd);
        for (int i = start; i <= end; i++) {
            LineRecord rec = this.allXmlLines.get(i);
            if (!rec.deleted && rec.lineData.contains(value)) return true;
        }
        return false;
    }

    @Override
    public void xmlLineChanged(int lineIndex, String newLine) {
        applyLineChange(lineIndex, newLine);
    }

    private boolean applyLineChange(int lineIndex, String newLine) {
        if (!isProVersion()) {
            showPromoteDialog(this.activityRef.get());
            return false;
        }
        String content;
        synchronized (this) {
            for (LineRecord rec : this.allXmlLines) {
                if (rec.lineIndex == lineIndex) {
                    rec.lineData = newLine;
                }
            }
            content = buildContent();
            Iterator<LineRecord> it = this.xmlLines.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                LineRecord rec2 = it.next();
                if (rec2.lineIndex == lineIndex) {
                    rec2.lineData = newLine;
                    break;
                }
            }
        }
        synchronized (this.xmlLines) {
            refreshSyntax();
        }
        notifyDataSetChanged();
        boolean saved = this.callback != null && this.callback.manifestChanged(content);
        // Rebuild section boundaries after tag edits and restore from disk on failure.
        reload();
        return saved;
    }

    @Override
    public boolean manifestChanged(String newContent) {
        if (!isProVersion()) {
            showPromoteDialog(this.activityRef.get());
            return false;
        }
        return this.callback != null && this.callback.manifestChanged(newContent);
    }

    void reload() {
        synchronized (this.xmlLines) {
            this.allXmlLines.clear();
            this.xmlLines.clear();
            initData();
        }
        notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        LineRecord lineRec = null;
        try {
            lineRec = this.xmlLines.get(position);
        } catch (Exception e) {
        }
        if (lineRec != null) {
            final Activity activity = this.activityRef.get();
            if (activity == null) return;
            final int index = lineRec.lineIndex;
            final EditText editor = new EditText(activity);
            editor.setText(lineRec.lineData);
            editor.setSingleLine(false);
            editor.setTypeface(Typeface.MONOSPACE);
            editor.setSelection(editor.length());
            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(R.string.lab_manifest_edit_line)
                    .setView(editor)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.save, null)
                    .create();
            dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        if (applyLineChange(index, editor.getText().toString())) dialog.dismiss();
                    }));
            dialog.show();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        LineRecord lineRec = null;
        try {
            lineRec = this.xmlLines.get(position);
        } catch (Exception e) {
        }
        if (lineRec != null && lineRec.owner >= 0) {
            // Long-pressing an attribute must offer the whole element, otherwise
            // "delete section" would remove only the tag line and orphan its
            // attributes.
            lineRec = this.allXmlLines.get(lineRec.owner);
        }
        ManifestLongClickDlg dlg = new ManifestLongClickDlg(this.activityRef.get(), this.xmlPath, lineRec, this);
        dlg.show();
        return true;
    }

    private static class ViewHolder {
        ImageView collapseImage;
        TextView lineNumber;
        TextView lineData;

        private ViewHolder() {
        }
    }
}
