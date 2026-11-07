package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.ProcessingDialog;
import com.gmail.heagoo.common.RefInvoke;
import java.io.File;
import java.io.IOException;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorActivity extends CustomizedLangActivity implements View.OnClickListener {
    private AsyncSyntaxController asyncSyntax;
    private boolean autoTextChange;
    private ImageView configButton;
    private Document curDocument;
    private ViewAnimator docFindAnim;
    private ImageView documentListButton;
    private SlidingDrawer documentListDrawer;
    private HorizontalScrollView editorHorizontalLayout;
    private LinearLayout editorInnerLayout;
    private LinearLayout editorLayout;
    private FrameLayout editorOuterLayout;
    private ObScrollView editorScrollView;
    private EditorStatusBar editorStatus;
    private LinearLayout editorView;
    private String filePath;
    private ImageButton findButton;
    private EditText findText;
    private int fontSize;
    private boolean isRootMode;
    private View lineDivider;
    private EditText lineNumbers;
    private ImageView openFindButton;
    public int previousLineCount;
    private int previousMaxDigits;
    private String realFilePath;
    private ImageButton replaceButton;
    private EditText replaceText;
    private int resId_fileSaved;
    private int resId_notFound;
    private int resId_tooBig;
    private ImageView saveFileButton;
    private ScrollView scrollView;
    private String syntaxFileName;
    protected ObEditText textEditor;
    private ToggleButton toggleIgnoreCase;
    private ToggleButton toggleRegularExpression;
    private static int AUTODELAY = 300;
    private static int LINECOUNTDELAY = 300;
    private static int TYPEDELAY = 400;
    private static int SCROLLDELAY = 40;
    protected boolean modifySaved = false;
    UpdateLineCount ulcTask = new UpdateLineCount();
    SyntaxHighLight shTask = new SyntaxHighLight();
    private boolean syntaxHighlighting = true;
    private boolean hlChange = false;
    private int hlEnd = -1;
    private int hlStart = -1;
    private boolean textWrap = true;
    private int highlightSize = 80;
    private Handler mHandler = new Handler();
    private boolean updateLineCount = true;
    private boolean wasOpenedDrawer = false;
    private boolean lastRedoState = true;
    private boolean lastSaveState = true;
    private boolean lastChangedState = false;
    private boolean lastUndoState = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        this.textWrap = true;
        if (this.textWrap) {
            setContentView(R.layout.editorutil_main);
        } else {
            setContentView(R.layout.editorutil_main);
        }
        try {
            Bundle bundle = getIntent().getExtras();
            this.filePath = bundle.getString("filePath");
            this.realFilePath = bundle.getString("realFilePath");
            this.syntaxFileName = bundle.getString("syntaxFileName");
            this.isRootMode = bundle.getBoolean("isRootMode");
            int[] resIds = bundle.getIntArray("resourceIds");
            this.resId_tooBig = resIds[0];
            this.resId_fileSaved = resIds[1];
            this.resId_notFound = resIds[2];
            if (savedInstanceState != null) {
                this.modifySaved = savedInstanceState.getBoolean("modifySaved", false);
                if (this.modifySaved) {
                    setResult();
                }
            }
            initData();
            initMainView();
            initEditView();
        } catch (Exception e) {
            Toast.makeText((Context) this, (CharSequence) "failed", 0).show();
            finish();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("modifySaved", this.modifySaved);
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
        setupOnclickListener();
        if (this.fontSize != 12) {
            this.fontSize = 12;
            changeFontSize(12);
        }
        int lines = this.curDocument.getText().split(Document.LINE_SEPARATOR_UNIX).length + 1;
        int digits = this.ulcTask.getNumberDigits(lines);
        changeLineNumbers(true, digits);
        new Handler().postDelayed(new Runnable() {
            @Override
            public final void run() {
                EditorActivity.this.m399lambda$onResume$0$comgmailheagooneweditorEditorActivity();
            }
        }, 400L);
    }

    void m399lambda$onResume$0$comgmailheagooneweditorEditorActivity() {
        updateEditor(true);
    }

    private boolean isXml() {
        return this.filePath.endsWith(".xml");
    }

    private boolean isSmali() {
        return this.filePath.endsWith(".smali");
    }

    private boolean isValuesXml() {
        String[] folders = this.filePath.split("/");
        if (folders.length > 2) {
            String folder = folders[folders.length - 2];
            if (folder.startsWith("values")) {
                return true;
            }
            return false;
        }
        return false;
    }

    private String getFileName() {
        int pos;
        if (this.realFilePath != null && (pos = this.realFilePath.lastIndexOf(47)) != -1) {
            return this.realFilePath.substring(pos + 1);
        }
        int pos2 = this.filePath.lastIndexOf(47);
        if (pos2 != -1) {
            return this.filePath.substring(pos2 + 1);
        }
        return this.filePath;
    }

    private void initData() {
        this.curDocument = new Document(this, new File(this.filePath), this.syntaxFileName);
        try {
            this.curDocument.load(this, this.filePath, this.resId_tooBig);
        } catch (IOException e) {
            Toast.makeText((Context) this, (CharSequence) ("Failed to open " + this.filePath), 1).show();
            e.printStackTrace();
        }
    }

    private void initMainView() {
        this.editorStatus = (EditorStatusBar) findViewById(R.id.editor_status);
        this.editorStatus.setFileName(getFileName());
        this.editorStatus.setSyntaxFromFile(this.filePath, this.syntaxFileName);
        findViewById(R.id.menu_back).setOnClickListener(this);
        this.editorView = (LinearLayout) findViewById(R.id.editorView);
    }

    private void initEditView() {
        this.editorInnerLayout = (LinearLayout) findViewById(R.id.editorLayout);
        this.editorOuterLayout = (FrameLayout) findViewById(R.id.center_layout);
        this.lineNumbers = (EditText) findViewById(R.id.lineNumbers);
        this.textEditor = (ObEditText) findViewById(R.id.editor);
        this.lineDivider = findViewById(R.id.divider);
        this.editorScrollView = (ObScrollView) findViewById(R.id.editorScrollview);
        this.docFindAnim = (ViewAnimator) findViewById(R.id.searchAnimator);
        this.documentListDrawer = (SlidingDrawer) findViewById(R.id.sliding_drawer);
        this.findText = (EditText) findViewById(R.id.findEdit);
        this.replaceText = (EditText) findViewById(R.id.replaceEdit);
        this.findButton = (ImageButton) findViewById(R.id.findBtn);
        this.replaceButton = (ImageButton) findViewById(R.id.replaceBtn);
        this.openFindButton = (ImageView) findViewById(R.id.openFindBtn);
        this.toggleIgnoreCase = (ToggleButton) findViewById(R.id.checkBoxIgnoreCase);
        this.toggleRegularExpression = (ToggleButton) findViewById(R.id.checkBoxRegexp);
        this.saveFileButton = (ImageView) findViewById(R.id.saveBtn);
        this.configButton = (ImageView) findViewById(R.id.configBtn);
        this.editorLayout = this.editorInnerLayout;
        this.documentListButton = (ImageView) findViewById(R.id.panel_button);
        this.replaceButton = (ImageButton) findViewById(R.id.replaceBtn);
        this.scrollView = this.editorScrollView;
        this.textEditor.setWrapped(this.textWrap);
        this.textEditor.setInputType(721041);
        this.fontSize = 12;
        changeFontSize(this.fontSize);
        String strContent = this.curDocument.getText();
        if (strContent != null) {
            this.textEditor.setText(strContent);
            syntaxHighlight(0, strContent.length(), true);
        }
        this.openFindButton.setOnClickListener(this);
        this.findButton.setOnClickListener(this);
        this.replaceButton.setOnClickListener(this);
        this.saveFileButton.setOnClickListener(this);
        this.configButton.setOnClickListener(this);
        this.saveFileButton.getDrawable().setAlpha(80);
        this.saveFileButton.setClickable(false);
        this.replaceButton.setEnabled(false);
        ColorTheme colorTheme = new ColorTheme(this);
        this.editorInnerLayout.setBackgroundColor(colorTheme.getBackgroundColor());
        this.editorOuterLayout.setBackgroundColor(colorTheme.getBackgroundColor());
        this.lineNumbers.setBackgroundColor(colorTheme.getGutterColor());
        this.textEditor.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setTextColor(colorTheme.getForeground());
        this.lineNumbers.setTextColor(colorTheme.getLineNumberColor());
        this.lineDivider.setBackgroundColor(colorTheme.getDividerColor());
        this.textEditor.setBracketSpanColor(getInverseColor(colorTheme.getBackgroundColor()));
        this.textEditor.setCurrentLineHighlightColor(colorTheme.getCurrentLineColor());
        this.textEditor.setIndentGuideColor(colorTheme.getIndentGuideColor());
        this.editorStatus.attachEditor(this.textEditor);
        this.lineNumbers.setOnLongClickListener(null);
    }

    private void setupOnclickListener() {
        this.documentListDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            @Override
            public final void onDrawerOpened() {
                EditorActivity.this.adjustOpenedDrawer();
            }
        });
        this.documentListDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public final void onDrawerClosed() {
                EditorActivity.this.adjustClosedDrawer();
            }
        });
        this.textEditor.setTextSelectionListener(new TextSelectionListener() {
            @Override
            public final void selectionChanged(int i, int i2) {
                EditorActivity.this.m400xffa81011(i, i2);
            }
        });
        this.textEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Document document = EditorActivity.this.getCurrentDocument();
                boolean wasChanged = document.changed();
                if (!EditorActivity.this.autoTextChange) {
                    document.textChanged(s, start, before, count);
                }
                EditorActivity.this.syntaxHighlight(Math.min(EditorActivity.this.hlStart, start), Math.max(before, count) + start, true);
                EditorActivity.this.updateUndoRedoState();
                EditorActivity.this.updateLineCount(true);
                if (!wasChanged && document.changed()) {
                    EditorActivity.this.updateSaveState();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
        this.editorScrollView.setScrollViewListener(new ScrollViewListener() {
            @Override
            public final void onScrollChanged(ObScrollView obScrollView, int i, int i2, int i3, int i4) {
                EditorActivity.this.m401xe4e97ed2(obScrollView, i, i2, i3, i4);
            }
        });
        this.findText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == 1 && keyCode == 66) {
                    EditorActivity.this.executeFindWrapAction(true);
                    return true;
                }
                if (EditorActivity.this.documentListDrawer.getVisibility() == 0 && event.getAction() == 1 && keyCode == 4) {
                    EditorActivity.this.documentListDrawer.close();
                    return true;
                }
                EditorActivity.this.updateReplaceState();
                return false;
            }
        });
        this.replaceText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public final boolean onKey(View view, int i, KeyEvent keyEvent) {
                return EditorActivity.this.m402xca2aed93(view, i, keyEvent);
            }
        });
    }

    void m400xffa81011(int selStart, int selEnd) {
        this.editorStatus.updatePosition(selStart);
        if (this.documentListDrawer.isOpened()) {
            updateReplaceState();
        }
    }

    void m401xe4e97ed2(ObScrollView scrollView, int x, int y, int oldx, int oldy) {
        syntaxHighlight(-1, -1, false);
    }

    boolean m402xca2aed93(View v, int keyCode, KeyEvent event) {
        if (this.documentListDrawer.getVisibility() == 0 && event.getAction() == 1 && keyCode == 4) {
            this.documentListDrawer.close();
            return true;
        }
        updateReplaceState();
        return false;
    }

    private int getInverseColor(int background) {
        return Color.argb(128, 255 - Color.red(background), 255 - Color.green(background), 255 - Color.blue(background));
    }

    protected void updateLineCount(boolean withDelay) {
        if (this.updateLineCount) {
            this.mHandler.removeCallbacks(this.ulcTask);
            if (withDelay) {
                this.mHandler.postDelayed(this.ulcTask, LINECOUNTDELAY);
                return;
            } else {
                this.mHandler.postDelayed(this.ulcTask, 0L);
                return;
            }
        }
        if (this.editorHorizontalLayout != null && String.valueOf(this.previousLineCount).length() != String.valueOf(this.textEditor.getLineCount()).length()) {
            this.editorHorizontalLayout.requestLayout();
        }
        this.previousLineCount = this.textEditor.getLineCount();
    }

    protected void syntaxHighlight(int start, int end, boolean change) {
        if (!this.syntaxHighlighting || this.textEditor == null || this.curDocument == null) {
            return;
        }
        if (this.asyncSyntax == null || this.asyncSyntax.document() != this.curDocument) {
            if (this.asyncSyntax != null) {
                this.asyncSyntax.close();
            }
            this.asyncSyntax = new AsyncSyntaxController(this.curDocument, this.textEditor);
        }
        this.asyncSyntax.request(change);
    }

    protected void onDestroy() {
        if (this.asyncSyntax != null) {
            this.asyncSyntax.close();
        }
        super.onDestroy();
    }

    public Document getCurrentDocument() {
        return this.curDocument;
    }

    public void changeLineNumbers(boolean updateLineCount, int nd) {
        if (!updateLineCount || nd != this.previousMaxDigits) {
            this.updateLineCount = updateLineCount;
            LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.lineNumbers.getLayoutParams();
            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) this.lineDivider.getLayoutParams();
            if (updateLineCount) {
                this.lineNumbers.setVisibility(0);
                this.lineDivider.setVisibility(0);
                String digits = "";
                for (int i = 0; i < nd; i++) {
                    digits = String.valueOf(digits) + "9";
                }
                this.previousMaxDigits = nd;
                lp1.width = ((int) this.textEditor.getPaint().measureText(digits)) + ((int) TypedValue.applyDimension(1, 14.0f, getResources().getDisplayMetrics()));
                lp2.width = 1;
            } else {
                this.lineNumbers.setVisibility(8);
                this.lineDivider.setVisibility(8);
                lp1.width = 0;
                lp2.width = 0;
                this.previousMaxDigits = 0;
            }
            this.lineNumbers.requestLayout();
            this.lineDivider.requestLayout();
        }
    }

    protected synchronized void updateSaveState() {
        Document document = getCurrentDocument();
        boolean docChanged = document.changed();
        if (this.lastChangedState != docChanged) {
            if (docChanged) {
                this.saveFileButton.getDrawable().setAlpha(ParserRule.MAJOR_ACTIONS);
                this.saveFileButton.setClickable(true);
            } else {
                this.saveFileButton.getDrawable().setAlpha(80);
                this.saveFileButton.setClickable(false);
            }
            this.saveFileButton.invalidate();
            this.lastChangedState = docChanged;
        }
    }

    protected synchronized void updateUndoRedoState() {
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.menu_back) {
            onBackPressed();
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
        } else if (id == R.id.saveBtn) {
            executeSaveAction();
        } else {
            int i = R.id.configBtn;
        }
    }

    private void executeSaveAction() {
        ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            public void process() throws Exception {
                EditorActivity.this.curDocument.save((Context) EditorActivity.this, false);
                EditorActivity.this.curDocument.setChanged(false);
                if (EditorActivity.this.realFilePath != null) {
                    EditorActivity.this.copyBack2RealPath();
                }
                EditorActivity.this.setResult(1);
            }

            public void afterProcess() {
                EditorActivity.this.updateSaveState();
                EditorActivity.this.modifySaved = true;
                EditorActivity.this.setResult();
            }
        }, this.resId_fileSaved);
        dlg.show();
    }

    protected void copyBack2RealPath() {
        RefInvoke.invokeStaticMethod("com.gmail.heagoo.appdm.util.FileCopyUtil", "copyBack", new Class[]{Context.class, String.class, String.class, Boolean.TYPE}, new Object[]{this, this.filePath, this.realFilePath, Boolean.valueOf(this.isRootMode)});
    }

    protected void setResult() {
        Intent intent = new Intent();
        intent.putExtra("xmlPath", this.filePath);
        setResult(1, intent);
    }

    private void openFindDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int i = Build.VERSION.SDK_INT;
        if (!prefs.getBoolean("hideDocumentList", false)) {
        }
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
        int start = Math.min(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd());
        int end = Math.max(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd());
        if (start < end) {
            String selection = this.textEditor.getText().subSequence(start, end).toString();
            if (!selection.contains(Document.LINE_SEPARATOR_UNIX)) {
                this.findText.setText(selection);
            }
        }
        this.findText.requestFocus();
    }

    public boolean executeFindWrapAction(boolean toast) {
        if (this.findText.getText().toString().equals("")) {
            return false;
        }
        if (executeFindAction()) {
            this.textEditor.requestFocus();
            return true;
        }
        int selStart = this.textEditor.getSelectionStart();
        int selEnd = this.textEditor.getSelectionEnd();
        this.textEditor.setSelection(0);
        if (executeFindAction()) {
            this.textEditor.requestFocus();
            return true;
        }
        this.textEditor.setSelection(selStart, selEnd);
        if (!toast) {
            return false;
        }
        Toast.makeText(getApplicationContext(), getString(this.resId_notFound), 0).show();
        return false;
    }

    private boolean executeFindAction() {
        int flags = 8;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = 10;
        }
        if (this.toggleRegularExpression != null && !this.toggleRegularExpression.isChecked()) {
            flags |= 16;
        }
        try {
            Matcher matcher = Pattern.compile(this.findText.getText().toString(), flags).matcher(getCurrentDocument().getText().toString());
            if (matcher.find(this.textEditor.getSelectionEnd())) {
                this.textEditor.setSelection(matcher.start(), matcher.end());
                return true;
            }
            this.textEditor.setSelection(this.textEditor.getSelectionEnd());
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void adjustOpenedDrawer() {
        FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) this.editorLayout.getLayoutParams();
        int width = this.documentListDrawer.getWidth();
        lp1.setMargins(0, 0, width, lp1.bottomMargin);
        this.editorLayout.requestLayout();
        if (this.textWrap) {
            adjustWrappedTextEditor(true);
            updateLineCount(false);
        }
        this.documentListButton.setImageResource(R.drawable.edit_slide_right);
    }

    public void adjustClosedDrawer() {
        this.documentListDrawer.setVisibility(8);
        FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) this.editorLayout.getLayoutParams();
        lp1.setMargins(0, 0, 0, lp1.bottomMargin);
        this.editorLayout.requestLayout();
        if (this.textWrap) {
            adjustWrappedTextEditor(false);
            updateLineCount(false);
        }
        this.documentListButton.setImageResource(R.drawable.edit_slide_left);
        this.docFindAnim.setDisplayedChild(0);
    }

    public void updateReplaceState() {
        Pattern pattern;
        try {
            int start = Math.min(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd());
            int end = Math.max(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd());
            if (start != end) {
                String selected = this.textEditor.getText().subSequence(start, end).toString();
                String toFind = this.findText.getText().toString();
                if (this.toggleIgnoreCase.isChecked()) {
                    pattern = Pattern.compile(toFind, 10);
                } else {
                    pattern = Pattern.compile(toFind);
                }
                if (pattern.matcher(selected).matches()) {
                    this.replaceButton.setEnabled(true);
                    return;
                }
            }
        } catch (Exception e) {
        }
        this.replaceButton.setEnabled(false);
    }

    private void executeReplaceAction() {
        this.textEditor.getEditableText().replace(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd(), this.replaceText.getText());
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
        this.lineNumbers.setTextSize(2, fontSize);
        this.previousMaxDigits = 0;
    }

    private void adjustWrappedTextEditor(boolean openedDrawer) {
        if (openedDrawer) {
            int editorLayoutWidth = this.editorInnerLayout.getWidth();
            int lineNumberWidth = this.lineNumbers.getWidth();
            int dividerWidth = this.lineDivider.getWidth();
            int docListBtnWidth = this.documentListButton.getWidth();
            this.textEditor.setMaxWidth(((editorLayoutWidth - lineNumberWidth) - dividerWidth) - docListBtnWidth);
            return;
        }
        this.textEditor.setMaxWidth(((this.editorInnerLayout.getWidth() - this.lineNumbers.getWidth()) - this.lineDivider.getWidth()) - this.documentListDrawer.getWidth());
    }

    protected void updateEditor(boolean setSelection) {
        final Document document = getCurrentDocument();
        this.autoTextChange = true;
        this.textEditor.setText(document.getText());
        this.autoTextChange = false;
        if (setSelection) {
            int ss = document.getSelectionStart();
            int se = document.getSelectionEnd();
            Handler handler = new Handler();
            this.textEditor.setSelectionNoHack(ss, se);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    EditorActivity.this.scrollView.scrollTo(document.getScrollPositionX(), document.getScrollPositionY());
                }
            }, 400L);
        }
        updateLineCount(true);
        updateUndoRedoState();
        updateSaveState();
    }

    class UpdateLineCount extends TimerTask {
        UpdateLineCount() {
        }

        @Override
        public void run() {
            try {
                if (EditorActivity.this.textWrap) {
                    updateWrap();
                } else {
                    updateNoWrap();
                }
            } catch (Exception e) {
            }
            EditorActivity.this.previousLineCount = EditorActivity.this.textEditor.getLineCount();
        }

        private void updateWrap() {
            int expected2;
            String[] lines = (String.valueOf(EditorActivity.this.textEditor.getText().toString()) + "\nEND").split(Document.LINE_SEPARATOR_UNIX);
            EditorActivity.this.changeLineNumbers(EditorActivity.this.updateLineCount, getNumberDigits(lines.length));
            int offset = 0;
            StringBuilder sb = new StringBuilder();
            int expected = 0;
            int i = 0;
            while (i < lines.length - 1) {
                while (true) {
                    expected2 = expected + 1;
                    if (expected >= EditorActivity.this.textEditor.getLineNumber(offset)) {
                        break;
                    }
                    sb.append('\n');
                    expected = expected2;
                }
                sb.append(i + 1);
                sb.append('\n');
                offset += lines[i].length() + 1;
                i++;
                expected = expected2;
            }
            EditorActivity.this.lineNumbers.setText(sb.toString());
        }

        protected int getNumberDigits(int lines) {
            int nd = 1;
            while (lines >= 10) {
                lines /= 10;
                nd++;
            }
            return Math.max(2, nd);
        }

        private void updateNoWrap() {
            int lineCount = EditorActivity.this.textEditor.getLineCount();
            if (lineCount == 0) {
                lineCount = 1;
            }
            EditorActivity.this.changeLineNumbers(EditorActivity.this.updateLineCount, getNumberDigits(lineCount));
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                sb.append(i);
                sb.append('\n');
            }
            EditorActivity.this.lineNumbers.setText(sb.toString());
            EditorActivity.this.lineNumbers.requestLayout();
        }
    }

    class SyntaxHighLight extends TimerTask {
        SyntaxHighLight() {
        }

        @Override
        public void run() {
            Document document = EditorActivity.this.getCurrentDocument();
            Rect r = new Rect();
            if (EditorActivity.this.textEditor.getLocalVisibleRect(r)) {
                doHighlighting(document, r);
            } else {
                highlightingOnLayout(document, r);
            }
            EditorActivity.this.hlStart = -1;
            EditorActivity.this.hlEnd = -1;
            EditorActivity.this.hlChange = false;
        }

        private void highlightingOnLayout(final Document document, final Rect r) {
            document.syntaxHighlight(EditorActivity.this.textEditor, EditorActivity.this.hlStart, EditorActivity.this.hlEnd, -1, -1, EditorActivity.this.hlChange, EditorActivity.this.getApplicationContext());
            EditorActivity.this.textEditor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    EditorActivity.this.textEditor.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    if (EditorActivity.this.textEditor.getLocalVisibleRect(r)) {
                        SyntaxHighLight.this.doHighlighting(document, r);
                    }
                }
            });
        }

        public void doHighlighting(Document document, Rect r) {
            try {
                int lstart = r.top / EditorActivity.this.textEditor.getLineHeight();
                int lend = (r.top + EditorActivity.this.getWindowManager().getDefaultDisplay().getHeight()) / EditorActivity.this.textEditor.getLineHeight();
                if (EditorActivity.this.textWrap) {
                    int rlstart = 0;
                    int rlend = 0;
                    try {
                        String[] lines = EditorActivity.this.textEditor.getText().toString().split("\\n");
                        int offset = 0;
                        for (int i = 0; i < lines.length; i++) {
                            int line = EditorActivity.this.textEditor.getLineNumber(offset);
                            offset += lines[i].length() + 1;
                            if (line <= lstart) {
                                rlstart = i;
                            }
                            if (line <= lend) {
                                rlend = i;
                            }
                        }
                        lstart = rlstart;
                        lend = rlend;
                    } catch (Exception e) {
                        return;
                    }
                }
                try {
                    document.syntaxHighlight(EditorActivity.this.textEditor, EditorActivity.this.hlStart, EditorActivity.this.hlEnd, lstart - EditorActivity.this.highlightSize, lend + EditorActivity.this.highlightSize, EditorActivity.this.hlChange, EditorActivity.this.getApplicationContext());
                } catch (Exception e2) {
                }
            } catch (Exception e3) {
            }
        }
    }
}
