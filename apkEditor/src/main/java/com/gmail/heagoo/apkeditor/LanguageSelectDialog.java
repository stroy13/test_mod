package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import brut.util.LanguageMapping;
import com.gmail.heagoo.apkeditor.base.R;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class LanguageSelectDialog extends Dialog implements View.OnClickListener {
    private WeakReference<ApkInfoActivity> activityRef;
    private EditText codeEt;
    private String[] codes;
    private View contentView;
    private boolean isAutoTranslate;
    private String[] languages;

    public LanguageSelectDialog(ApkInfoActivity apkInfoActivity, String[] _lang, String[] _codes) {
        super(apkInfoActivity);
        requestWindowFeature(1);
        this.languages = _lang;
        this.codes = _codes;
        this.isAutoTranslate = this.languages != null;
        int layoutId = R.layout.dlg_selectlanguage;
        this.activityRef = new WeakReference<>(apkInfoActivity);
        this.contentView = apkInfoActivity.getLayoutInflater().inflate(layoutId, (ViewGroup) null, false);
        setContentView(this.contentView);
        this.codeEt = (EditText) this.contentView.findViewById(R.id.language_code);
        if (this.isAutoTranslate) {
            this.codeEt.setEnabled(false);
        }
        initSpinner();
        initButton();
    }

    @Override
    public void setTitle(int resId) {
        TextView titleTv = (TextView) this.contentView.findViewById(R.id.tv_title);
        titleTv.setText(resId);
    }

    /** Explain that this picker selects the destination, not the source. */
    public void setTranslationSource(String source) {
        TextView title = (TextView) this.contentView.findViewById(R.id.tv_title);
        title.setText(getContext().getString(R.string.ai_target_picker_title, source));
        TextView label = (TextView) this.contentView.findViewById(R.id.language_tv);
        label.setText(R.string.ai_target_language);
    }

    private void initButton() {
        Button okBtn = (Button) this.contentView.findViewById(R.id.btn_addlang_ok);
        okBtn.setOnClickListener(this);
        Button cancelBtn = (Button) this.contentView.findViewById(R.id.btn_addlang_cancel);
        cancelBtn.setOnClickListener(this);
    }

    private void initSpinner() {
        int size = LanguageMapping.getSize();
        if (this.codes == null || this.languages == null) {
            this.codes = new String[size];
            this.languages = new String[size];
            LanguageMapping.getLanguages(this.codes, this.languages);
        }
        Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>((Context) this.activityRef.get(), R.layout.language_spinner_item, this.languages);
        adapter.setDropDownViewResource(R.layout.language_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        Locale locale = Locale.getDefault();
        String code = "-" + locale.getLanguage();
        int selected = getCodeIndex(code);
        if (selected != -1) {
            spinner.setSelection(selected);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                LanguageSelectDialog.this.updateLanguageCode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private int getCodeIndex(String code) {
        for (int i = 0; i < this.codes.length; i++) {
            if (this.codes[i].startsWith(code)) {
                int idx = i;
                return idx;
            }
        }
        return -1;
    }

    protected void updateLanguageCode(int position) {
        this.codeEt.setText(this.codes[position]);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_addlang_ok) {
            String strCode = this.codeEt.getText().toString();
            if (this.isAutoTranslate) {
                translateLanguage(strCode);
                dismiss();
                return;
            } else {
                if (addLanguage(strCode)) {
                    dismiss();
                    return;
                }
                return;
            }
        }
        if (id == R.id.btn_addlang_cancel) {
            dismiss();
        }
    }

    private boolean addLanguage(String strCode) {
        ApkInfoActivity apkInfoActivity = this.activityRef.get();
        String error = apkInfoActivity.addLanguageRetError(strCode);
        if (error == null) {
            return true;
        }
        Toast.makeText((Context) apkInfoActivity, (CharSequence) error, 0).show();
        return false;
    }

    private void translateLanguage(String strCode) {
        ApkInfoActivity activity = this.activityRef.get();
        activity.translateLanguage(strCode);
    }
}
