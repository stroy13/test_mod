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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTreeNodeStream;

public final class SmaliCheck {

    private static final BooleanSupplier NEVER = () -> false;

    private static final class InterruptibleReader extends FilterReader {
        private final BooleanSupplier cancelled;

        InterruptibleReader(Reader in, BooleanSupplier cancelled) {
            super(in);
            this.cancelled = cancelled;
        }

        @Override // java.io.FilterReader, java.io.Reader
        public int read(char[] chars, int start, int len) throws IOException {
            checkInterrupted(cancelled);
            return super.read(chars, start, len);
        }

        @Override // java.io.FilterReader, java.io.Reader
        public int read() throws IOException {
            checkInterrupted(cancelled);
            return super.read();
        }
    }

    public static void checkInterrupted() {
        checkInterrupted(NEVER);
    }

    public static void checkInterrupted(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || (cancelled != null && cancelled.getAsBoolean())) {
            throw new CancellationException("Smali check cancelled");
        }
    }

    public static String[] check(String path, int api) throws Exception {
        return check(path, api, NEVER);
    }

    public static String[] check(String path, int api, BooleanSupplier cancelled) throws Exception {
        BooleanSupplier stop = cancelled == null ? NEVER : cancelled;
        checkInterruptedAtEntry(stop);
        final List<String> errors = new ArrayList<>();
        File file = new File(path);
        try (Reader reader = new InterruptibleReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), stop)) {
            final smaliFlexLexer lexer = new smaliFlexLexer(reader, api);
            lexer.setSourceFile(file);
            TokenSource cancellableTokens = new TokenSource() {
                @Override
                public String getSourceName() {
                    return lexer.getSourceName();
                }

                @Override
                public Token nextToken() {
                    SmaliCheck.checkInterrupted(stop);
                    Token token = lexer.nextToken();
                    SmaliCheck.checkInterrupted(stop);
                    return token;
                }
            };
            CommonTokenStream tokens = new CommonTokenStream(cancellableTokens);
            smaliParser parser = new smaliParser(tokens) {
                @Override
                public void displayRecognitionError(String[] names, RecognitionException error) {
                    errors.add(error.line + ":" + getErrorMessage(error, names));
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
                return errors.toArray(new String[0]);
            }
            CommonTreeNodeStream tree = new CommonTreeNodeStream(parsed.getTree()) {
                @Override
                public Object nextElement() {
                    SmaliCheck.checkInterrupted(stop);
                    return super.nextElement();
                }
            };
            tree.setTokenStream(tokens);
            smaliTreeWalker walker = new smaliTreeWalker(tree) {
                @Override
                public void displayRecognitionError(String[] names, RecognitionException error) {
                    errors.add(error.line + ":" + getErrorMessage(error, names));
                }
            };
            walker.setApiLevel(api);
            walker.setVerboseErrors(false);
            walker.setDexBuilder(new DexBuilder(Opcodes.forApi(api)));
            walker.smali_file();
            if (walker.getNumberOfSyntaxErrors() > 0 && errors.isEmpty()) {
                errors.add("0:Ошибка сборки smali");
            }
            checkInterrupted(stop);
            return errors.toArray(new String[0]);
        } catch (CancellationException cancelled) {
            InterruptedIOException stop = new InterruptedIOException("Smali check cancelled");
            stop.initCause(cancelled);
            throw stop;
        }
    }

    private static void checkInterruptedAtEntry(BooleanSupplier cancelled) throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || (cancelled != null && cancelled.getAsBoolean())) {
            throw new InterruptedIOException("Smali check cancelled");
        }
    }
}
