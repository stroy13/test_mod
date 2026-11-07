package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MfSearchRetActivity extends CustomizedLangActivity implements View.OnClickListener {
    private ArrayList<EditText> editViews = new ArrayList<>();
    private ArrayList<String> lineContents;
    private ArrayList<Integer> lineIndexs;
    private String xmlPath;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        int layoutId = R.layout.activity_mf_searchret;
        setContentView(layoutId);
        Bundle bundle = getIntent().getExtras();
        this.xmlPath = bundle.getString("xmlPath");
        this.lineIndexs = bundle.getIntegerArrayList("lineIndexs");
        this.lineContents = bundle.getStringArrayList("lineContents");
        initView();
    }

    private void initView() {
        TextView titleTv = (TextView) findViewById(R.id.title);
        String format = getResources().getString(R.string.mf_search_ret);
        String title = String.format(format, Integer.valueOf(this.lineIndexs.size()));
        titleTv.setText(title);
        Button saveBtn = (Button) findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(this);
        Button closeBtn = (Button) findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        LinearLayout layout = (LinearLayout) findViewById(R.id.result_layout);
        for (int i = 0; i < this.lineContents.size(); i++) {
            EditText et = new EditText(this);
            et.setText(this.lineContents.get(i));
            et.setTextSize(2, 14.0f);
            layout.addView(et);
            this.editViews.add(et);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            finish();
        } else if (id == R.id.btn_save) {
            saveModification();
        }
    }

    private void saveModification() {
        boolean modified = false;
        for (int i = 0; i < this.editViews.size(); i++) {
            EditText et = this.editViews.get(i);
            String newStr = et.getText().toString();
            String oldStr = this.lineContents.get(i);
            if (!oldStr.equals(newStr)) {
                this.lineContents.set(i, newStr);
                modified = true;
            }
        }
        if (modified) {
            if (saveManifest()) {
                Toast.makeText((Context) this, R.string.succeed, 0).show();
                setResult(1);
                finish();
                return;
            }
            return;
        }
        Toast.makeText((Context) this, R.string.no_change_detected, 0).show();
    }

    private boolean saveManifest() {
        try {
            FileOutputStream fos = new FileOutputStream(this.xmlPath + ".tmp");
            FileInputStream fis = new FileInputStream(this.xmlPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            List<String> allContents = new ArrayList<>();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                allContents.add(line);
            }
            for (int i = 0; i < this.lineIndexs.size(); i++) {
                int lineIndex = this.lineIndexs.get(i).intValue() - 1;
                String newStr = this.lineContents.get(i);
                String oldStr = allContents.get(lineIndex);
                String head = getHeadPadding(oldStr);
                allContents.set(lineIndex, head + newStr.trim());
            }
            for (String lineStr : allContents) {
                fos.write(lineStr.getBytes());
                fos.write(10);
            }
            br.close();
            fis.close();
            fos.close();
            new File(this.xmlPath + ".tmp").renameTo(new File(this.xmlPath));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText((Context) this, (CharSequence) e.getMessage(), 0).show();
            return false;
        }
    }

    private String getHeadPadding(String str) {
        char c;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length() && ((c = str.charAt(i)) == ' ' || c == '\t'); i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
