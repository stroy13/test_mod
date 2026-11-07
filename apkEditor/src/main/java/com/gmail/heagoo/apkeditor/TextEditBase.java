package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.common.colormixer.ColorMixer;
import com.common.colormixer.ColorMixerDialog;
import com.gmail.heagoo.apkeditor.LinesOpDialogHelper;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.SmaliMethodWindowHelper;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.editor.HtmlViewDialog;
import com.gmail.heagoo.apkeditor.inf.IJavaExtractor;
import com.gmail.heagoo.apkeditor.util.AndroidBug5497Workaround;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.InputMethodWatcher;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class TextEditBase extends CustomizedLangActivity implements ColorMixer.OnColorChangedListener, SmaliMethodWindowHelper.ISmaliMethodClicked, LinesOpDialogHelper.ILinesOperation {
    private String apkPath;
    private boolean bInputMethodShown;
    protected boolean batchMode;
    protected Document curDocument;
    private String curFilePath;
    protected String displayName;
    protected List<String> extraStringList;
    protected List<String> fileList;
    private boolean fixBug;
    private HtmlViewDialog htmlDialog;
    private boolean isForBigFile;
    protected int linesOP;
    private LinearLayout menuLayout;
    protected String searchString;
    private View specialCharLayout;
    protected List<Integer> startLineList;
    protected List<String> syntaxFileList;
    protected String unsavedFilePath;
    protected int curFileIndex = 0;
    protected boolean textWrap = true;
    protected boolean modifySaved = false;
    protected ArrayList<String> modifiedFiles = new ArrayList<>();
    private SmaliMethodWindowHelper popupWindowHelper = new SmaliMethodWindowHelper(this);
    private String[] specialStrings = {",.?!:;~-_=\"'/@*+()<>{}[]%&$|\\#^", "<>\":=/@+.-?#_()[]{}\\;!$%^&*|~',", "(){};.=\"'|&![]@<>+-*/?:,_\\^%#~$"};
    private int specialStrIndex = -1;

    public abstract void insertString(String str);

    protected abstract void specialCharClicked(int i);

    public TextEditBase(boolean isForBigFile, boolean fixBug) {
        this.isForBigFile = isForBigFile;
        this.fixBug = fixBug;
    }

    public static boolean isSmali(String filePath) {
        return filePath != null && filePath.endsWith(".smali");
    }

    public static boolean isJava(String filePath) {
        return filePath != null && filePath.endsWith(".java");
    }

    public static boolean isXml(String filePath) {
        return filePath.endsWith(".xml");
    }

    protected static boolean allStartsWith(String[] lines, int start, int end, String strStart) {
        for (int i = start; i <= end; i++) {
            if (!lines[i - 1].trim().startsWith(strStart)) {
                return false;
            }
        }
        return true;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        boolean isFullScreen = GlobalConfig.instance(this).isFullScreen();
        if (isFullScreen) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
        }
        getWindow().setSoftInputMode(2);
        GlobalConfig.instance(this).getThemeId();
        if (this.isForBigFile) {
            setContentView(R.layout.activity_editor_bigfile);
        } else {
            this.textWrap = SettingEditorActivity.isLineWrap(this);
            if (this.textWrap) {
                setContentView(R.layout.activity_editor_wrap);
            } else {
                setContentView(R.layout.activity_editor);
            }
        }
        if (this.fixBug && isFullScreen) {
            AndroidBug5497Workaround.assistActivity(this);
        }
        Intent intent = getIntent();
        String filePath = ActivityUtil.getParam(intent, "xmlPath");
        this.apkPath = ActivityUtil.getParam(intent, "apkPath");
        this.searchString = ActivityUtil.getParam(intent, "searchString");
        try {
            if (filePath != null) {
                this.batchMode = false;
                int startLine = -1;
                String strLine = ActivityUtil.getParam(intent, "startLine");
                if (strLine != null) {
                    startLine = Integer.valueOf(strLine).intValue();
                }
                String syntaxFileName = ActivityUtil.getParam(intent, "syntaxFileName");
                String extraString = ActivityUtil.getParam(intent, "extraString");
                this.displayName = ActivityUtil.getParam(intent, "displayFileName");
                this.fileList = new ArrayList(1);
                this.fileList.add(filePath);
                this.startLineList = new ArrayList();
                this.startLineList.add(Integer.valueOf(startLine));
                this.syntaxFileList = new ArrayList(1);
                this.syntaxFileList.add(syntaxFileName);
                this.extraStringList = new ArrayList(1);
                this.extraStringList.add(extraString);
            } else {
                this.batchMode = true;
                this.fileList = ActivityUtil.getStringArray(intent, "fileList");
                this.curFileIndex = ActivityUtil.getIntParam(intent, "curFileIndex");
                this.startLineList = ActivityUtil.getIntArray(intent, "startLineList");
                this.syntaxFileList = ActivityUtil.getStringArray(intent, "syntaxFileList");
                this.extraStringList = ActivityUtil.getStringArray(intent, "extraStringList");
                if (this.startLineList == null) {
                    this.startLineList = new ArrayList(this.fileList.size());
                    for (int i = 0; i < this.fileList.size(); i++) {
                        this.startLineList.add(-1);
                    }
                }
                if (this.syntaxFileList == null) {
                    this.syntaxFileList = new ArrayList(this.fileList.size());
                    for (int i2 = 0; i2 < this.fileList.size(); i2++) {
                        this.syntaxFileList.add(null);
                    }
                }
                if (this.extraStringList == null) {
                    this.extraStringList = new ArrayList(this.fileList.size());
                    for (int i3 = 0; i3 < this.fileList.size(); i3++) {
                        this.extraStringList.add(null);
                    }
                }
            }
            if (savedInstanceState != null) {
                this.curFileIndex = savedInstanceState.getInt("curFileIndex");
                this.modifySaved = savedInstanceState.getBoolean("modifySaved", false);
                if (this.modifySaved) {
                    setResult();
                }
                boolean changed = savedInstanceState.getBoolean("docChanged", false);
                if (changed) {
                    this.unsavedFilePath = savedInstanceState.getString("unsavedFilePath");
                }
            }
            initBottomMenu();
        } catch (Exception e) {
            Toast.makeText((Context) this, R.string.failed, 0).show();
            finish();
        }
    }

    protected String getCurrentFilePath() {
        return this.curFilePath;
    }

    protected void setCurrentFilePath(String filePath) {
        this.curFilePath = filePath;
        updateSpecialCharButton(filePath);
    }

    private void initBottomMenu() {
        this.specialCharLayout = findViewById(R.id.special_char_layout);
        this.menuLayout = (LinearLayout) findViewById(R.id.menu_layout);
        View switchView = findViewById(R.id.switch_view);
        if (SettingEditorActivity.symbolInputEnabled(this)) {
            switchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextEditBase.this.menuLayout.getVisibility() == 0) {
                        TextEditBase.this.specialCharLayout.setVisibility(0);
                        TextEditBase.this.menuLayout.setVisibility(4);
                    } else {
                        TextEditBase.this.menuLayout.setVisibility(0);
                        TextEditBase.this.specialCharLayout.setVisibility(4);
                    }
                }
            });
        } else {
            switchView.setVisibility(8);
            findViewById(R.id.separate_view).setVisibility(8);
        }
    }

    private void updateSpecialCharButton(String filePath) {
        int idx = getSpecialStringIndex(filePath);
        if (idx == this.specialStrIndex) {
            return;
        }
        this.specialStrIndex = idx;
        LinearLayout parentView = (LinearLayout) findViewById(R.id.special_char_container);
        parentView.removeAllViews();
        int screenWidth = Display.getWidth(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(screenWidth / 11, -1);
        String str = this.specialStrings[this.specialStrIndex];
        for (int i = 0; i < str.length(); i++) {
            TextView tv = new TextView(this);
            tv.setTextSize(2, 20.0f);
            tv.setBackgroundResource(R.drawable.editor_action_background);
            tv.setText("" + str.charAt(i));
            TypedValue color = new TypedValue();
            getTheme().resolveAttribute(R.attr.labText, color, true);
            tv.setTextColor(color.data);
            tv.setGravity(17);
            tv.setTag(Integer.valueOf(i));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int idx2 = ((Integer) v.getTag()).intValue();
                    TextEditBase.this.specialCharClicked(idx2);
                }
            });
            parentView.addView(tv, param);
        }
    }

    public void updateBottomMenu(boolean inputMethodVisible) {
        if (SettingEditorActivity.symbolInputEnabled(this)) {
            if (inputMethodVisible) {
                this.specialCharLayout.setVisibility(0);
                this.menuLayout.setVisibility(4);
            } else {
                this.menuLayout.setVisibility(0);
                this.specialCharLayout.setVisibility(4);
            }
        }
    }

    private int getSpecialStringIndex(String filePath) {
        if (filePath == null) {
            return this.specialStrIndex;
        }
        if (filePath.endsWith(".xml")) {
            return 1;
        }
        if (filePath.endsWith(".java")) {
            return 2;
        }
        return 0;
    }

    protected String getSpecialChars(String filePath) {
        if (filePath == null) {
            if (this.specialStrIndex >= 0) {
                return this.specialStrings[this.specialStrIndex];
            }
            return "";
        }
        return this.specialStrings[getSpecialStringIndex(filePath)];
    }

    protected void setResult() {
        Intent intent = new Intent();
        if (!this.batchMode) {
            intent.putExtra("xmlPath", this.curFilePath);
            intent.putExtra("extraString", this.extraStringList.get(0));
        } else {
            if (!this.modifiedFiles.contains(this.curFilePath)) {
                this.modifiedFiles.add(this.curFilePath);
            }
            intent.putStringArrayListExtra("modifiedFiles", this.modifiedFiles);
        }
        setResult(1, intent);
    }

    protected int getNumberDigits(int lines) {
        int nd = 1;
        while (lines >= 10) {
            lines /= 10;
            nd++;
        }
        return Math.max(2, nd);
    }

    protected void setupInputMethodMonitor(final InputMethodWatcher watcher) {
        final View contentView = findViewById(android.R.id.content);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                contentView.getWindowVisibleDisplayFrame(r);
                int screenHeight = contentView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                TextEditBase.this.bInputMethodShown = ((double) keypadHeight) > ((double) screenHeight) * 0.15d;
                watcher.setInputMethodVisible(TextEditBase.this.bInputMethodShown);
                TextEditBase.this.updateBottomMenu(TextEditBase.this.bInputMethodShown);
            }
        });
    }

    protected boolean isInputMethodShown() {
        return this.bInputMethodShown;
    }

    protected void showMoreOptions(View parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.popup_list, (ViewGroup) null);
        ListView lv_group = (ListView) view.findViewById(R.id.lvGroup);
        final MoreEditorOptionAdapter adapter = new MoreEditorOptionAdapter(this, this.curFilePath);
        lv_group.setAdapter((ListAdapter) adapter);
        int optionNum = adapter.getOptions();
        int height = Math.min(Display.dip2px(this, (optionNum * 48) + 12), (getResources().getDisplayMetrics().heightPixels * 3) / 5);
        int width = Math.min(Display.dip2px(this, 300.0f), Display.getWidth(this) - Display.dip2px(this, 32.0f));
        final PopupWindow popupWindow = new PopupWindow(view, width, height);
        lv_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view2, int position, long id) {
                int cmd = adapter.getCommandByPosition(position);
                switch (cmd) {
                    case 0:
                        TextEditBase.this.showHtmlDialog();
                        break;
                    case 1:
                        new ColorMixerDialog(TextEditBase.this, -1, TextEditBase.this).show();
                        break;
                    case 2:
                        TextEditBase.this.linesOP = 2;
                        new LinesOpDialogHelper().showDialog(TextEditBase.this, R.string.delete_lines, TextEditBase.this);
                        break;
                    case 3:
                        Intent intent = new Intent((Context) TextEditBase.this, (Class<?>) SettingEditorActivity.class);
                        TextEditBase.this.startActivity(intent);
                        break;
                    case 4:
                        Intent it = new Intent((Context) TextEditBase.this, (Class<?>) EditorHelpActivity.class);
                        TextEditBase.this.startActivity(it);
                        break;
                    case 5:
                        TextEditBase.this.linesOP = 5;
                        new LinesOpDialogHelper().showDialog(TextEditBase.this, R.string.comment_lines, TextEditBase.this);
                        break;
                    case 6:
                        new SmaliCodeDialog(TextEditBase.this, TextEditBase.this.curFilePath).show();
                        break;
                    case 7:
                        TextEditBase.this.showJavaCodeWithTip();
                        break;
                }
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
            }
        });
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setElevation(Display.dip2px(this, 8.0f));
        popupWindow.showAtLocation(parent, 8388661, Display.dip2px(this, 12.0f), Display.dip2px(this, 52.0f));
    }

    public void showHtmlDialog() {
        if (this.htmlDialog == null) {
            this.htmlDialog = new HtmlViewDialog(this);
        }
        this.htmlDialog.show(this.curFilePath, getDisplayName(this.curFilePath));
    }

    public void showJavaCodeWithTip() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean shown = sp.getBoolean("java_edit_tip_shown", false);
        if (!shown) {
            AlertDialog.Builder tipDlg = new AlertDialog.Builder(this).setTitle(R.string.please_note).setMessage(R.string.java_code_edit_tip).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditBase.this.showJavaCode();
                }
            });
            tipDlg.show();
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("java_edit_tip_shown", true);
            editor.commit();
            return;
        }
        showJavaCode();
    }

    public void showJavaCode() {
        if (this.apkPath == null) {
            Toast.makeText((Context) this, (CharSequence) "Internal error: cannot find apk path to decode java code, please contact the author.", 1).show();
            return;
        }
        try {
            final String workingDirectory = SDCard.makeWorkingDir(this);
            String[] dexAndClass = getDexAndClassName();
            final String dexName = dexAndClass[0];
            final String className = dexAndClass[1];
            if (className == null) {
                Toast.makeText((Context) this, (CharSequence) "Internal error: Cannot get class name, please contact the author.", 1).show();
            } else {
                new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
                    String errMessage;
                    boolean succeed = false;

                    @Override
                    public void process() throws Exception {
                        boolean modern = "modern".equals(PreferenceManager.getDefaultSharedPreferences(TextEditBase.this).getString("JadxVersion", "legacy"));
                        String extractorClass = modern ? "com.gmail.heagoo.apkeditor.pro.ModernJavaExtractor" : "com.gmail.heagoo.apkeditor.pro.JavaExtractor";
                        IJavaExtractor extractor = (IJavaExtractor) RefInvoke.createInstance(extractorClass, new Class[]{String.class, String.class, String.class, String.class}, new Object[]{TextEditBase.this.apkPath, dexName, className, workingDirectory});
                        if (extractor == null) {
                            this.errMessage = "Cannot load Java decompiler";
                        }
                        if (extractor != null) {
                            this.succeed = extractor.extract();
                            if (!this.succeed) {
                                this.errMessage = extractor.getErrorMessage();
                            }
                        }
                    }

                    @Override
                    public void afterProcess() {
                        if (!this.succeed) {
                            Toast.makeText((Context) TextEditBase.this, (CharSequence) this.errMessage, 1).show();
                            return;
                        }
                        String relativePath = className.substring(1);
                        String path = workingDirectory + relativePath + ".java";
                        boolean fileExist = new File(path).exists();
                        if (!fileExist) {
                            int position = relativePath.lastIndexOf(36);
                            if (position != -1) {
                                relativePath = relativePath.substring(0, position);
                                path = workingDirectory + relativePath + ".java";
                                fileExist = new File(path).exists();
                            }
                        }
                        if (!fileExist) {
                            String defPath = workingDirectory + "defpackage/" + relativePath + ".java";
                            if (new File(defPath).exists()) {
                                path = defPath;
                                fileExist = true;
                            }
                        }
                        if (!fileExist) {
                            Toast.makeText((Context) TextEditBase.this, R.string.cannot_find_java_file, 1).show();
                            return;
                        }
                        Intent intent = TextEditor.getEditorIntent(TextEditBase.this, path, null);
                        TextEditBase.this.startActivity(intent);
                    }
                }, -1).show();
            }
        } catch (Exception e) {
            Toast.makeText((Context) this, (CharSequence) "Cannot make working directory.", 0).show();
        }
    }

    private String[] getDexAndClassName() {
        String dexName = "classes.dex";
        String[] dirs = this.curFilePath.split("/");
        int i = 0;
        while (true) {
            if (i >= dirs.length || "smali".equals(dirs[i])) {
                break;
            }
            if (!dirs[i].startsWith("smali_")) {
                i++;
            } else {
                dexName = dirs[i].substring(6) + ".dex";
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append('L');
        while (true) {
            i++;
            if (i >= dirs.length) {
                break;
            }
            String name = dirs[i];
            if (i == dirs.length - 1) {
                if (name.length() > 6 && name.endsWith(".smali")) {
                    sb.append(name.substring(0, name.length() - 6));
                }
            } else {
                sb.append(name);
                sb.append('/');
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return new String[]{dexName, sb.toString()};
    }

    public String getDisplayName(String filePath) {
        if (!this.batchMode && this.displayName != null) {
            return this.displayName;
        }
        int pos = filePath.lastIndexOf(47);
        if (pos != -1) {
            return filePath.substring(pos + 1);
        }
        return filePath;
    }

    public void onColorChange(int argb) {
        String strColor = String.format("#%08x", Integer.valueOf(argb));
        ClipboardUtil.copyToClipboard(this, strColor);
        String message = String.format(getString(R.string.copied_to_clipboard), strColor);
        Toast.makeText((Context) this, (CharSequence) message, 0).show();
    }

    @Override
    public void operateLines(int from, int to) {
        Toast.makeText((Context) this, (CharSequence) "Not implemented.", 0).show();
    }

    protected void showPopWindow(View parent) {
        if (this.popupWindowHelper != null && this.curFilePath.equals(this.popupWindowHelper.getFile())) {
            this.popupWindowHelper.doPopWindowShow(parent);
        } else {
            this.popupWindowHelper.asyncShowPopup(this, this.curFilePath, this.curDocument.getText(), parent);
        }
    }
}
