package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.base.R;
import java.io.File;

public final class DecodeDirectoryPreference extends EditTextPreference {
    public DecodeDirectoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNeutralButton(R.string.decode_choose_folder, (DialogInterface.OnClickListener) null);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        Dialog dialog = getDialog();
        if (dialog instanceof AlertDialog) {
            ((AlertDialog) dialog).getButton(-3).setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    DecodeDirectoryPreference.this.m192xa68832bd(view);
                }
            });
        }
    }

    void m192xa68832bd(View v) {
        browse();
    }

    private void browse() {
        final Context context = getContext();
        final EditText field = getEditText();
        InputMethodManager keyboard = (InputMethodManager) context.getSystemService("input_method");
        if (keyboard != null && field.getWindowToken() != null) {
            keyboard.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        String text = field.getText().toString();
        String str = null;
        File current = text.isEmpty() ? null : new File(text);
        if (current != null && current.isDirectory()) {
            str = current.getAbsolutePath();
        }
        String initial = str;
        FileSelectDialog.IFileSelection callback = new FileSelectDialog.IFileSelection() {
            @Override
            public boolean isInterestedFile(String name, String extra) {
                return false;
            }

            @Override
            public String getConfirmMessage(String path, String extra) {
                return path;
            }

            @Override
            public void fileSelectedInDialog(String path, String extra, boolean open) {
                if (!SettingActivity.dirCanWrite(path)) {
                    Toast.makeText(context, String.format(context.getString(R.string.decode_folder_unwritable), path), 1).show();
                } else {
                    field.setText(path);
                    field.setSelection(field.length());
                }
            }
        };
        new FileSelectDialog(context, callback, null, null, context.getString(R.string.decode_choose_folder_title), true, false, false, "decode-directory", initial, true).show();
    }
}
