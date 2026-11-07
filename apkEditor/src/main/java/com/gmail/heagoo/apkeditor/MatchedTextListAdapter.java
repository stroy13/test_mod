package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.ac.EditTextWithTip;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.neweditor.Encoding;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MatchedTextListAdapter extends BaseExpandableListAdapter implements View.OnClickListener {
    private WeakReference<ApkInfoActivity> activityRef;
    private boolean[] editClicked;
    private boolean isDark;
    private String keyword;
    private int keywordWidth;
    private WeakReference<ExpandableListView> listviewRef;
    private String pathPrefix;
    private boolean[] replaceClicked;
    private int themeId;
    private int lineTotalWidth = 0;
    private int titleWidth = 0;
    private boolean notShowReplaceDlg = false;
    private String strReplace = null;
    private ArrayList<String> filePathList = new ArrayList<>();
    private final Map<String, List<MatchedLineItem>> matchedContents = new HashMap();

    MatchedTextListAdapter(WeakReference<ApkInfoActivity> activityRef, ExpandableListView listView, String pathPrefix, List<String> fileList, String keyword) {
        this.activityRef = activityRef;
        this.listviewRef = new WeakReference<>(listView);
        this.pathPrefix = pathPrefix + "/";
        this.keyword = keyword;
        this.themeId = GlobalConfig.instance((Context) activityRef.get()).getThemeId();
        this.isDark = GlobalConfig.instance((Context) activityRef.get()).isDarkTheme();
        for (String filePath : fileList) {
            this.filePathList.add(filePath);
        }
        this.replaceClicked = new boolean[fileList.size()];
        this.editClicked = new boolean[fileList.size()];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        String filePath = this.filePathList.get(groupPosition);
        List<MatchedLineItem> item = this.matchedContents.get(filePath);
        if (item != null && childPosition < item.size()) {
            return item.get(childPosition);
        }
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return (65536 * groupPosition) + childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View convertView2;
        String lineRec;
        MatchedLineItem matchedLine = (MatchedLineItem) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.activityRef.get().getSystemService("layout_inflater");
            int resId = R.layout.item_matchedline;
            convertView2 = layoutInflater.inflate(resId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.matchedLine = (TextView) convertView2.findViewById(R.id.tv_line);
            convertView2.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            convertView2 = convertView;
        }
        if (matchedLine != null) {
            Paint paint = new Paint();
            paint.setTextSize(viewHolder.matchedLine.getTextSize());
            if (this.lineTotalWidth == 0) {
                this.lineTotalWidth = viewHolder.matchedLine.getWidth();
                this.keywordWidth = (int) paint.measureText(this.keyword);
            }
            String header = "" + matchedLine.lineIndex + ": ";
            int headerWidth = (int) paint.measureText(header);
            int requiredWidth = (int) paint.measureText(header + matchedLine.lineContent);
            int cutChars = 0;
            if (this.lineTotalWidth < requiredWidth && matchedLine.matchedPosition > 0) {
                int actualWidth = (int) paint.measureText(matchedLine.lineContent.substring(0, matchedLine.matchedPosition));
                int idealWidth = ((this.lineTotalWidth - headerWidth) - this.keywordWidth) / 2;
                if (actualWidth > idealWidth) {
                    cutChars = matchedLine.matchedPosition - (((matchedLine.matchedPosition * idealWidth) / actualWidth) - 2);
                }
                if (cutChars > matchedLine.matchedPosition) {
                    cutChars = matchedLine.matchedPosition;
                }
            }
            int highlightStart = header.length() + matchedLine.matchedPosition;
            if (cutChars > 0) {
                lineRec = header + "..." + matchedLine.lineContent.substring(cutChars);
                highlightStart -= cutChars - 3;
            } else {
                lineRec = header + matchedLine.lineContent;
            }
            SpannableString sp = new SpannableString(lineRec);
            sp.setSpan(new ForegroundColorSpan(-65536), highlightStart, this.keyword.length() + highlightStart, 34);
            viewHolder.matchedLine.setText(sp);
        }
        return convertView2;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        String filePath = this.filePathList.get(groupPosition);
        List<MatchedLineItem> matched = this.matchedContents.get(filePath);
        if (matched != null) {
            return matched.size();
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.filePathList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.filePathList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder viewHolder;
        int editResId;
        int replaceId;
        String filePath = this.filePathList.get(groupPosition);
        String groupLabel = filePath.substring(this.pathPrefix.length());
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.activityRef.get().getSystemService("layout_inflater");
            int resId = R.layout.item_matchedfile;
            convertView = layoutInflater.inflate(resId, (ViewGroup) null);
            viewHolder = new GroupViewHolder();
            viewHolder.groupLabel = (TextView) convertView.findViewById(R.id.tv_filepath);
            viewHolder.editMenu = convertView.findViewById(R.id.menu_edit);
            viewHolder.replaceMenu = convertView.findViewById(R.id.menu_replace);
            viewHolder.editImage = (ImageView) convertView.findViewById(R.id.image_edit);
            viewHolder.replaceImage = (ImageView) convertView.findViewById(R.id.image_replace);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }
        if (this.editClicked[groupPosition]) {
            editResId = R.drawable.pencil_blue;
        } else {
            editResId = this.isDark ? R.drawable.pencil_white : R.drawable.pencil;
        }
        viewHolder.editImage.setImageResource(editResId);
        if (this.replaceClicked[groupPosition]) {
            replaceId = R.drawable.ic_replace_blue;
        } else {
            replaceId = this.isDark ? R.drawable.ic_replace_white : R.drawable.ic_replace;
        }
        viewHolder.replaceImage.setImageResource(replaceId);
        TextView groupTextView = viewHolder.groupLabel;
        groupTextView.setTypeface(null, 1);
        groupTextView.setText(groupLabel);
        viewHolder.editMenu.setTag(Integer.valueOf(groupPosition));
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.replaceMenu.setTag(Integer.valueOf(groupPosition));
        viewHolder.replaceMenu.setOnClickListener(this);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public ArrayList<String> getFileList() {
        return this.filePathList;
    }

    public String getKeyword() {
        return this.keyword;
    }

    void addSearchResult(String filePath, List<MatchedLineItem> matchList) {
        synchronized (this.matchedContents) {
            this.matchedContents.put(filePath, matchList);
        }
    }

    public boolean groupChildExist(int groupPosition) {
        boolean containsKey;
        String filePath = this.filePathList.get(groupPosition);
        if (filePath != null) {
            synchronized (this.matchedContents) {
                containsKey = this.matchedContents.containsKey(filePath);
            }
            return containsKey;
        }
        return false;
    }

    public void removeSearchResult(int groupPosition) {
        String filePath = this.filePathList.get(groupPosition);
        if (filePath != null) {
            synchronized (this.matchedContents) {
                this.matchedContents.remove(filePath);
            }
        }
    }

    /* JADX WARN: Type inference failed for: r3v12, types: [android.content.Context, com.gmail.heagoo.apkeditor.ApkInfoActivity] */
    @Override
    public void onClick(View v) {
        Intent intent;
        int id = v.getId();
        if (id == R.id.menu_edit) {
            Integer index = (Integer) v.getTag();
            if (index.intValue() < this.filePathList.size()) {
                if (!this.editClicked[index.intValue()]) {
                    this.editClicked[index.intValue()] = true;
                    notifyDataSetChanged();
                }
                if (this.filePathList.size() <= 100) {
                    ApkInfoActivity activity = this.activityRef.get();
                    intent = TextEditor.getEditorIntent((Context) this.activityRef.get(), this.filePathList, index.intValue(), activity.getApkPath());
                } else {
                    String filePath = this.filePathList.get(index.intValue());
                    ApkInfoActivity apkInfoActivity = this.activityRef.get();
                    intent = TextEditor.getEditorIntent(apkInfoActivity, filePath, apkInfoActivity.getApkPath());
                }
                ActivityUtil.attachParam(intent, "searchString", this.keyword);
                this.activityRef.get().startActivityForResult(intent, 0);
                return;
            }
            return;
        }
        if (id == R.id.menu_replace) {
            Integer index2 = (Integer) v.getTag();
            if (!this.replaceClicked[index2.intValue()]) {
                this.replaceClicked[index2.intValue()] = true;
                notifyDataSetChanged();
            }
            if (index2.intValue() < this.filePathList.size()) {
                if (this.notShowReplaceDlg) {
                    replace(index2.intValue());
                } else {
                    showReplaceDialog(index2.intValue());
                }
            }
        }
    }

    private void showReplaceDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder((Context) this.activityRef.get());
        builder.setTitle(R.string.replace);
        CharSequence msg = String.format(this.activityRef.get().getString(R.string.str_replace_with), this.keyword);
        builder.setMessage(msg);
        final AutoCompleteAdapter adapter = new AutoCompleteAdapter((Context) this.activityRef.get(), "search_replace_with");
        LinearLayout linearLayout = new LinearLayout((Context) this.activityRef.get());
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        final EditTextWithTip input = new EditTextWithTip((Context) this.activityRef.get());
        input.setAdapter(adapter);
        linearLayout.addView(input, new ViewGroup.LayoutParams(-1, -2));
        final CheckBox notshowCb = new CheckBox((Context) this.activityRef.get());
        notshowCb.setText(R.string.label_replace_with_same_setting);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 8, 0, 20);
        notshowCb.setLayoutParams(params);
        linearLayout.addView(notshowCb);
        builder.setView(linearLayout);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                MatchedTextListAdapter.this.strReplace = input.getText().toString();
                if (notshowCb.isChecked()) {
                    MatchedTextListAdapter.this.notShowReplaceDlg = true;
                }
                if (!"".equals(MatchedTextListAdapter.this.strReplace.trim())) {
                    adapter.addInputHistory(MatchedTextListAdapter.this.strReplace);
                }
                MatchedTextListAdapter.this.replace(index);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        builder.show();
    }

    public void replace(int index) {
        String filePath = this.filePathList.get(index);
        try {
            replaceWith(filePath, this.strReplace);
            this.activityRef.get().dealWithModifiedFile(filePath, null);
            this.listviewRef.get().collapseGroup(index);
            removeSearchResult(index);
            String msg = String.format(this.activityRef.get().getString(R.string.str_replaced), this.keyword);
            Toast.makeText((Context) this.activityRef.get(), msg, 0).show();
        } catch (IOException e) {
            Toast.makeText((Context) this.activityRef.get(), e.getMessage(), 1).show();
        }
    }

    public void replaceWith(String filePath, String strReplace) throws IOException {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(filePath, "rw");
            long size = file.length();
            byte[] buffer = new byte[(int) size];
            int offset = 0;
            while (true) {
                int read = file.read(buffer, offset, buffer.length - offset);
                if (read > 0) {
                    offset += read;
                } else {
                    String content = new String(buffer, Encoding.DEFAULT_CHARSET_NAME);
                    String content2 = content.replace(this.keyword, strReplace);
                    file.setLength(0L);
                    file.write(content2.getBytes());
                    return;
                }
            }
        } finally {
            closeQuietly(file);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    public void removeItem(int position) {
        if (position < this.filePathList.size()) {
            String path = this.filePathList.remove(position);
            this.matchedContents.remove(path);
            notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        TextView matchedLine;

        private ViewHolder() {
        }
    }

    private static class GroupViewHolder {
        ImageView editImage;
        View editMenu;
        TextView groupLabel;
        ImageView replaceImage;
        View replaceMenu;

        private GroupViewHolder() {
        }
    }
}
