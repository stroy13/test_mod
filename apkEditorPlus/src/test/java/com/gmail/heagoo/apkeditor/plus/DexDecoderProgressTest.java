package com.gmail.heagoo.apkeditor.plus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Runs the real smali -> DEX -> smali path, not a mocked progress timer. */
public class DexDecoderProgressTest {
    private static final int CLASS_COUNT = 130; // Crosses the decoder's 128-class batch boundary.

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    private File makeDex() throws Exception {
        File source = temporary.newFolder("smali-source");
        for (int i = 0; i < CLASS_COUNT; i++) {
            String name = String.format("Class%03d", i);
            String smali = ".class public Ltest/" + name + ";\n.super Ljava/lang/Object;\n.source \"" + name + ".java\"\n";
            Files.write(new File(source, name + ".smali").toPath(), smali.getBytes(StandardCharsets.UTF_8));
        }
        File dex = temporary.newFile("classes.dex");
        DexEncoder.smali2Dex(source.getAbsolutePath(), dex.getAbsolutePath(), (done, total) -> { });
        return dex;
    }

    private static long countSmali(File directory) throws Exception {
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            return paths.filter(p -> p.toString().endsWith(".smali")).count();
        }
    }

    @Test(timeout = 60000)
    public void reportsRealCompletedClassesAndKeepsReflectionEntrypoints() throws Exception {
        File dex = makeDex();
        File output = temporary.newFolder("smali-output");
        // AsyncDecodeTask lives in the library module and looks these methods up by name.
        assertNotNull(DexDecoder.class.getMethod("setProgressListener", BiConsumer.class));
        assertNotNull(DexDecoder.class.getMethod("dex2smali", String.class));
        assertNotNull(DexDecoder.class.getMethod("getWarning"));

        List<int[]> updates = new ArrayList<>();
        DexDecoder decoder = new DexDecoder(dex.getAbsolutePath());
        decoder.setProgressListener((done, total) -> updates.add(new int[]{done, total}));
        decoder.dex2smali(output.getAbsolutePath());

        assertEquals(CLASS_COUNT, countSmali(output));
        assertTrue("expected at least two real progress batches", updates.size() >= 2);
        int previous = 0;
        for (int[] update : updates) {
            assertEquals(CLASS_COUNT, update[1]);
            assertTrue("counter must advance", update[0] > previous);
            assertTrue("counter must not exceed the total", update[0] <= update[1]);
            previous = update[0];
        }
        assertEquals(CLASS_COUNT, previous);
        // The marker stores the API selected by dexlib for the DEX version; the
        // exact mapping can evolve, so only require a valid modern API value.
        String apiMarker = new String(Files.readAllBytes(new File(output, ".apkeditor-dex-api").toPath()),
                StandardCharsets.UTF_8);
        assertTrue("invalid API marker: " + apiMarker, apiMarker.matches("\\d+"));
        assertTrue(Integer.parseInt(apiMarker) >= 15);
        assertNull(decoder.getWarning());
    }

    @Test(timeout = 60000)
    public void streamAndSingleArgumentEntrypointsRemainCompatible() throws Exception {
        File dex = makeDex();
        File output = temporary.newFolder("smali-stream");
        DexDecoder decoder = new DexDecoder(dex.getAbsolutePath());
        try (InputStream input = new FileInputStream(dex)) {
            decoder.dexStream2smali(input, output.getAbsolutePath());
        }
        assertEquals(CLASS_COUNT, countSmali(output));
        File legacyOutput = temporary.newFolder("smali-no-listener");
        new DexDecoder(dex.getAbsolutePath()).dex2smali(legacyOutput.getAbsolutePath());
        assertEquals(CLASS_COUNT, countSmali(legacyOutput));
    }
}
