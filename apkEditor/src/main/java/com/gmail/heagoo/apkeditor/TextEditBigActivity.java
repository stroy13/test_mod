package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;
import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.editor.HtmlViewDialog;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.ICommonCallback;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.ColorTheme;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.EditorStatusBar;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditBigActivity extends TextEditBase implements View.OnClickListener, TextWatcher, SelectionChangedListener {
    private ImageView copyButton;
    private int curStartLine;
    private ViewAnimator docFindAnim;
    private ImageView documentListButton;
    private SlidingDrawer documentListDrawer;
    private FrameLayout editorOuterLayout;
    private EditorStatusBar editorStatus;
    private LinearLayout editorView;
    private ImageButton findButton;
    private EditText findText;
    private int fontSize;
    private ImageButton goButton;
    private HtmlViewDialog htmlDialog;
    private boolean lastUndoState;
    private EditText lineNumEdit;
    private View methodMenu;
    private ImageView moreButton;
    private View nextMenu;
    private ImageView openFindButton;
    private ImageView pasteButton;
    private View previousMenu;
    private ImageButton replaceAllButton;
    private ImageButton replaceButton;
    private EditText replaceText;
    private boolean saveBtnEnabled;
    private ImageView saveFileButton;
    private String strSelected;
    private TextEditBigListHelper textEditor;
    private boolean textLoaded;
    private ToggleButton toggleIgnoreCase;
    private ToggleButton toggleRegularExpression;
    private boolean wasOpenedDrawer;

    public TextEditBigActivity() {
        super(true, false);
        this.wasOpenedDrawer = false;
        this.saveBtnEnabled = false;
        this.lastUndoState = true;
        this.textLoaded = false;
        this.strSelected = "";
    }

    public static boolean isValuesXml(String filePath) {
        String[] folders = filePath.split("/");
        if (folders.length > 2) {
            String folder = folders[folders.length - 2];
            if (folder.startsWith("values")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void onStop() {
        super.onStop();
    }

    private void debug(String format, Object... args) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        this.curDocument.setChanged(true);
        updateSaveState();
    }

    public void textSelected(int lineIndex, int start, int end, String strSelected) {
        this.strSelected = strSelected;
        this.editorStatus.setPosition(lineIndex + 1, start + 1);
        updateCopyState(start, end);
        if (this.documentListDrawer.isOpened()) {
            updateReplaceState();
        }
    }

    public void updateLineCount() {
        if (this.curStartLine > 0) {
            this.textEditor.selectItem(this.curStartLine - 1);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMainView();
        initEditView();
        setupOnclickListener();
        new TextLoader().execute(new Void[0]);
    }

    @Override
    protected void specialCharClicked(int idx) {
        Editable editable;
        if (this.textEditor != null && (editable = this.textEditor.getEditableText()) != null) {
            String text = "" + getSpecialChars(null).charAt(idx);
            editable.replace(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd(), text);
            this.textEditor.requestFocus();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void onSaveInstanceState(Bundle outState) {
        String unsavedFilePath;
        super.onSaveInstanceState(outState);
        outState.putInt("curFileIndex", this.curFileIndex);
        outState.putBoolean("modifySaved", this.modifySaved);
        if (this.curDocument != null && this.curDocument.changed()) {
            outState.putBoolean("docChanged", true);
            try {
                unsavedFilePath = SDCard.makeWorkingDir(this) + PathUtil.getNameFromPath(getCurrentFilePath()) + ".tmp";
            } catch (Exception e) {
                unsavedFilePath = getCurrentFilePath() + ".tmp";
            }
            try {
                this.curDocument.save(unsavedFilePath, (Context) this);
                outState.putString("unsavedFilePath", unsavedFilePath);
            } catch (IOException e2) {
            }
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void onResume() {
        super.onResume();
        debug("enter onResume", new Object[0]);
        int fontSize = SettingEditorActivity.getFontSize((Context) this);
        if (this.fontSize != fontSize) {
            this.fontSize = fontSize;
            changeFontSize(fontSize);
            updateSaveState();
        }
        showHideLineNumbers();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void showHideLineNumbers() {
        if (SettingEditorActivity.showLineNumbers(this)) {
            this.textEditor.showLineNumber(true);
        } else {
            this.textEditor.showLineNumber(false);
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    private void initMainView() {
        this.editorStatus = (EditorStatusBar) findViewById(R.id.editor_status);
        this.previousMenu = findViewById(R.id.menu_previous);
        this.nextMenu = findViewById(R.id.menu_next);
        this.methodMenu = findViewById(R.id.menu_methods);
        findViewById(R.id.menu_back).setOnClickListener(this);
        this.previousMenu.setOnClickListener(this);
        this.nextMenu.setOnClickListener(this);
        this.methodMenu.setOnClickListener(this);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void initEditView() {
        this.editorOuterLayout = (FrameLayout) findViewById(R.id.center_layout);
        this.textEditor = new TextEditBigListHelper(this, findViewById(R.id.text_list));
        this.docFindAnim = (ViewAnimator) findViewById(R.id.searchAnimator);
        this.documentListDrawer = (SlidingDrawer) findViewById(R.id.sliding_drawer);
        this.findText = (EditText) findViewById(R.id.findEdit);
        this.replaceText = (EditText) findViewById(R.id.replaceEdit);
        this.findButton = (ImageButton) findViewById(R.id.findBtn);
        this.replaceButton = (ImageButton) findViewById(R.id.replaceBtn);
        this.replaceAllButton = (ImageButton) findViewById(R.id.replaceAllBtn);
        this.openFindButton = (ImageView) findViewById(R.id.openFindBtn);
        this.toggleIgnoreCase = (ToggleButton) findViewById(R.id.checkBoxIgnoreCase);
        this.toggleRegularExpression = (ToggleButton) findViewById(R.id.checkBoxRegexp);
        this.saveFileButton = (ImageView) findViewById(R.id.saveBtn);
        this.copyButton = (ImageView) findViewById(R.id.copyBtn);
        this.pasteButton = (ImageView) findViewById(R.id.pasteBtn);
        this.moreButton = (ImageView) findViewById(R.id.moreBtn);
        this.goButton = (ImageButton) findViewById(R.id.goBtn);
        this.lineNumEdit = (EditText) findViewById(R.id.lineNumEdit);
        this.documentListButton = (ImageView) findViewById(R.id.panel_button);
        enableButton(this.copyButton, false);
        this.textEditor.setInputType(720897);
        this.fontSize = SettingEditorActivity.getFontSize((Context) this);
        changeFontSize(this.fontSize);
        this.openFindButton.setOnClickListener(this);
        this.findButton.setOnClickListener(this);
        this.replaceButton.setOnClickListener(this);
        this.replaceAllButton.setOnClickListener(this);
        this.saveFileButton.setOnClickListener(this);
        this.goButton.setOnClickListener(this);
        this.copyButton.setOnClickListener(this);
        this.pasteButton.setOnClickListener(this);
        this.moreButton.setOnClickListener(this);
        this.saveFileButton.getDrawable().setAlpha(80);
        this.saveFileButton.setClickable(false);
        this.replaceButton.setEnabled(false);
        this.replaceAllButton.setEnabled(false);
        ColorTheme colorTheme = new ColorTheme(this);
        this.editorOuterLayout.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setTextColor(colorTheme.getForeground());
        this.textEditor.setCodeTheme(colorTheme);
        this.textEditor.setBracketSpanColor(getInverseColor(colorTheme.getBackgroundColor()));
        this.textEditor.setOnTextChangeListener(this);
    }

    private void setupOnclickListener() {
        this.documentListDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                TextEditBigActivity.this.adjustOpenedDrawer();
            }
        });
        this.documentListDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                TextEditBigActivity.this.adjustClosedDrawer();
            }
        });
        this.textEditor.setTextSelectionListener(this);
        this.findText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == 1 && keyCode == 66) {
                    TextEditBigActivity.this.executeFindWrapAction(true);
                    return true;
                }
                if (TextEditBigActivity.this.documentListDrawer.getVisibility() == 0 && event.getAction() == 1 && keyCode == 4) {
                    TextEditBigActivity.this.documentListDrawer.close();
                    return true;
                }
                TextEditBigActivity.this.updateReplaceState();
                return false;
            }
        });
        this.replaceText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (TextEditBigActivity.this.documentListDrawer.getVisibility() == 0 && event.getAction() == 1 && keyCode == 4) {
                    TextEditBigActivity.this.documentListDrawer.close();
                    return true;
                }
                TextEditBigActivity.this.updateReplaceState();
                return false;
            }
        });
        super.setupInputMethodMonitor(findViewById(R.id.text_list));
    }

    private int getInverseColor(int background) {
        return Color.argb(BinaryDiffIndex.PAGE_ROWS, 255 - Color.red(background), 255 - Color.green(background), 255 - Color.blue(background));
    }

    private void enableButton(ImageView button, boolean enable) {
        if (enable) {
            button.getDrawable().setAlpha(ParserRule.MAJOR_ACTIONS);
            button.setClickable(true);
        } else {
            button.getDrawable().setAlpha(80);
            button.setClickable(false);
        }
    }

    protected synchronized void updateSaveState() {
        Document document = this.curDocument;
        if (document == null) {
            return;
        }
        boolean docChanged = document.changed();
        if (this.saveBtnEnabled != docChanged) {
            if (docChanged) {
                enableButton(this.saveFileButton, true);
            } else {
                enableButton(this.saveFileButton, false);
            }
            this.saveFileButton.invalidate();
            this.saveBtnEnabled = docChanged;
        }
    }

    private void updateCopyState(int selStart, int selEnd) {
        if (selStart == selEnd) {
            enableButton(this.copyButton, false);
        } else {
            enableButton(this.copyButton, true);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void doReloadWithCheck(final int fileIdxOffset) {
        Document document = this.curDocument;
        boolean docChanged = document.changed();
        if (docChanged) {
            new AlertDialog.Builder(this).setMessage(R.string.save_changes_tip).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditBigActivity.this.executeSaveAction(new ICommonCallback() {
                        public void doCallback() {
                            TextEditBigActivity.this.curFileIndex += fileIdxOffset;
                            new TextLoader().execute(new Void[0]);
                        }
                    });
                }
            }).setNegativeButton(R.string.donot_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditBigActivity.this.curFileIndex += fileIdxOffset;
                    new TextLoader().execute(new Void[0]);
                }
            }).setNeutralButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        } else {
            this.curFileIndex += fileIdxOffset;
            new TextLoader().execute(new Void[0]);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void doCopySelectedText() {
        String selected = this.strSelected;
        if (selected != null && !selected.equals("")) {
            ClipboardUtil.copyToClipboard(this, selected);
            if (selected.contains(Document.LINE_SEPARATOR_UNIX)) {
                Toast.makeText((Context) this, R.string.selected_str_copied, 0).show();
            } else {
                String message = String.format(getString(R.string.copied_to_clipboard), selected);
                Toast.makeText((Context) this, (CharSequence) message, 0).show();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void doPaste() {
        String text = ClipboardUtil.getText(this);
        if (text == null) {
            Toast.makeText((Context) this, R.string.clipboard_no_text, 0).show();
            return;
        }
        Editable editable = this.textEditor.getEditableText();
        if (editable != null) {
            editable.replace(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd(), text);
            this.textEditor.requestFocus();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.menu_back) {
            onBackPressed();
            return;
        }
        if (id == R.id.menu_next) {
            doReloadWithCheck(1);
            return;
        }
        if (id == R.id.menu_previous) {
            doReloadWithCheck(-1);
            return;
        }
        if (id == R.id.moreBtn) {
            showMoreOptions(v);
            return;
        }
        if (id == R.id.openFindBtn) {
            openFindDialog();
            return;
        }
        if (id == R.id.findBtn) {
            executeFindWrapAction(true);
            return;
        }
        if (id == R.id.replaceBtn) {
            executeReplaceAction();
            return;
        }
        if (id == R.id.replaceAllBtn) {
            executeReplaceAll();
            return;
        }
        if (id == R.id.goBtn) {
            gotoButtonClicked();
            return;
        }
        if (id == R.id.saveBtn) {
            executeSaveAction(null);
            return;
        }
        if (id == R.id.copyBtn) {
            doCopySelectedText();
        } else if (id == R.id.pasteBtn) {
            doPaste();
        } else if (id == R.id.menu_methods) {
            showPopWindow(v);
        }
    }

    private void executeReplaceAll() {
        int flags = 8;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = 10;
        }
        if (this.toggleRegularExpression != null && !this.toggleRegularExpression.isChecked()) {
            flags |= 16;
        }
        int totalReplaces = 0;
        try {
            String replacing = this.replaceText.getText().toString();
            List<String> lines = this.textEditor.getTextLines();
            Pattern pattern = Pattern.compile(this.findText.getText().toString(), flags);
            List<Location> occurences = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String lineText = lines.get(i);
                Matcher matcher = pattern.matcher(lineText);
                int startIdx = 0;
                while (matcher.find(startIdx)) {
                    int start = matcher.start();
                    int end = matcher.end();
                    occurences.add(new Location(start, end));
                    startIdx = end;
                }
                if (!occurences.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int lastPos = 0;
                    for (Location loc : occurences) {
                        sb.append(lineText.substring(lastPos, loc.start));
                        sb.append(replacing);
                        lastPos = loc.end;
                    }
                    sb.append(lineText.substring(lastPos));
                    lines.set(i, sb.toString());
                    totalReplaces += occurences.size();
                    occurences.clear();
                }
            }
            this.textEditor.refresh();
            if (totalReplaces > 0) {
                Toast.makeText(getApplicationContext(), String.format(getString(R.string.replace_all_ret), Integer.valueOf(totalReplaces)), 0).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.not_found), 0).show();
            }
        } catch (Exception e) {
        }
    }

    private void gotoButtonClicked() {
        String str = this.lineNumEdit.getText().toString().trim();
        if (str.equals("")) {
            this.textEditor.insert(Document.LINE_SEPARATOR_UNIX);
            this.textEditor.requestFocus();
        } else {
            try {
                int lineNO = Integer.valueOf(str).intValue();
                gotoLine(lineNO);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void gotoLine(int lineNO) {
        if (lineNO > 0) {
            this.lineNumEdit.setText("");
            this.textEditor.selectItem(lineNO - 1);
            this.textEditor.requestFocus();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void executeSaveAction(final ICommonCallback callback) {
        ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            @Override
            public void process() throws Exception {
                FileUtil.writeToFile(TextEditBigActivity.this.getCurrentFilePath(), TextEditBigActivity.this.textEditor.getTextLines());
                TextEditBigActivity.this.curDocument.setChanged(false);
            }

            @Override
            public void afterProcess() {
                TextEditBigActivity.this.updateSaveState();
                TextEditBigActivity.this.modifySaved = true;
                TextEditBigActivity.this.setResult();
                if (callback != null) {
                    callback.doCallback();
                }
            }
        }, R.string.file_saved);
        dlg.show();
    }

    private void openFindDialog() {
        if (this.documentListDrawer.getVisibility() == 8) {
            if (!this.wasOpenedDrawer) {
                this.documentListDrawer.animateOpen();
            }
            this.documentListDrawer.setVisibility(0);
        } else {
            if (!this.wasOpenedDrawer) {
                this.documentListDrawer.close();
            }
            this.documentListDrawer.setVisibility(8);
            this.textEditor.requestFocus();
        }
        String selectedStr = this.strSelected;
        if (selectedStr != null && !"".equals(selectedStr)) {
            if (!selectedStr.contains(Document.LINE_SEPARATOR_UNIX)) {
                this.findText.setText(selectedStr);
            }
        } else if (this.searchString != null && "".equals(this.findText.getText().toString())) {
            this.findText.setText(this.searchString);
        }
        this.findText.requestFocus();
    }

    public boolean executeFindWrapAction(boolean toast) {
        if (this.findText.getText().toString().equals("")) {
            return false;
        }
        int lineIndex = this.textEditor.getSelectionItemIndex();
        int cursorPos = this.textEditor.getSelectionEnd();
        if (executeFindAction(lineIndex, cursorPos)) {
            this.textEditor.requestFocus();
            return true;
        }
        if (executeFindAction(0, 0)) {
            this.textEditor.requestFocus();
            return true;
        }
        if (!toast) {
            return false;
        }
        Toast.makeText(getApplicationContext(), getString(R.string.not_found), 0).show();
        return false;
    }

    private boolean executeFindAction(int fromLine, int fromPosition) {
        Matcher matcher;
        if (fromLine < 0) {
            fromLine = 0;
        }
        int flags = 8;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = 10;
        }
        if (this.toggleRegularExpression != null && !this.toggleRegularExpression.isChecked()) {
            flags |= 16;
        }
        try {
            List<String> allLines = this.textEditor.getTextLines();
            String curLine = allLines.get(fromLine);
            Pattern pattern = Pattern.compile(this.findText.getText().toString(), flags);
            Matcher matcher2 = pattern.matcher(curLine);
            if (matcher2.find(fromPosition)) {
                setSelection(fromLine, matcher2.start(), matcher2.end());
                return true;
            }
            do {
                fromLine++;
                if (fromLine >= allLines.size()) {
                    return false;
                }
                matcher = pattern.matcher(allLines.get(fromLine));
            } while (!matcher.find());
            setSelection(fromLine, matcher.start(), matcher.end());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setSelection(int lineIndex, int start, int end) {
        this.textEditor.setSelection(lineIndex, start, end);
    }

    public void adjustOpenedDrawer() {
        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.textEditor.getLayoutParams();
        int width = this.documentListDrawer.getWidth();
        lp1.setMargins(0, 0, width, lp1.bottomMargin);
        this.textEditor.requestLayout();
        this.documentListButton.setImageResource(R.drawable.edit_slide_right);
    }

    public void adjustClosedDrawer() {
        this.documentListDrawer.setVisibility(8);
        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.textEditor.getLayoutParams();
        lp1.setMargins(0, 0, 0, lp1.bottomMargin);
        this.textEditor.requestLayout();
        this.documentListButton.setImageResource(R.drawable.edit_slide_left);
        this.docFindAnim.setDisplayedChild(0);
    }

    public void updateReplaceState() {
        Pattern pattern;
        try {
            String selected = this.strSelected;
            if (selected != null && !"".equals(selected)) {
                String toFind = this.findText.getText().toString();
                if (this.toggleIgnoreCase.isChecked()) {
                    pattern = Pattern.compile(toFind, 10);
                } else {
                    pattern = Pattern.compile(toFind);
                }
                if (pattern.matcher(selected).matches()) {
                    this.replaceButton.setEnabled(true);
                    this.replaceAllButton.setEnabled(true);
                    return;
                }
            }
        } catch (Exception e) {
        }
        this.replaceButton.setEnabled(false);
        this.replaceAllButton.setEnabled(false);
    }

    private void executeReplaceAction() {
        this.textEditor.replaceSelection(this.replaceText.getText().toString());
        executeFindWrapAction(true);
    }

    private void changeFontSize(int fontSize) {
        if (fontSize > 40) {
            fontSize = 40;
        }
        if (fontSize < 4) {
            fontSize = 4;
        }
        this.textEditor.setTextSize(2, fontSize);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onBackPressed() {
        Document document = this.curDocument;
        boolean docChanged = document != null && document.changed();
        if (!docChanged) {
            finish();
        } else {
            new AlertDialog.Builder(this).setMessage(R.string.save_changes_tip).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditBigActivity.this.executeSaveAction(new ICommonCallback() {
                        public void doCallback() {
                            TextEditBigActivity.this.finish();
                        }
                    });
                }
            }).setNegativeButton(R.string.donot_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditBigActivity.this.finish();
                }
            }).setNeutralButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        }
    }

    @Override
    public void insertString(String str) {
        int index = this.textEditor.getSelectionStart();
        Editable edit = this.textEditor.getEditableText();
        if (edit == null) {
            return;
        }
        if (index < 0 || index >= edit.length()) {
            edit.append((CharSequence) str);
        } else {
            edit.insert(index, str);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override
    public void operateLines(int from, int to) {
        String fmt;
        boolean bModified = false;
        if (this.linesOP == 2) {
            String[] lines = this.textEditor.getText().toString().split(Document.LINE_SEPARATOR_UNIX);
            if (from <= 0) {
                from = 1;
            }
            int end = to <= lines.length ? to : lines.length;
            if (from <= end) {
                List<String> newLines = new ArrayList<>();
                for (int i = 1; i < from; i++) {
                    newLines.add(lines[i - 1]);
                }
                for (int i2 = end + 1; i2 <= lines.length; i2++) {
                    newLines.add(lines[i2 - 1]);
                }
                this.textEditor.setText(newLines);
                bModified = true;
                int deleted = (end - from) + 1;
                String fmt2 = getString(R.string.n_lines_deleted);
                Toast.makeText((Context) this, (CharSequence) String.format(fmt2, Integer.valueOf(deleted)), 1).show();
            }
        } else if (this.linesOP == 5) {
            String[] lines2 = this.textEditor.getText().toString().split(Document.LINE_SEPARATOR_UNIX);
            if (from <= 0) {
                from = 1;
            }
            int end2 = to <= lines2.length ? to : lines2.length;
            if (from <= end2) {
                List<String> newLines2 = new ArrayList<>();
                for (int i3 = 1; i3 < from; i3++) {
                    newLines2.add(lines2[i3 - 1]);
                }
                boolean isUnComment = allStartsWith(lines2, from, end2, "#");
                if (isUnComment) {
                    for (int i4 = from; i4 <= end2; i4++) {
                        int pos = lines2[i4 - 1].indexOf(35);
                        if (pos > 0) {
                            newLines2.add(lines2[i4 - 1].substring(0, pos) + lines2[i4 - 1].substring(pos + 1));
                        } else {
                            newLines2.add(lines2[i4 - 1].substring(pos + 1));
                        }
                    }
                } else {
                    for (int i5 = from; i5 <= end2; i5++) {
                        newLines2.add("#" + lines2[i5 - 1]);
                    }
                }
                for (int i6 = end2 + 1; i6 <= lines2.length; i6++) {
                    newLines2.add(lines2[i6 - 1]);
                }
                this.textEditor.setText(newLines2);
                bModified = true;
                int modified = (end2 - from) + 1;
                if (isUnComment) {
                    fmt = getString(R.string.n_lines_uncommented);
                } else {
                    fmt = getString(R.string.n_lines_commented);
                }
                Toast.makeText((Context) this, (CharSequence) String.format(fmt, Integer.valueOf(modified)), 1).show();
            }
        }
        if (bModified) {
            this.curDocument.setChanged(true);
            updateSaveState();
        }
    }

    private static final class Location {
        public final int end;
        public final int start;

        private Location(int _start, int _end) {
            this.start = _start;
            this.end = _end;
        }
    }

    private class TextLoader extends AsyncTask<Void, Integer, Boolean> {
        private String curFilePath;
        private boolean htmlAvailable;
        private boolean loadSucceed;
        private String syntaxFileName;

        private TextLoader() {
        }

        @Override
        protected void onPreExecute() {
            this.curFilePath = TextEditBigActivity.this.fileList.get(TextEditBigActivity.this.curFileIndex);
            TextEditBigActivity.this.curStartLine = TextEditBigActivity.this.startLineList.get(TextEditBigActivity.this.curFileIndex).intValue();
            this.syntaxFileName = TextEditBigActivity.this.syntaxFileList.get(TextEditBigActivity.this.curFileIndex);
            TextEditBigActivity.this.setCurrentFilePath(this.curFilePath);
            TextEditBigActivity.this.editorStatus.setFileName(TextEditBigActivity.this.getDisplayName(this.curFilePath));
            TextEditBigActivity.this.editorStatus.setSyntaxFromFile(TextEditBigActivity.this.getDisplayName(this.curFilePath), this.syntaxFileName);
            TextEditBigActivity.this.editorStatus.setPosition(1, 1);
            TextEditBigActivity.this.methodMenu.setVisibility((TextEditBase.isSmali(this.curFilePath) || TextEditBase.isJava(this.curFilePath)) ? 0 : 8);
            if (TextEditBigActivity.this.curFileIndex >= TextEditBigActivity.this.fileList.size() - 1) {
                TextEditBigActivity.this.nextMenu.setVisibility(4);
            } else {
                TextEditBigActivity.this.nextMenu.setVisibility(0);
            }
            if (TextEditBigActivity.this.curFileIndex == 0) {
                TextEditBigActivity.this.previousMenu.setVisibility(4);
            } else {
                TextEditBigActivity.this.previousMenu.setVisibility(0);
            }
        }

        @Override
        public Boolean doInBackground(Void... params) {
            Document document = new Document(TextEditBigActivity.this, new File(this.curFilePath), this.syntaxFileName);
            try {
                if (TextEditBigActivity.this.unsavedFilePath != null) {
                    document.load(TextEditBigActivity.this, TextEditBigActivity.this.unsavedFilePath, R.string.error_file_too_big);
                    document.setChanged(true);
                    new File(TextEditBigActivity.this.unsavedFilePath).delete();
                    TextEditBigActivity.this.unsavedFilePath = null;
                } else {
                    document.load(TextEditBigActivity.this, this.curFilePath, R.string.error_file_too_big);
                }
                TextEditBigActivity.this.curDocument = document;
                this.loadSucceed = true;
            } catch (IOException e) {
                this.loadSucceed = false;
            }
            this.htmlAvailable = TextEditBase.isXml(this.curFilePath) || TextEditBase.isSmali(this.curFilePath);
            return null;
        }

        @Override
        public void onPostExecute(Boolean result) {
            if (this.loadSucceed) {
                TextEditBigActivity.this.textLoaded = true;
                TextEditBigActivity.this.textEditor.setDocument(TextEditBigActivity.this.curDocument);
                String strContent = TextEditBigActivity.this.curDocument.getText();
                TextEditBigActivity.this.textEditor.setText(strContent);
                TextEditBigActivity.this.updateLineCount();
                TextEditBigActivity.this.updateSaveState();
                return;
            }
            Toast.makeText((Context) TextEditBigActivity.this, (CharSequence) ("Failed to open " + this.curFilePath), 1).show();
        }
    }
}
