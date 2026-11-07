package com.gmail.heagoo.apkeditor.pro;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.smali.smaliFlexLexer;
import com.android.tools.smali.smali.smaliParser;
import com.android.tools.smali.smali.smaliTreeWalker;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTreeNodeStream;

/* loaded from: /tmp/apkx/classes26.dex */
public final class SmaliCheck {

    private static final class InterruptibleReader extends FilterReader {
        InterruptibleReader(Reader in) {
            super(in);
        }

        @Override // java.io.FilterReader, java.io.Reader
        public int read(char[] chars, int start, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Smali check cancelled");
            }
            return super.read(chars, start, len);
        }

        @Override // java.io.FilterReader, java.io.Reader
        public int read() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Smali check cancelled");
            }
            return super.read();
        }
    }

    public static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Smali check cancelled");
        }
    }

    public static String[] check(String path, int api) throws Exception {
        checkInterruptedAtEntry();
        final List<String> errors = new ArrayList<>();
        File file = new File(path);
        try {
            Reader reader = new InterruptibleReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            try {
                final smaliFlexLexer lexer = new smaliFlexLexer(reader, api);
                lexer.setSourceFile(file);
                TokenSource cancellableTokens = new TokenSource() {
                    public String getSourceName() {
                        return lexer.getSourceName();
                    }

                    public Token nextToken() {
                        SmaliCheck.checkInterrupted();
                        Token t = lexer.nextToken();
                        SmaliCheck.checkInterrupted();
                        return t;
                    }
                };
                CommonTokenStream tokens = new CommonTokenStream(cancellableTokens);
                smaliParser parser = new smaliParser(tokens) {
                    public void displayRecognitionError(String[] names, RecognitionException e) {
                        errors.add(e.line + ":" + getErrorMessage(e, names));
                    }
                };
                parser.setApiLevel(api);
                parser.setAllowOdex(false);
                parser.setVerboseErrors(false);
                smaliParser.smali_file_return parsed = parser.smali_file();
                if (lexer.getNumberOfSyntaxErrors() > 0) {
                    errors.add("0:Лексических ошибок smali: " + lexer.getNumberOfSyntaxErrors());
                }
                if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
                    if (errors.isEmpty()) {
                        errors.add("0:Ошибка синтаксиса smali");
                    }
                    String[] strArr = (String[]) errors.toArray(new String[0]);
                    reader.close();
                    return strArr;
                }
                CommonTreeNodeStream tree = new CommonTreeNodeStream(parsed.getTree()) {
                    public Object nextElement() {
                        SmaliCheck.checkInterrupted();
                        return super.nextElement();
                    }
                };
                tree.setTokenStream(tokens);
                smaliTreeWalker walker = new smaliTreeWalker(tree) {
                    public void displayRecognitionError(String[] names, RecognitionException e) {
                        errors.add(e.line + ":" + getErrorMessage(e, names));
                    }
                };
                walker.setApiLevel(api);
                walker.setVerboseErrors(false);
                walker.setDexBuilder(new DexBuilder(Opcodes.forApi(api)));
                walker.smali_file();
                if (walker.getNumberOfSyntaxErrors() > 0 && errors.isEmpty()) {
                    errors.add("0:Ошибка сборки smali");
                }
                checkInterrupted();
                String[] strArr2 = (String[]) errors.toArray(new String[0]);
                reader.close();
                return strArr2;
            } finally {
            }
        } catch (CancellationException cancelled) {
            InterruptedIOException stop = new InterruptedIOException("Smali check cancelled");
            stop.initCause(cancelled);
            throw stop;
        }
    }

    private static void checkInterruptedAtEntry() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Smali check cancelled");
        }
    }
}
