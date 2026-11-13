package com.gmail.heagoo.apkeditor.plus;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SmaliToolVersionsTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private File fixture() throws Exception {
        File source = temp.newFolder();
        Files.write(new File(source, "Test.smali").toPath(), (".class public Lexample/Test;\n"
                + ".super Ljava/lang/Object;\n.field public static answer:I = 0x2a\n"
                + ".method public static value()I\n    .registers 1\n    const/16 v0, 0x2a\n    return v0\n.end method\n").getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private Set<String> types(File dex) throws Exception {
        Set<String> types = new HashSet<>();
        for (ClassDef cls : DexFileFactory.loadDexFile(dex, null).getClasses()) {
            types.add(cls.getType());
            assertTrue(cls.getFields().iterator().hasNext());
            assertTrue(cls.getMethods().iterator().hasNext());
        }
        return types;
    }

    @Test(timeout = 60000) public void bothEncodersAndDecodersRoundTrip() throws Exception {
        File source = fixture();
        for (int encoder = 0; encoder < 2; encoder++) {
            File dex = new File(temp.getRoot(), "source" + encoder + ".dex");
            if (encoder == 0) DexEncoder.smali2Dex(source.getPath(), dex.getPath(), (a, b) -> {});
            else ModernDexEncoder.smali2Dex(source.getPath(), dex.getPath(), (a, b) -> {});
            assertEquals(java.util.Collections.singleton("Lexample/Test;"), types(dex));
            for (int decoder = 0; decoder < 2; decoder++) {
                File decoded = new File(temp.getRoot(), "decoded" + encoder + decoder);
                if (decoder == 0) new LegacyDexDecoder(dex.getPath()).dex2smali(decoded.getPath());
                else new DexDecoder(dex.getPath()).dex2smali(decoded.getPath());
                File rebuilt = new File(temp.getRoot(), "rebuilt" + encoder + decoder + ".dex");
                if (encoder == 0) DexEncoder.smali2Dex(decoded.getPath(), rebuilt.getPath(), (a, b) -> {});
                else ModernDexEncoder.smali2Dex(decoded.getPath(), rebuilt.getPath(), (a, b) -> {});
                assertEquals(types(dex), types(rebuilt));
            }
        }
    }

    @Test(timeout = 60000) public void bothEncodersRejectInvalidSmali() throws Exception {
        File source = temp.newFolder();
        Files.write(new File(source, "Invalid.smali").toPath(), "not a valid class".getBytes(StandardCharsets.UTF_8));
        for (int encoder = 0; encoder < 2; encoder++) {
            File dex = new File(temp.getRoot(), "bad" + encoder + ".dex");
            try {
                if (encoder == 0) DexEncoder.smali2Dex(source.getPath(), dex.getPath(), (a, b) -> {});
                else ModernDexEncoder.smali2Dex(source.getPath(), dex.getPath(), (a, b) -> {});
                fail("invalid smali was accepted");
            } catch (Exception expected) { assertFalse(dex.isFile()); }
        }
    }
}
