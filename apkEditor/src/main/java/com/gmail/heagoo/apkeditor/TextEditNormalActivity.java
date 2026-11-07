package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
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
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.editor.HtmlViewDialog;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.ICommonCallback;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.AsyncSyntaxController;
import com.gmail.heagoo.neweditor.ColorTheme;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.EditorStatusBar;
import com.gmail.heagoo.neweditor.ObEditText;
import com.gmail.heagoo.neweditor.ObScrollView;
import com.gmail.heagoo.neweditor.ParserRule;
import com.gmail.heagoo.neweditor.ScrollViewListener;
import com.gmail.heagoo.neweditor.TextSelectionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditNormalActivity extends TextEditBase implements View.OnClickListener, TextSelectionListener {
    private AsyncSyntaxController asyncSyntax;
    private boolean autoTextChange;
    private ImageView copyButton;
    private int curStartLine;
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
    private boolean enableSyntaxHighlight;
    private ImageButton findButton;
    private EditText findText;
    private int fontSize;
    private ImageButton goButton;
    private int highlightSize;
    private boolean hlChange;
    private int hlEnd;
    private int hlStart;
    private HtmlViewDialog htmlDialog;
    private boolean lastUndoState;
    private View lineDivider;
    private EditText lineNumEdit;
    private EditText lineNumbers;
    private Handler mHandler;
    private View methodMenu;
    private ImageView moreButton;
    private View nextMenu;
    private ImageView openFindButton;
    private Menu optionsMenu;
    private ImageView pasteButton;
    public int previousLineCount;
    private int previousMaxDigits;
    private View previousMenu;
    private ImageButton replaceAllButton;
    private ImageButton replaceButton;
    private EditText replaceText;
    private boolean saveBtnEnabled;
    private ImageView saveFileButton;
    private ScrollView scrollView;
    SyntaxHighLight shTask;
    private boolean syntaxHighlighting;
    protected ObEditText textEditor;
    private boolean textLoaded;
    private TextChangeListener textWatcher;
    private ToggleButton toggleIgnoreCase;
    private ToggleButton toggleRegularExpression;
    UpdateLineCount ulcTask;
    private boolean updateLineCount;
    private boolean wasOpenedDrawer;
    private static int AUTODELAY = 300;
    private static int LINECOUNTDELAY = 300;
    private static int TYPEDELAY = 400;
    private static int SCROLLDELAY = 40;

    public TextEditNormalActivity() {
        super(false, true);
        this.ulcTask = new UpdateLineCount();
        this.shTask = new SyntaxHighLight();
        this.syntaxHighlighting = true;
        this.hlChange = false;
        this.hlEnd = -1;
        this.hlStart = -1;
        this.highlightSize = 80;
        this.mHandler = new Handler();
        this.updateLineCount = true;
        this.wasOpenedDrawer = false;
        this.saveBtnEnabled = false;
        this.lastUndoState = true;
        this.enableSyntaxHighlight = true;
        this.textLoaded = false;
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

    public int countLines(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        int lines = 1;
        int pos = 0;
        while (true) {
            int indexOf = str.indexOf(Document.LINE_SEPARATOR_UNIX, pos) + 1;
            pos = indexOf;
            if (indexOf != 0) {
                lines++;
            } else {
                return lines;
            }
        }
    }

    public void debug(String format, Object... args) {
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
        if (this.textEditor != null) {
            String text = "" + getSpecialChars(null).charAt(idx);
            this.textEditor.getText().replace(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd(), text);
            this.textEditor.requestFocus();
        }
    }

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

    protected void onResume() {
        super.onResume();
        debug("enter onResume", new Object[0]);
        int fontSize = SettingEditorActivity.getFontSize((Context) this);
        if (this.fontSize != fontSize) {
            this.fontSize = fontSize;
            changeFontSize(fontSize);
            updateLineCount(true);
            syntaxHighlight(-1, -1, false);
            updateSaveState();
        }
        showHideLineNumbers();
        debug("exit onResume", new Object[0]);
    }

    private void showHideLineNumbers() {
        if (SettingEditorActivity.showLineNumbers(this)) {
            this.lineNumbers.setVisibility(0);
            this.lineDivider.setVisibility(0);
            if (this.textLoaded) {
                updateLineCount(false);
                return;
            }
            return;
        }
        this.lineNumbers.setVisibility(8);
        this.lineDivider.setVisibility(8);
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

    private void initEditView() {
        this.editorInnerLayout = (LinearLayout) findViewById(R.id.editorLayout);
        this.editorOuterLayout = (FrameLayout) findViewById(R.id.center_layout);
        this.lineNumbers = (EditText) findViewById(R.id.lineNumbers);
        this.lineNumbers.setFilters(new InputFilter[0]);
        this.textEditor = (ObEditText) findViewById(R.id.editor);
        this.textEditor.setFilters(new InputFilter[0]);
        this.lineDivider = findViewById(R.id.divider);
        this.editorScrollView = (ObScrollView) findViewById(R.id.editorScrollview);
        this.editorHorizontalLayout = (HorizontalScrollView) findViewById(R.id.hScrollView);
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
        this.editorLayout = this.editorInnerLayout;
        this.documentListButton = (ImageView) findViewById(R.id.panel_button);
        this.scrollView = this.editorScrollView;
        enableButton(this.copyButton, false);
        this.textEditor.setWrapped(this.textWrap);
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
            public void onDrawerOpened() {
                TextEditNormalActivity.this.adjustOpenedDrawer();
            }
        });
        this.documentListDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                TextEditNormalActivity.this.adjustClosedDrawer();
            }
        });
        this.textEditor.setTextSelectionListener(this);
        this.textWatcher = new TextChangeListener();
        this.textEditor.addTextChangedListener(this.textWatcher);
        this.editorScrollView.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObScrollView scrollView, int x, int y, int oldx, int oldy) {
                TextEditNormalActivity.this.debug("posted scroll message to do syntax highlight", new Object[0]);
                TextEditNormalActivity.this.syntaxHighlight(-1, -1, false);
            }
        });
        this.findText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == 1 && keyCode == 66) {
                    TextEditNormalActivity.this.executeFindWrapAction(true);
                    return true;
                }
                if (TextEditNormalActivity.this.documentListDrawer.getVisibility() == 0 && event.getAction() == 1 && keyCode == 4) {
                    TextEditNormalActivity.this.documentListDrawer.close();
                    return true;
                }
                TextEditNormalActivity.this.updateReplaceState();
                return false;
            }
        });
        this.replaceText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (TextEditNormalActivity.this.documentListDrawer.getVisibility() == 0 && event.getAction() == 1 && keyCode == 4) {
                    TextEditNormalActivity.this.documentListDrawer.close();
                    return true;
                }
                TextEditNormalActivity.this.updateReplaceState();
                return false;
            }
        });
        this.textEditor.setOnTouchListener(new View.OnTouchListener() {
            private boolean bPossibleEvent = false;
            private long downTime;
            private float downX;
            private float downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 0) {
                    this.bPossibleEvent = false;
                    int selectionStart = TextEditNormalActivity.this.textEditor.getSelectionStart();
                    int selectionEnd = TextEditNormalActivity.this.textEditor.getSelectionEnd();
                    if (selectionStart != selectionEnd) {
                        Layout layout = ((EditText) v).getLayout();
                        float x = event.getX() + TextEditNormalActivity.this.textEditor.getScrollX();
                        float y = event.getY() + TextEditNormalActivity.this.textEditor.getScrollY();
                        int line = layout.getLineForVertical((int) y);
                        int offset = layout.getOffsetForHorizontal(line, x);
                        if (offset >= selectionStart && offset < selectionEnd && !TextEditNormalActivity.this.isInputMethodShown()) {
                            this.bPossibleEvent = true;
                            this.downX = event.getX();
                            this.downY = event.getY();
                            this.downTime = System.currentTimeMillis();
                            return true;
                        }
                    }
                } else if (event.getAction() != 2 && event.getAction() == 1) {
                    if (this.bPossibleEvent) {
                        float upX = event.getX();
                        float upY = event.getY();
                        long upTime = System.currentTimeMillis();
                        if (Math.abs(upX - this.downX) < 32.0f && Math.abs(upY - this.downY) < 32.0f && upTime - this.downTime < 500) {
                            TextEditNormalActivity.this.showInputMethod();
                            return true;
                        }
                    }
                    this.bPossibleEvent = false;
                }
                return false;
            }
        });
        super.setupInputMethodMonitor(this.textEditor);
    }

    @Override
    public void selectionChanged(int selStart, int selEnd) {
        this.editorStatus.updatePosition(selStart);
        updateCopyState(selStart, selEnd);
        if (this.documentListDrawer.isOpened()) {
            debug("updateReplaceState starts", new Object[0]);
            updateReplaceState();
            debug("updateReplaceState ends", new Object[0]);
        }
    }

    private int getInverseColor(int background) {
        return Color.argb(BinaryDiffIndex.PAGE_ROWS, 255 - Color.red(background), 255 - Color.green(background), 255 - Color.blue(background));
    }

    protected void updateLineCount(boolean withDelay) {
        if (this.curStartLine > 0) {
            String[] lines = this.textEditor.getText().toString().split(Document.LINE_SEPARATOR_UNIX);
            int charIndex = 0;
            int line = this.curStartLine > lines.length ? lines.length : this.curStartLine;
            for (int i = 0; i < line - 1; i++) {
                charIndex += lines[i].length() + 1;
            }
            this.textEditor.setSelection(charIndex);
            this.textEditor.requestFocus();
            hideInputMethod();
            this.curStartLine = -1;
        }
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

    private void hideInputMethod() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showInputMethod() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
            imm.showSoftInput(view, 0);
        }
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

    public void changeLineNumbers(boolean updateLineCount, int nd) {
        if (!updateLineCount || nd != this.previousMaxDigits) {
            this.updateLineCount = updateLineCount;
            LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.lineNumbers.getLayoutParams();
            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) this.lineDivider.getLayoutParams();
            if (updateLineCount) {
                if (SettingEditorActivity.showLineNumbers(this)) {
                    this.lineNumbers.setVisibility(0);
                    this.lineDivider.setVisibility(0);
                }
                String digits = "";
                for (int i = 0; i < nd; i++) {
                    digits = digits + "9";
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
                if (this.optionsMenu != null) {
                    this.optionsMenu.findItem(R.id.action_save).setEnabled(true);
                }
            } else {
                enableButton(this.saveFileButton, false);
                if (this.optionsMenu != null) {
                    this.optionsMenu.findItem(R.id.action_save).setEnabled(false);
                }
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

    protected synchronized void updateUndoRedoState() {
    }

    private void doReloadWithCheck(final int fileIdxOffset) {
        Document document = this.curDocument;
        boolean docChanged = document.changed();
        if (docChanged) {
            new AlertDialog.Builder(this).setMessage(R.string.save_changes_tip).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditNormalActivity.this.executeSaveAction(new ICommonCallback() {
                        public void doCallback() {
                            TextEditNormalActivity.this.curFileIndex += fileIdxOffset;
                            new TextLoader().execute(new Void[0]);
                        }
                    });
                }
            }).setNegativeButton(R.string.donot_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditNormalActivity.this.curFileIndex += fileIdxOffset;
                    new TextLoader().execute(new Void[0]);
                }
            }).setNeutralButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        } else {
            this.curFileIndex += fileIdxOffset;
            new TextLoader().execute(new Void[0]);
        }
    }

    private void doCopySelectedText() {
        int start = this.textEditor.getSelectionStart();
        int end = this.textEditor.getSelectionEnd();
        String selected = this.textEditor.getText().toString().substring(start, end);
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

    private void doPaste() {
        String text = ClipboardUtil.getText(this);
        if (text == null) {
            Toast.makeText((Context) this, R.string.clipboard_no_text, 0).show();
        } else {
            this.textEditor.getText().replace(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd(), text);
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
        try {
            Matcher matcher = Pattern.compile(this.findText.getText().toString(), flags).matcher(this.curDocument.getText());
            List<Location> occurences = new ArrayList<>();
            int startIdx = 0;
            while (matcher.find(startIdx)) {
                int start = matcher.start();
                int end = matcher.end();
                occurences.add(new Location(start, end));
                startIdx = end;
            }
            if (!occurences.isEmpty()) {
                String replacing = this.replaceText.getText().toString();
                int delta = 0;
                for (int i = 0; i < occurences.size(); i++) {
                    Location loc = occurences.get(i);
                    this.textEditor.getEditableText().replace(loc.start + delta, loc.end + delta, replacing);
                    delta += replacing.length() - (loc.end - loc.start);
                }
                Toast.makeText(getApplicationContext(), String.format(getString(R.string.replace_all_ret), Integer.valueOf(occurences.size())), 0).show();
                return;
            }
            Toast.makeText(getApplicationContext(), getString(R.string.not_found), 0).show();
        } catch (Exception e) {
        }
    }

    private void gotoButtonClicked() {
        String str = this.lineNumEdit.getText().toString().trim();
        if (str.equals("")) {
            this.textEditor.getText().insert(this.textEditor.getSelectionStart(), Document.LINE_SEPARATOR_UNIX);
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
            String[] lines = this.textEditor.getText().toString().split(Document.LINE_SEPARATOR_UNIX);
            int charIndex = 0;
            int lineNO2 = lineNO > lines.length ? lines.length : lineNO;
            for (int i = 0; i < lineNO2 - 1; i++) {
                charIndex += lines[i].length() + 1;
            }
            this.lineNumEdit.setText("");
            this.textEditor.setSelection(charIndex);
            this.textEditor.requestFocus();
        }
    }

    public void executeSaveAction(final ICommonCallback callback) {
        ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            @Override
            public void process() throws Exception {
                TextEditNormalActivity.this.curDocument.save((Context) TextEditNormalActivity.this, false);
                TextEditNormalActivity.this.curDocument.setChanged(false);
            }

            @Override
            public void afterProcess() {
                TextEditNormalActivity.this.updateSaveState();
                TextEditNormalActivity.this.modifySaved = true;
                TextEditNormalActivity.this.setResult();
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
        int start = Math.min(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd());
        int end = Math.max(this.textEditor.getSelectionStart(), this.textEditor.getSelectionEnd());
        if (start < end) {
            String selection = this.textEditor.getText().subSequence(start, end).toString();
            if (!selection.contains(Document.LINE_SEPARATOR_UNIX)) {
                this.findText.setText(selection);
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
        Toast.makeText(getApplicationContext(), getString(R.string.not_found), 0).show();
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
            Matcher matcher = Pattern.compile(this.findText.getText().toString(), flags).matcher(this.curDocument.getText());
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
            int width = ((editorLayoutWidth - lineNumberWidth) - dividerWidth) - docListBtnWidth;
            this.textEditor.setMaxWidth(width);
            return;
        }
        int width2 = ((this.editorInnerLayout.getWidth() - this.lineNumbers.getWidth()) - this.lineDivider.getWidth()) - this.documentListDrawer.getWidth();
        this.textEditor.setMaxWidth(width2);
    }

    @Override
    public void insertString(String str) {
        int index = this.textEditor.getSelectionStart();
        Editable edit = this.textEditor.getEditableText();
        if (index < 0 || index >= edit.length()) {
            edit.append((CharSequence) str);
        } else {
            edit.insert(index, str);
        }
    }

    public void onBackPressed() {
        if (this.curDocument == null || !this.curDocument.changed()) {
            finish();
        } else {
            new AlertDialog.Builder(this).setMessage(R.string.save_changes_tip).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditNormalActivity.this.executeSaveAction(new ICommonCallback() {
                        public void doCallback() {
                            TextEditNormalActivity.this.finish();
                        }
                    });
                }
            }).setNegativeButton(R.string.donot_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextEditNormalActivity.this.finish();
                }
            }).setNeutralButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        }
    }

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
                int startIndex = 0;
                for (int i = 1; i < from; i++) {
                    startIndex += lines[i - 1].length() + 1;
                }
                int endIndex = startIndex;
                for (int i2 = from; i2 <= end; i2++) {
                    endIndex += lines[i2 - 1].length() + 1;
                }
                this.textEditor.getText().replace(startIndex, endIndex, "");
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
                StringBuilder sb = new StringBuilder();
                int startIndex2 = 0;
                for (int i3 = 1; i3 < from; i3++) {
                    startIndex2 += lines2[i3 - 1].length() + 1;
                }
                int endIndex2 = startIndex2;
                for (int i4 = from; i4 <= end2; i4++) {
                    endIndex2 += lines2[i4 - 1].length() + 1;
                }
                boolean isUnComment = allStartsWith(lines2, from, end2, "#");
                if (isUnComment) {
                    for (int i5 = from; i5 <= end2; i5++) {
                        int pos = lines2[i5 - 1].indexOf(35);
                        if (pos > 0) {
                            sb.append(lines2[i5 - 1].substring(0, pos));
                        }
                        sb.append(lines2[i5 - 1].substring(pos + 1));
                        sb.append(Document.LINE_SEPARATOR_UNIX);
                    }
                } else {
                    for (int i6 = from; i6 <= end2; i6++) {
                        sb.append("#");
                        sb.append(lines2[i6 - 1]);
                        sb.append(Document.LINE_SEPARATOR_UNIX);
                    }
                }
                this.textEditor.getText().replace(startIndex2, endIndex2, sb.toString());
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

    class UpdateLineCount extends TimerTask {
        UpdateLineCount() {
        }

        @Override
        public void run() {
            TextEditNormalActivity.this.debug("updateLineCount starts", new Object[0]);
            if (!SettingEditorActivity.showLineNumbers(TextEditNormalActivity.this)) {
                TextEditNormalActivity.this.previousLineCount = 0;
                return;
            }
            if (TextEditNormalActivity.this.textWrap) {
                updateWrap();
            } else {
                updateNoWrap();
            }
            TextEditNormalActivity.this.previousLineCount = TextEditNormalActivity.this.textEditor.getLineCount();
            TextEditNormalActivity.this.debug("updateLineCount ends", new Object[0]);
        }

        private void updateWrap() {
            int expected2;
            String[] lines = TextEditNormalActivity.this.textEditor.getText().toString().split(Document.LINE_SEPARATOR_UNIX);
            TextEditNormalActivity.this.changeLineNumbers(TextEditNormalActivity.this.updateLineCount, getNumberDigits(lines.length + 1));
            int offset = 0;
            StringBuilder sb = new StringBuilder();
            int expected = 0;
            int i = 0;
            while (i < lines.length) {
                while (true) {
                    expected2 = expected + 1;
                    if (expected >= TextEditNormalActivity.this.textEditor.getLineNumber(offset)) {
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
            TextEditNormalActivity.this.lineNumbers.setText(sb.toString());
        }

        public int getNumberDigits(int lines) {
            int nd = 1;
            while (lines >= 10) {
                lines /= 10;
                nd++;
            }
            return Math.max(2, nd);
        }

        private void updateNoWrap() {
            int lineCount = TextEditNormalActivity.this.textEditor.getLineCount();
            if (lineCount == 0) {
                lineCount = 1;
            }
            TextEditNormalActivity.this.changeLineNumbers(TextEditNormalActivity.this.updateLineCount, getNumberDigits(lineCount));
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                sb.append(i);
                sb.append('\n');
            }
            TextEditNormalActivity.this.lineNumbers.setText(sb.toString());
            TextEditNormalActivity.this.lineNumbers.requestLayout();
        }
    }

    class SyntaxHighLight extends TimerTask {
        SyntaxHighLight() {
        }

        @Override
        public void run() {
            TextEditNormalActivity.this.debug("SyntaxHighLight starts", new Object[0]);
            if (!TextEditNormalActivity.this.enableSyntaxHighlight) {
                return;
            }
            Document document = TextEditNormalActivity.this.curDocument;
            Rect r = new Rect();
            if (TextEditNormalActivity.this.textEditor.getLocalVisibleRect(r)) {
                doHighlighting(document, r);
            } else {
                highlightingOnLayout(document, r);
            }
            TextEditNormalActivity.this.hlStart = -1;
            TextEditNormalActivity.this.hlEnd = -1;
            TextEditNormalActivity.this.hlChange = false;
            TextEditNormalActivity.this.debug("SyntaxHighLight ends", new Object[0]);
        }

        private void highlightingOnLayout(final Document document, final Rect r) {
            if (!TextEditNormalActivity.this.enableSyntaxHighlight) {
                return;
            }
            document.syntaxHighlight(TextEditNormalActivity.this.textEditor, TextEditNormalActivity.this.hlStart, TextEditNormalActivity.this.hlEnd, -1, -1, TextEditNormalActivity.this.hlChange, TextEditNormalActivity.this.getApplicationContext());
            TextEditNormalActivity.this.textEditor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    TextEditNormalActivity.this.textEditor.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    if (TextEditNormalActivity.this.textEditor.getLocalVisibleRect(r)) {
                        SyntaxHighLight.this.doHighlighting(document, r);
                    }
                }
            });
        }

        public void doHighlighting(Document document, Rect r) {
            if (!TextEditNormalActivity.this.enableSyntaxHighlight) {
                return;
            }
            try {
                int lineHeight = TextEditNormalActivity.this.textEditor.getLineHeight();
                int lstart = r.top / lineHeight;
                int lend = r.bottom / lineHeight;
                if (TextEditNormalActivity.this.textWrap) {
                    int rlstart = 0;
                    int rlend = 0;
                    try {
                        String[] lines = TextEditNormalActivity.this.textEditor.getText().toString().split("\\n");
                        int offset = 0;
                        for (int i = 0; i < lines.length; i++) {
                            int line = TextEditNormalActivity.this.textEditor.getLineNumber(offset);
                            offset += lines[i].length() + 1;
                            if (line <= lstart) {
                                rlstart = i;
                            }
                            if (line > lend) {
                                break;
                            }
                            rlend = i;
                        }
                        lstart = rlstart;
                        lend = rlend;
                    } catch (Exception e) {
                        return;
                    }
                }
                int lstart2 = lstart - TextEditNormalActivity.this.highlightSize;
                int lend2 = lend + TextEditNormalActivity.this.highlightSize;
                System.currentTimeMillis();
                try {
                    document.syntaxHighlight(TextEditNormalActivity.this.textEditor, TextEditNormalActivity.this.hlStart, TextEditNormalActivity.this.hlEnd, lstart2, lend2, TextEditNormalActivity.this.hlChange, TextEditNormalActivity.this.getApplicationContext());
                    System.currentTimeMillis();
                } catch (Exception e2) {
                }
            } catch (Exception e3) {
            }
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
            this.curFilePath = TextEditNormalActivity.this.fileList.get(TextEditNormalActivity.this.curFileIndex);
            TextEditNormalActivity.this.curStartLine = TextEditNormalActivity.this.startLineList.get(TextEditNormalActivity.this.curFileIndex).intValue();
            this.syntaxFileName = TextEditNormalActivity.this.syntaxFileList.get(TextEditNormalActivity.this.curFileIndex);
            TextEditNormalActivity.this.setCurrentFilePath(this.curFilePath);
            TextEditNormalActivity.this.editorStatus.setFileName(TextEditNormalActivity.this.getDisplayName(this.curFilePath));
            TextEditNormalActivity.this.editorStatus.setSyntaxFromFile(TextEditNormalActivity.this.getDisplayName(this.curFilePath), this.syntaxFileName);
            TextEditNormalActivity.this.editorStatus.setPosition(1, 1);
            TextEditNormalActivity.this.methodMenu.setVisibility((TextEditBase.isSmali(this.curFilePath) || TextEditBase.isJava(this.curFilePath)) ? 0 : 8);
            if (TextEditNormalActivity.this.curFileIndex >= TextEditNormalActivity.this.fileList.size() - 1) {
                TextEditNormalActivity.this.nextMenu.setVisibility(4);
            } else {
                TextEditNormalActivity.this.nextMenu.setVisibility(0);
            }
            if (TextEditNormalActivity.this.curFileIndex == 0) {
                TextEditNormalActivity.this.previousMenu.setVisibility(4);
            } else {
                TextEditNormalActivity.this.previousMenu.setVisibility(0);
            }
        }

        @Override
        public Boolean doInBackground(Void... params) {
            Document document = new Document(TextEditNormalActivity.this, new File(this.curFilePath), this.syntaxFileName);
            try {
                if (TextEditNormalActivity.this.unsavedFilePath != null) {
                    document.load(TextEditNormalActivity.this, TextEditNormalActivity.this.unsavedFilePath, R.string.error_file_too_big);
                    document.setChanged(true);
                    new File(TextEditNormalActivity.this.unsavedFilePath).delete();
                    TextEditNormalActivity.this.unsavedFilePath = null;
                } else {
                    document.load(TextEditNormalActivity.this, this.curFilePath, R.string.error_file_too_big);
                }
                TextEditNormalActivity.this.curDocument = document;
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
                TextEditNormalActivity.this.textLoaded = true;
                TextEditNormalActivity.this.textEditor.removeTextChangedListener(TextEditNormalActivity.this.textWatcher);
                String strContent = TextEditNormalActivity.this.curDocument.getText();
                TextEditNormalActivity.this.textEditor.setText(strContent);
                TextEditNormalActivity.this.editorStatus.updatePosition(TextEditNormalActivity.this.textEditor.getSelectionStart());
                TextEditNormalActivity.this.textEditor.getLineNumber(801639);
                TextEditNormalActivity.this.textEditor.addTextChangedListener(TextEditNormalActivity.this.textWatcher);
                int lines = TextEditNormalActivity.this.curDocument.getText().split(Document.LINE_SEPARATOR_UNIX).length + 1;
                int digits = TextEditNormalActivity.this.ulcTask.getNumberDigits(lines);
                TextEditNormalActivity.this.changeLineNumbers(true, digits);
                TextEditNormalActivity.this.updateLineCount(true);
                TextEditNormalActivity.this.syntaxHighlight(-1, -1, false);
                TextEditNormalActivity.this.updateSaveState();
                return;
            }
            Toast.makeText((Context) TextEditNormalActivity.this, (CharSequence) ("Failed to open " + this.curFilePath), 1).show();
        }
    }

    private class TextChangeListener implements TextWatcher {
        private TextChangeListener() {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Document document = TextEditNormalActivity.this.curDocument;
            if (document == null) {
                return;
            }
            boolean wasChanged = document.changed();
            if (!TextEditNormalActivity.this.autoTextChange) {
                document.textChanged(s, start, before, count);
            }
            TextEditNormalActivity.this.syntaxHighlight(Math.min(TextEditNormalActivity.this.hlStart, start), Math.max(before, count) + start, true);
            TextEditNormalActivity.this.updateUndoRedoState();
            TextEditNormalActivity.this.updateLineCount(true);
            if (!wasChanged && document.changed()) {
                TextEditNormalActivity.this.updateSaveState();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable e) {
        }
    }
}
