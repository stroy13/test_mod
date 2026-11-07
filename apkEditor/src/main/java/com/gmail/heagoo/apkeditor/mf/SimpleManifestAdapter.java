package com.gmail.heagoo.apkeditor.mf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

public class SimpleManifestAdapter extends BaseAdapter implements ISectionDeleteCallback {
    private static final String hanging = "\t";
    private ISectionDeleteCallback callback;
    private Activity ctx;
    private List<LineRecord> allXmlLines = new ArrayList();
    private List<LineRecord> xmlLines = new ArrayList();

    public SimpleManifestAdapter(Activity ctx, String xmlPath, ISectionDeleteCallback callback) {
        this.ctx = ctx;
        this.callback = callback;
        try {
            BufferedReader br = new BufferedReader(new FileReader(xmlPath));
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
            Toast.makeText(this.ctx, "Manifest parse error.", 0).show();
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
        ViewHolder viewHolder;
        LineRecord rec = this.xmlLines.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(this.ctx).inflate(R.layout.item_manifestline, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.collapseImage = (ImageView) convertView.findViewById(R.id.collapse_icon);
            viewHolder.lineData = (TextView) convertView.findViewById(R.id.line_data);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.lineData.setText(rec.lineData);
        setContentClickable(viewHolder.lineData, rec);
        if (rec.indent > 0) {
            viewHolder.collapseImage.setVisibility(0);
            viewHolder.collapseImage.setImageBitmap(getImage(rec));
            setCollapsable(viewHolder.collapseImage, rec);
        } else {
            viewHolder.collapseImage.setVisibility(8);
        }
        return convertView;
    }

    private void setContentClickable(TextView lineData, LineRecord lineRec) {
        lineData.setClickable(true);
        lineData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    private void setCollapsable(ImageView collapseImage, final LineRecord lineRec) {
        collapseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleManifestAdapter.this.collapseOrExpand(lineRec);
            }
        });
    }

    protected void collapseOrExpand(LineRecord lineRec) {
        synchronized (this) {
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
                if (rec.collapsed) {
                    i = rec.sectionEnd;
                }
            }
            i++;
        }
    }

    private Bitmap getImage(LineRecord lineRec) {
        Bitmap arrow;
        int indent = lineRec.indent;
        int width = indent * 48;
        Bitmap b = Bitmap.createBitmap(width, 48, Bitmap.Config.ALPHA_8);
        if (lineRec.sectionEnd != lineRec.lineIndex) {
            if (!lineRec.collapsed) {
                arrow = BitmapFactory.decodeResource(this.ctx.getResources(), R.drawable.arrow_down);
            } else {
                arrow = BitmapFactory.decodeResource(this.ctx.getResources(), R.drawable.arrow_right);
            }
            Canvas c = new Canvas(b);
            Paint paint = new Paint();
            c.drawBitmap(arrow, width - 40, 8.0f, paint);
        }
        return b;
    }

    @Override
    public boolean sectionDeleted(LineRecord lineRec) {
        if (!isSectionDeletable(lineRec) || !this.callback.sectionDeleted(lineRec)) {
            return false;
        }
        int startLine = lineRec.sectionStart;
        int endLine = lineRec.sectionEnd;
        synchronized (this) {
            for (int i = 0; i < this.allXmlLines.size(); i++) {
                LineRecord rec = this.allXmlLines.get(i);
                if (rec.lineIndex >= startLine && rec.lineIndex <= endLine) {
                    rec.deleted = true;
                }
            }
            updateDisplayLineData();
        }
        notifyDataSetChanged();
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
    public String getUndeletableReason() {
        return null;
    }

    private static class ViewHolder {
        public ImageView collapseImage;
        public TextView lineData;

        private ViewHolder() {
        }
    }
}
