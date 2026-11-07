package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.xmlPath));
            int index = 0;
            while (true) {
                String data = br.readLine();
                if (data == null) {
                    break;
                }
                LineRecord rec = new LineRecord(index, data, hanging);
                this.allXmlLines.add(rec);
                this.xmlLines.add(rec);
                index++;
            }
            br.close();
        } catch (Exception e) {
        }
        initXmlLines();
        refreshSyntax();
    }

    private void initXmlLines() {
        try {
            Stack<LineRecord> stack = new Stack<>();
            for (int i = 0; i < this.xmlLines.size(); i++) {
                LineRecord lr = this.xmlLines.get(i);
                if (lr.indent > 0) {
                    if (lr.lineData.endsWith("/>")) {
                        lr.sectionStart = lr.lineIndex;
                        lr.sectionEnd = lr.lineIndex;
                    } else if (lr.lineData.startsWith("</")) {
                        LineRecord startLine = stack.pop();
                        startLine.sectionEnd = lr.lineIndex;
                        lr.sectionStart = startLine.lineIndex;
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
        if (convertView == null) {
            convertView = LayoutInflater.from(this.activityRef.get()).inflate(this.layoutId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.collapseImage = (ImageView) convertView.findViewById(R.id.collapse_icon);
            viewHolder.lineData = (TextView) convertView.findViewById(R.id.line_data);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        int availableWidth = parent.getWidth();
        if (availableWidth <= 0) {
            availableWidth = parent.getMeasuredWidth();
        }
        if (availableWidth <= 0) {
            availableWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }
        int availableWidth2 = availableWidth - (parent.getPaddingLeft() + parent.getPaddingRight());
        if (rec.indent > 0) {
            viewHolder.collapseImage.setVisibility(0);
            viewHolder.collapseImage.setImageBitmap(getImage(rec, availableWidth2));
            availableWidth2 -= viewHolder.collapseImage.getDrawable().getIntrinsicWidth();
            setCollapsable(viewHolder.collapseImage, rec);
        } else {
            viewHolder.collapseImage.setVisibility(8);
        }
        viewHolder.lineData.setText(formatManifestLine(rec, viewHolder.lineData, availableWidth2 - (viewHolder.lineData.getCompoundPaddingLeft() + viewHolder.lineData.getCompoundPaddingRight())));
        return convertView;
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
        int color;
        SpannableString result = new SpannableString(rec.lineData);
        Activity activity = this.activityRef.get();
        if (activity == null) {
            return result;
        }
        TypedValue surface = new TypedValue();
        boolean light = true;
        if (activity.getTheme().resolveAttribute(R.attr.labSurface, surface, true)) {
            int color2 = surface.resourceId != 0 ? ContextCompat.getColor(activity, surface.resourceId) : surface.data;
            if (ColorUtils.calculateLuminance(color2) <= 0.45d) {
                light = false;
            }
        } else if (ColorUtils.calculateLuminance(view.getCurrentTextColor()) >= 0.45d) {
            light = false;
        }
        for (ManifestSyntax.Span span : rec.syntax) {
            switch (span.kind) {
                case 1:
                    if (light) {
                        color = -11189064;
                        break;
                    } else {
                        color = -4871937;
                        break;
                    }
                case 2:
                    if (light) {
                        color = -15246934;
                        break;
                    } else {
                        color = -7489281;
                        break;
                    }
                case 3:
                    if (light) {
                        color = -16222113;
                        break;
                    } else {
                        color = -8463168;
                        break;
                    }
                case 4:
                    if (light) {
                        color = -10916995;
                        break;
                    } else {
                        color = -7561035;
                        break;
                    }
                default:
                    if (light) {
                        color = -11443590;
                        break;
                    } else {
                        color = -5653551;
                        break;
                    }
            }
            if (span.start >= 0 && span.end <= result.length() && span.end > span.start) {
                result.setSpan(new ForegroundColorSpan(color), span.start, span.end, 33);
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

    private Bitmap getImage(LineRecord lineRec, int availableWidth) {
        Bitmap arrow;
        int maxIndent = Math.max(1, availableWidth / 96);
        int indent = Math.min(lineRec.indent, maxIndent);
        int width = indent * 48;
        Bitmap b = Bitmap.createBitmap(width, 48, Bitmap.Config.ALPHA_8);
        if (lineRec.sectionEnd != lineRec.lineIndex) {
            if (!lineRec.collapsed) {
                arrow = BitmapFactory.decodeResource(this.activityRef.get().getResources(), R.drawable.arrow_down);
            } else {
                arrow = BitmapFactory.decodeResource(this.activityRef.get().getResources(), R.drawable.arrow_right);
            }
            Canvas c = new Canvas(b);
            Paint paint = new Paint();
            c.drawBitmap(arrow, width - 40, 8.0f, paint);
        }
        return b;
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
        StringBuilder contentBuffer = new StringBuilder();
        synchronized (this.xmlLines) {
            for (int i = 0; i < this.allXmlLines.size(); i++) {
                LineRecord rec = this.allXmlLines.get(i);
                if (rec.lineIndex >= startLine && rec.lineIndex <= endLine) {
                    rec.deleted = true;
                }
                if (!rec.deleted) {
                    contentBuffer.append(rec.lineData);
                    contentBuffer.append('\n');
                }
            }
            updateDisplayLineData();
            refreshSyntax();
        }
        notifyDataSetChanged();
        if (this.callback != null) {
            this.callback.manifestChanged(contentBuffer.toString());
            return null;
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
            return !lineRec.lineData.contains("android.intent.action.MAIN");
        }
        if (!"category".equals(tag)) {
            return true;
        }
        boolean bContain = lineRec.lineData.contains("android.intent.category.LAUNCHER");
        return !bContain;
    }

    private boolean containMainAction(LineRecord lineRec) {
        for (int i = lineRec.sectionStart; i < lineRec.sectionEnd; i++) {
            LineRecord rec = this.allXmlLines.get(i);
            if ("action".equals(rec.getSectionTag()) && rec.lineData.contains("android.intent.action.MAIN")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void xmlLineChanged(int lineIndex, String newLine) {
        if (!isProVersion()) {
            showPromoteDialog(this.activityRef.get());
            return;
        }
        StringBuilder contentBuffer = new StringBuilder();
        synchronized (this) {
            for (LineRecord rec : this.allXmlLines) {
                if (rec.lineIndex == lineIndex) {
                    rec.lineData = newLine;
                }
                if (!rec.deleted) {
                    contentBuffer.append(rec.lineData);
                    contentBuffer.append('\n');
                }
            }
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
        if (this.callback != null) {
            this.callback.manifestChanged(contentBuffer.toString());
        }
    }

    @Override
    public void manifestChanged(String newContent) {
        if (!isProVersion()) {
            showPromoteDialog(this.activityRef.get());
        } else if (this.callback != null) {
            this.callback.manifestChanged(newContent);
        }
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
            XmlLineDialog dlg = new XmlLineDialog(this.activityRef.get(), this, lineRec.lineIndex, lineRec.lineData);
            dlg.show();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        LineRecord lineRec = null;
        try {
            lineRec = this.xmlLines.get(position);
        } catch (Exception e) {
        }
        ManifestLongClickDlg dlg = new ManifestLongClickDlg(this.activityRef.get(), this.xmlPath, lineRec, this);
        dlg.show();
        return true;
    }

    private static class ViewHolder {
        ImageView collapseImage;
        TextView lineData;

        private ViewHolder() {
        }
    }
}
