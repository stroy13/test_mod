package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.common.colormixer.ColorMixer;
import com.common.colormixer.ColorMixerDialog;
import com.common.colormixer.ColorValue;
import com.common.colormixer.ColorValueAdapter;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ColorXmlActivity extends CustomizedLangActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String ENTRYNAME = "res/values/colors.xml";
    private ColorValueAdapter colorAdapter;
    private ArrayList<ColorValue> colorValues;
    private Button saveBtn;
    private String xmlPath;

    /* JADX WARN: Multi-variable type inference failed */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        setContentView(R.layout.activity_colors_xml);
        this.xmlPath = ActivityUtil.getParam(getIntent(), "xmlPath");
        initData();
        initView();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode != 0) {
                    initData();
                    updateView();
                    setResult();
                    break;
                }
                break;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void initView() {
        TextView filenameTv = (TextView) findViewById(R.id.filename);
        filenameTv.setText(getFileName());
        this.colorAdapter = new ColorValueAdapter(this, this.colorValues);
        ListView colorLv = (ListView) findViewById(R.id.color_list);
        colorLv.setAdapter((ListAdapter) this.colorAdapter);
        colorLv.setOnItemClickListener(this);
        this.saveBtn = (Button) findViewById(R.id.btn_save);
        this.saveBtn.setOnClickListener(this);
        Button closeBtn = (Button) findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        Button openBtn = (Button) findViewById(R.id.btn_openeditor);
        openBtn.setOnClickListener(this);
    }

    private void updateView() {
        this.colorAdapter.updateData(this.colorValues);
    }

    private String getFileName() {
        int pos = this.xmlPath.lastIndexOf(47);
        return this.xmlPath.substring(pos + 1);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void initData() {
        this.colorValues = new ArrayList<>();
        BufferedReader br = null;
        try {
            try {
                br = new BufferedReader(new FileReader(new File(this.xmlPath)));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    ColorValue value = parseLine(line);
                    if (value != null) {
                        this.colorValues.add(value);
                    }
                }
                br.close();
            } catch (Exception e) {
                br.close();
            } catch (Throwable th) {
                try {
                    br.close();
                } catch (IOException e2) {
                }
                throw th;
            }
        } catch (IOException e3) {
        }
        for (int i = 0; i < this.colorValues.size(); i++) {
            this.colorValues.get(i).parseRefColor(this, this.colorValues);
        }
    }

    private ColorValue parseLine(String line) {
        int startPos;
        int endPos;
        int position = line.indexOf("<color name=\"");
        if (position == -1 || (endPos = line.indexOf("\">", (startPos = "<color name=\"".length() + position))) == -1) {
            return null;
        }
        String name = line.substring(startPos, endPos);
        int startPos2 = endPos + 2;
        int endPos2 = line.indexOf("</color>", startPos2);
        if (endPos2 == -1) {
            return null;
        }
        String strValue = line.substring(startPos2, endPos2);
        ColorValue result = new ColorValue(name, strValue);
        return result;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            finish();
            return;
        }
        if (id == R.id.btn_save) {
            saveColors();
            finish();
        } else if (id == R.id.btn_openeditor) {
            openInEditor();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void openInEditor() {
        Intent intent = TextEditor.getEditorIntent(this, this.xmlPath, null);
        ActivityUtil.attachParam(intent, "syntaxFileName", "xml.xml");
        ActivityUtil.attachParam(intent, "displayFileName", "colors.xml");
        ActivityUtil.attachParam(intent, "extraString", ENTRYNAME);
        startActivityForResult(intent, 0);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void saveColors() {
        BufferedWriter bw = null;
        try {
            try {
                bw = new BufferedWriter(new FileWriter(this.xmlPath));
                bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                bw.write("<resources>\n");
                for (int i = 0; i < this.colorValues.size(); i++) {
                    bw.write(this.colorValues.get(i).toString());
                    bw.write(Document.LINE_SEPARATOR_UNIX);
                }
                bw.write("</resources>");
                setResult();
            } catch (Throwable th) {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                    }
                }
                throw th;
            }
        } catch (Exception e2) {
            String fmt = getString(R.string.general_error);
            String message = String.format(fmt, e2.getMessage());
            Toast.makeText((Context) this, (CharSequence) message, 1).show();
            if (bw == null) {
                return;
            }
        }
        try {
            bw.close();
        } catch (IOException e3) {
        }
    }

    private void setResult() {
        Intent intent = new Intent();
        intent.putExtra("xmlPath", this.xmlPath);
        intent.putExtra("extraString", ENTRYNAME);
        setResult(1, intent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        int initColor = this.colorValues.get(position).intColorValue;
        ColorMixerDialog dialog = new ColorMixerDialog(this, initColor, new ColorMixer.OnColorChangedListener() {
            public void onColorChange(int argb) {
                ColorXmlActivity.this.colorChanged(position, argb);
            }
        });
        dialog.show();
    }

    protected void colorChanged(int position, int argb) {
        if (position < this.colorValues.size()) {
            ColorValue value = this.colorValues.get(position);
            value.intColorValue = argb;
            value.strColorValue = "#" + Integer.toHexString(argb);
            this.saveBtn.setVisibility(0);
            this.colorAdapter.notifyDataSetChanged();
        }
    }
}
