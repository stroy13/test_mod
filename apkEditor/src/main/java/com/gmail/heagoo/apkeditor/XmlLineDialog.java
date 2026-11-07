package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlLineDialog extends Dialog implements View.OnClickListener {
    private Context ctx;
    private String extraContent;
    private int itemLayoutId;
    private Dialog keyValueDlg;
    private LinearLayout keyValueLayout;
    private View keyValueView;
    private LinkedHashMap<String, String> keyValues;
    private IXmlLineChanged lineChangeListener;
    private int lineIndex;
    private boolean selfClosed;
    private String tag;
    List<EditText> valueEtList;

    public interface IXmlLineChanged {
        void xmlLineChanged(int i, String str);
    }

    public XmlLineDialog(Context ctx, IXmlLineChanged changeListener, int lineIndex, String lineContent) {
        super(ctx);
        String value;
        String lineContent2 = lineContent;
        this.selfClosed = false;
        requestWindowFeature(1);
        this.ctx = ctx;
        this.lineChangeListener = changeListener;
        this.lineIndex = lineIndex;
        this.itemLayoutId = R.layout.item_stringvalue;
        int endPos = lineContent2.indexOf(62);
        if (endPos != -1) {
            this.extraContent = lineContent2.substring(endPos + 1);
            if (endPos != lineContent2.length() - 1) {
                lineContent2 = lineContent2.substring(0, endPos + 1);
            }
        }
        this.keyValues = new LinkedHashMap<>();
        String[] words = lineContent2.split(" ");
        this.tag = words[0].trim();
        if (this.tag.startsWith("<")) {
            this.tag = this.tag.substring(1);
        } else {
            this.tag = "";
        }
        for (int i = 1; i < words.length; i++) {
            String[] segs = words[i].split("=");
            if (segs.length == 2 && (value = trimValue(segs[1])) != null) {
                this.keyValues.put(segs[0], value);
            }
        }
        this.selfClosed = lineContent2.endsWith("/>");
        int layoutId = R.layout.dlg_xmlline;
        ViewGroup viewGroup = null;
        View view = LayoutInflater.from(ctx).inflate(layoutId, (ViewGroup) null);
        TextView contentTv = (TextView) view.findViewById(R.id.tv_linecontent);
        contentTv.setText(lineContent2);
        Button closeBtn = (Button) view.findViewById(R.id.btn_dlgclose);
        closeBtn.setOnClickListener(this);
        Button saveBtn = (Button) view.findViewById(R.id.btn_dlgsave);
        saveBtn.setOnClickListener(this);
        this.keyValueLayout = (LinearLayout) view.findViewById(R.id.view_keyvalue);
        this.valueEtList = new ArrayList();
        if (this.keyValues.isEmpty()) {
            View v = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 100);
            v.setLayoutParams(lp);
            this.keyValueLayout.addView(v, 0);
            saveBtn.setVisibility(8);
        } else {
            int index = 0;
            for (Map.Entry<String, String> entry : this.keyValues.entrySet()) {
                String lineContent3 = lineContent2;
                View child = LayoutInflater.from(ctx).inflate(this.itemLayoutId, viewGroup);
                TextView tv = (TextView) child.findViewById(R.id.string_name);
                tv.setText(entry.getKey());
                EditText valueEt = (EditText) child.findViewById(R.id.string_value);
                this.valueEtList.add(valueEt);
                valueEt.setText(entry.getValue());
                this.keyValueLayout.addView(child, index);
                lineContent2 = lineContent3;
                index++;
                viewGroup = null;
            }
            ImageView imageView = (ImageView) this.keyValueLayout.findViewById(R.id.hidden_image);
            imageView.setVisibility(0);
            imageView.setOnClickListener(this);
        }
        setContentView(view);
    }

    private String trimValue(String strValue) {
        if (strValue.startsWith("\"")) {
            if (strValue.endsWith("\"")) {
                return strValue.substring(1, strValue.length() - 1);
            }
            if (strValue.endsWith("\">")) {
                return strValue.substring(1, strValue.length() - 2);
            }
            return null;
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_dlgclose) {
            dismiss();
            return;
        }
        if (id == R.id.btn_dlgsave) {
            if (this.lineChangeListener != null) {
                this.lineChangeListener.xmlLineChanged(this.lineIndex, getLineData());
            }
            dismiss();
            return;
        }
        if (id == R.id.hidden_image) {
            this.keyValueDlg = new Dialog(this.ctx);
            int resId = R.layout.dlg_addkeyvalue;
            View view = LayoutInflater.from(this.ctx).inflate(resId, (ViewGroup) null);
            Button okBtn = (Button) view.findViewById(R.id.btn_addkeyvalue_ok);
            okBtn.setOnClickListener(this);
            Button cancelBtn = (Button) view.findViewById(R.id.btn_addkeyvalue_cancel);
            cancelBtn.setOnClickListener(this);
            this.keyValueView = view;
            this.keyValueDlg.requestWindowFeature(1);
            this.keyValueDlg.setContentView(view);
            this.keyValueDlg.show();
            return;
        }
        if (id == R.id.btn_addkeyvalue_ok) {
            EditText keyEt = (EditText) this.keyValueView.findViewById(R.id.key);
            EditText valueEt = (EditText) this.keyValueView.findViewById(R.id.value);
            String strKey = keyEt.getText().toString().trim();
            String strValue = valueEt.getText().toString().trim();
            if (strKey.equals("")) {
                Toast.makeText(this.ctx, R.string.empty_key_tip, 0).show();
                return;
            }
            this.keyValues.put(strKey, strValue);
            View child = LayoutInflater.from(this.ctx).inflate(this.itemLayoutId, (ViewGroup) null);
            TextView tv = (TextView) child.findViewById(R.id.string_name);
            tv.setText(strKey);
            EditText valueEdit = (EditText) child.findViewById(R.id.string_value);
            this.valueEtList.add(valueEdit);
            valueEdit.setText(strValue);
            this.keyValueLayout.addView(child, this.keyValues.size() - 1);
            this.keyValueDlg.dismiss();
            return;
        }
        if (id == R.id.btn_addkeyvalue_cancel) {
            this.keyValueDlg.dismiss();
        }
    }

    private String getLineData() {
        StringBuffer sb = new StringBuffer();
        sb.append("<" + this.tag);
        int index = 0;
        for (Map.Entry<String, String> entry : this.keyValues.entrySet()) {
            String key = entry.getKey();
            EditText et = this.valueEtList.get(index);
            String newValue = et.getText().toString();
            sb.append(" " + key + "=\"" + newValue + "\"");
            index++;
        }
        if (this.selfClosed) {
            sb.append(" />");
        } else {
            sb.append(">");
        }
        sb.append(this.extraContent);
        return sb.toString();
    }
}
