package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.widget.EditText;
import com.gmail.heagoo.neweditor.SyntaxSnapshot;
import com.gmail.heagoo.neweditor.TokenMarker;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;
import java.util.function.BooleanSupplier;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class Document implements Serializable {
    public static final String LINE_SEPARATOR_UNIX = "\n";
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";
    private static int undoLevel = 64;
    private transient ArrayList<TokenMarker.LineContext> baseToken;
    private boolean clearNext;
    private ColorTheme colorTheme;
    private int lastSHLineEnd;
    private int lastSHLineStart;
    private File mFile;
    private transient TokenMarker mTokenMarker;
    private int selectionEnd;
    private int selectionStart;
    private String syntaxName;
    protected boolean changed = false;
    protected String text = null;
    private Vector<Change> redo = new Vector<>();
    private int scrollPositionX = 0;
    private int scrollPositionY = 0;
    private Vector<Change> undo = new Vector<>();

    public Document(Context context, File file, String syntaxFileName) {
        this.mFile = file;
        this.syntaxName = syntaxFileName;
        if (this.syntaxName == null) {
            this.syntaxName = getSyntaxName(file.getName());
        }
        this.colorTheme = new ColorTheme(context, this.syntaxName);
        XmlSyntaxParser xmh = new XmlSyntaxParser();
        try {
            XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            try {
                InputSource isrc = new InputSource(new BufferedInputStream(context.getAssets().open("syntax/" + this.syntaxName)));
                parser.setContentHandler(xmh);
                parser.setDTDHandler(xmh);
                parser.setEntityResolver(xmh);
                parser.setErrorHandler(xmh);
                parser.parse(isrc);
                this.mTokenMarker = xmh.getTokenMarker();
            } catch (Throwable th) {
            }
        } catch (ParserConfigurationException e2) {
            e2.printStackTrace();
        } catch (SAXException saxe) {
            saxe.printStackTrace();
        }
    }

    public static void setUndoLevel(int undoLevel2) {
    }

    private String getSyntaxName(String fileName) {
        int pos = fileName.lastIndexOf(46);
        if (pos != -1) {
            String suffix = fileName.substring(pos + 1);
            if (suffix.equals("htm")) {
                suffix = "html";
            }
            return suffix + ".xml";
        }
        return "txt.xml";
    }

    public void setSelection(int selectionStart, int selectionEnd) {
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
    }

    public int getSelectionStart() {
        return this.selectionStart;
    }

    public int getSelectionEnd() {
        return this.selectionEnd;
    }

    public void textChanged(CharSequence s, int start, int before, int count) {
        try {
            String oldText = this.text;
            this.text = s.toString();
            Change change = new Change(start, oldText.substring(start, start + before), this.text.substring(start, start + count));
            if (this.undo.size() > 0 && this.undo.lastElement().compatible(change)) {
                this.undo.lastElement().add(change);
                while (undoLevel != 0 && this.undo.size() > undoLevel) {
                    this.undo.remove(0);
                }
                this.redo.clear();
                this.changed = true;
            }
            this.undo.addElement(change);
            while (undoLevel != 0) {
                this.undo.remove(0);
            }
            this.redo.clear();
            this.changed = true;
        } catch (Exception e) {
        }
    }

    public void load(Context context, String path, int stringId) throws IOException {
        int numRead;
        File f = new File(path);
        FileInputStream fis = new FileInputStream(f);
        long size = f.length();
        if (size > 4194304) {
            throw new IOException(context.getString(stringId));
        }
        byte[] bts = new byte[(int) size];
        int offset = 0;
        while (offset < bts.length && (numRead = fis.read(bts, offset, bts.length - offset)) >= 0) {
            offset += numRead;
        }
        this.text = new String(bts, Encoding.DEFAULT_CHARSET_NAME);
        this.changed = false;
        fis.close();
    }

    private boolean isLetter(int i) {
        char c = this.text.charAt(i);
        return Character.isLetter(c) || ((c == '\'' || c == '-') && surroundedByLetters(i));
    }

    private boolean surroundedByLetters(int i) {
        return i > 0 && i < this.text.length() - 1 && Character.isLetter(this.text.charAt(i + (-1))) && Character.isLetter(this.text.charAt(i + 1));
    }

    public String getText() {
        return this.text;
    }

    public String toString() {
        if (this.changed) {
            return getName() + " *";
        }
        return getName();
    }

    public String getName() {
        if (this.mFile != null) {
            return this.mFile.getName();
        }
        return "untitled";
    }

    public String getSyntaxName() {
        return this.syntaxName;
    }

    public String getPath() {
        if (this.mFile == null) {
            return null;
        }
        return this.mFile.getPath();
    }

    public Change undo() {
        int selection = 0;
        Change change = null;
        if (!this.undo.isEmpty()) {
            Change change2 = this.undo.lastElement();
            change = change2;
            this.undo.removeElement(change);
            String textBefore = this.text.substring(0, change.getStart());
            this.text = String.valueOf(textBefore) + change.getOldText() + this.text.substring(change.getStart() + change.getNewText().length());
            this.redo.insertElementAt(change, 0);
            this.changed = true;
            selection = change.getStart() + change.getOldText().length();
        }
        this.selectionStart = selection;
        this.selectionEnd = selection;
        return change;
    }

    public Change redo() {
        int selection = 0;
        Change change = null;
        if (!this.redo.isEmpty()) {
            Change change2 = this.redo.firstElement();
            change = change2;
            this.redo.removeElement(change);
            String textBefore = this.text.substring(0, change.getStart());
            this.text = String.valueOf(textBefore) + change.getNewText() + this.text.substring(change.getStart() + change.getOldText().length());
            this.undo.addElement(change);
            this.changed = true;
            selection = change.getStart() + change.getNewText().length();
        }
        this.selectionEnd = selection;
        this.selectionStart = selection;
        return change;
    }

    public boolean canUndo() {
        return this.undo.size() > 0;
    }

    public boolean changed() {
        return this.changed;
    }

    public boolean canRedo() {
        return this.redo.size() > 0;
    }

    public void save(Context context, boolean rootMode) throws Exception {
        save(this.mFile.getAbsolutePath(), context);
    }

    public void save(String path, Context context) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(path));
        OutputStreamWriter osw = new OutputStreamWriter(fos, Encoding.DEFAULT_CHARSET_NAME);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(this.text);
        bw.flush();
        osw.close();
        fos.close();
    }

    int m378lambda$syntaxSnapshot$0$comgmailheagooneweditorDocument(Token token) {
        return this.colorTheme.getForegroundColor(token);
    }

    public SyntaxSnapshot syntaxSnapshot(String text, BooleanSupplier cancelled) {
        return SyntaxSnapshot.parse(text, this.mTokenMarker, new SyntaxSnapshot.Color() {
            @Override
            public final int color(Token token) {
                return Document.this.m378lambda$syntaxSnapshot$0$comgmailheagooneweditorDocument(token);
            }
        }, cancelled);
    }

    public void syntaxHighlight(EditText textEditor, int start, int end, int lstart, int lend, boolean change, Context context) {
        try {
            if (this.mTokenMarker == null) {
                if (this.clearNext) {
                    clearSyntaxHighlighting(textEditor);
                }
                this.clearNext = false;
                return;
            }
            Spannable spannable = textEditor.getText();
            String[] lines = spannable.toString().split("\n");
            Segment segment = new Segment();
            TokenMarker.LineContext lastTokenType = null;
            int pos = 0;
            for (int l = 0; l < lines.length; l++) {
                TokenMarker.LineContext initialTokenType = lastTokenType;
                if (l >= this.lastSHLineStart && l <= this.lastSHLineEnd && (l < lstart || l > lend)) {
                    clearSyntaxHighlighting(spannable, pos, pos + lines[l].length());
                }
                boolean cached = getBaseToken(l) == lastTokenType;
                if ((change || l < lstart || l > lend) && (cached || l < lstart || l > lend)
                        && !(change && start <= pos + lines[l].length() && end >= pos)) {
                    lastTokenType = getBaseToken(l + 1);
                    pos += lines[l].length() + 1;
                    continue;
                }
                clearSyntaxHighlighting(spannable, pos, pos + lines[l].length());
                DefaultTokenHandler dth = new DefaultTokenHandler();
                segment.array = lines[l].toCharArray();
                segment.offset = 0;
                segment.count = lines[l].length();
                TokenMarker.LineContext newTokenType = this.mTokenMarker.markTokens(lastTokenType, dth, segment);
                for (Token token = TokenMerger.merge(dth.getTokens()); token != null; token = token.next) {
                    if (token.length > 0 && token.id != 0 && l >= lstart && l <= lend) {
                        setSpan(spannable, new SyntaxHighlightSpan(this.colorTheme.getForegroundColor(token)),
                                pos + token.offset, pos + token.offset + token.length, 33);
                    }
                }
                setBaseToken(l, initialTokenType);
                lastTokenType = newTokenType;
                pos += lines[l].length() + 1;
            }
            this.lastSHLineStart = lstart;
            this.lastSHLineEnd = lend;
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    public void syntaxHighlight(EditText textEditor) {
        try {
            if (this.mTokenMarker == null) {
                if (this.clearNext) {
                    clearSyntaxHighlighting(textEditor);
                }
                this.clearNext = false;
                return;
            }
            Spannable spannable = textEditor.getText();
            String[] lines = spannable.toString().split("\n");
            Segment segment = new Segment();
            TokenMarker.LineContext lastTokenType = null;
            int pos = 0;
            for (int l = 0; l < lines.length; l++) {
                TokenMarker.LineContext initialTokenType = lastTokenType;
                clearSyntaxHighlighting(spannable, pos, pos + lines[l].length());
                DefaultTokenHandler dth = new DefaultTokenHandler();
                segment.array = lines[l].toCharArray();
                segment.offset = 0;
                segment.count = lines[l].length();
                TokenMarker.LineContext newTokenType = this.mTokenMarker.markTokens(lastTokenType, dth, segment);
                for (Token token = TokenMerger.merge(dth.getTokens()); token != null; token = token.next) {
                    if (token.length > 0 && token.id != 0) {
                        setSpan(spannable, new SyntaxHighlightSpan(this.colorTheme.getForegroundColor(token)),
                                pos + token.offset, pos + token.offset + token.length, 33);
                    }
                }
                setBaseToken(l, initialTokenType);
                lastTokenType = newTokenType;
                pos += lines[l].length() + 1;
            }
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    private void setBaseToken(int line, TokenMarker.LineContext initialTokenType) {
        if (line >= 0) {
            if (this.baseToken == null) {
                this.baseToken = new ArrayList<>();
            }
            while (line >= this.baseToken.size()) {
                this.baseToken.add(null);
            }
            this.baseToken.set(line, initialTokenType);
        }
    }

    private TokenMarker.LineContext getBaseToken(int line) {
        if (this.baseToken == null) {
            this.baseToken = new ArrayList<>();
        }
        if (line >= this.baseToken.size()) {
            return null;
        }
        return this.baseToken.get(line);
    }

    private void setSpan(Spannable spannable, CharacterStyle style, int i, int j, int flags) {
        spannable.setSpan(style, i, j, flags);
    }

    public void clearSyntaxHighlighting(EditText textEditor) {
        clearSyntaxHighlighting(textEditor.getText(), 0, this.text.length());
    }

    public void clearSyntaxHighlighting(Spannable spannable, int start, int end) {
        Object[] toRemoveSpans = spannable.getSpans(start, end, SyntaxHighlightSpan.class);
        for (Object removeSpan : toRemoveSpans) {
            spannable.removeSpan(removeSpan);
        }
    }

    public String getExtension() {
        if (this.mFile == null || this.mFile.getName() == null || !this.mFile.getName().contains(".")) {
            return null;
        }
        return this.mFile.getName().substring(this.mFile.getName().lastIndexOf(46) + 1);
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getNameNoExtension() {
        String name = getName();
        if (name.contains(".")) {
            return name.substring(0, name.lastIndexOf(46));
        }
        return name;
    }

    public String getNumberWords() {
        return getNumberWords(0, this.text.length());
    }

    public String getNumberWords(int start, int end) {
        if (start > end) {
            start = end;
            end = start;
        }
        if (start == end) {
            start = 0;
            end = this.text.length();
        }
        int words = 0;
        int lines = 1;
        boolean wasLastWhiteSpace = true;
        for (int i = Math.max(0, start); i < Math.min(this.text.length(), end); i++) {
            if (wasLastWhiteSpace && isLetter(i)) {
                words++;
            }
            if (isLetter(i)) {
                wasLastWhiteSpace = false;
            } else {
                wasLastWhiteSpace = true;
            }
            if (this.text.charAt(i) == '\n') {
                lines++;
            }
        }
        return String.valueOf(lines) + " / " + words + " / " + (end - start);
    }

    public void setScrollPosition(int x, int y) {
        this.scrollPositionX = x;
        this.scrollPositionY = y;
    }

    public int getScrollPositionX() {
        return this.scrollPositionX;
    }

    public int getScrollPositionY() {
        return this.scrollPositionY;
    }

    public void setFile(File file) {
        this.mFile = file;
    }
}
