package com.gmail.heagoo.apkeditor.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class BuildLogTailTest {
    @Rule public TemporaryFolder dir = new TemporaryFolder();
    @Test public void readsIncrementallyWithUtf8AndReset() throws Exception {
        Path file=dir.newFile("build-log.txt").toPath(); BuildLogTail tail=new BuildLogTail();
        Files.write(file,"one\nпр".getBytes(StandardCharsets.UTF_8));
        assertEquals(Collections.singletonList("one"),tail.read(file.toFile()));
        Files.write(file,"ивет\nlast\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        assertEquals(Arrays.asList("привет","last"),tail.read(file.toFile()));
        assertTrue(tail.read(file.toFile()).isEmpty());
        Files.write(file,"new\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(Arrays.asList("\u0000RESET","new"),tail.read(file.toFile()));
    }
}
