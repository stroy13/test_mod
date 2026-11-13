package com.gmail.heagoo.apkeditor.plus;

import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * DEX decoder used by the editor. LegacyDexDisassembler reports completed classes
 * through the progress listener, rather than a timer or file polling.
 * The single-argument entry point remains available to comparison tools.
 */
public class LegacyDexDecoder {
    private final String dexFilePath;
    private BiConsumer<Integer, Integer> progressListener;
    private String strWarning;

    public LegacyDexDecoder(String dexFilePath) {
        this.dexFilePath = dexFilePath;
    }

    public void setProgressListener(BiConsumer<Integer, Integer> listener) {
        this.progressListener = listener;
    }

    protected DexBackedDexFile loadDexFile(String input) throws Exception {
        File file = new File(input);
        if (!file.exists() || file.isDirectory()) {
            throw new Exception("Can't find file: " + input);
        }
        // Let dexlib recognize the DEX version/API instead of forcing the old API 15.
        return DexFileFactory.loadDexFile(file, (Opcodes) null);
    }

    public void dex2smali(String outputDirectory) throws Exception {
        decode(loadDexFile(dexFilePath), outputDirectory);
    }

    public void dexStream2smali(InputStream input, String outputDirectory) throws Exception {
        decode(DexBackedDexFile.fromInputStream(null, new BufferedInputStream(input, 65536)),
                outputDirectory);
    }

    private void decode(DexBackedDexFile dexFile, String outputDirectory) throws Exception {
        if (dexFile.supportsOptimizedOpcodes()) {
            strWarning = "You are disassembling an odex file without deodexing it. You won't be able to re-assemble the results unless you deodex it";
        }
        File output = new File(outputDirectory);
        if (!output.exists() && !output.mkdirs()) {
            throw new Exception("Can't create the output directory " + outputDirectory);
        }
        BaksmaliOptions options = getOptions();
        options.apiLevel = dexFile.getOpcodes().api;
        LegacyDexDisassembler.disassemble(dexFile, output, LegacyDexDisassembler.workerCount(),
                options, progressListener);
        // Preserve the v175 per-directory API marker used by project diagnostics.
        File marker = new File(output, ".apkeditor-dex-api");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(marker),
                StandardCharsets.UTF_8)) {
            writer.write(Integer.toString(dexFile.getOpcodes().api));
        }
    }

    private BaksmaliOptions getOptions() {
        BaksmaliOptions options = new BaksmaliOptions();
        options.parameterRegisters = true;
        options.localsDirective = false;
        options.sequentialLabels = false;
        options.debugInfo = true;
        options.codeOffsets = false;
        options.accessorComments = true;
        options.implicitReferences = false;
        options.normalizeVirtualMethods = false;
        options.registerInfo = 0;
        return options;
    }

    public String getWarning() {
        return strWarning;
    }
}
