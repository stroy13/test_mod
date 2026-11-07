package com.gmail.heagoo.apkeditor;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFind9PatchChunk;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResValuesFile;
import brut.androlib.res.data.value.ResBoolValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.decoder.Res9patchStreamDecoder;
import brut.androlib.res.decoder.ResRawStreamDecoder;
import brut.androlib.res.decoder.ResStreamDecoderContainer;
import brut.androlib.res.util.ExtFile;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import brut.util.KXmlSerializer;
import com.gmail.heagoo.apkeditor.XmlDecoder;
import com.gmail.heagoo.apkeditor.util.TimeDumper;
import com.gmail.heagoo.apklib.AXMLParser;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.neweditor.Encoding;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ApkDecoderMine implements XmlDecoder.IReferenceDecoder, AXMLParser.IReferenceDecode {
    private byte[] dummy9Png;
    private byte[] dummyJpg;
    private byte[] dummyPng;
    private volatile int fallbackBatches;
    private boolean isFullDecoding;
    private String packageName;
    private volatile int parallelRawFiles;
    private volatile int parallelXmlFiles;
    private ProgressListener progressListener;
    private boolean protectedApk;
    private ResTable resTable;
    private boolean serialAfterResourceFailure;
    private volatile int serialFiles;
    private XmlDecoder xmlDecoder;
    private static final String[] APK_STANDARD_FILES = {"classes.dex", "AndroidManifest.xml", "resources.arsc"};
    private static final String[] APK_STANDARD_DIRS = {"res", "r", "R", "lib", "libs", "assets", "kotlin"};
    private ResStreamDecoderContainer mDecoders = new ResStreamDecoderContainer();
    private String resDirName = "";
    private volatile boolean stopRunning = false;
    private Map<String, String> fileEntry2ZipEntry = new HashMap();

    interface ProgressListener {
        void onCount(int i, int i2);

        void onProgress(int i);
    }

    String getWorkerStats() {
        return "Resource workers: XML=" + this.parallelXmlFiles + ", raw=" + this.parallelRawFiles + ", serial=" + this.serialFiles + ", replayed batches=" + this.fallbackBatches;
    }

    void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    private void progress(int phase) {
        if (this.progressListener != null) {
            this.progressListener.onProgress(phase);
        }
    }

    /**
     * Reports how many resources are already decoded. Used both for the on-screen
     * "N / M" counter and as a liveness signal for the decode watchdog.
     */
    private void progressCount(int done, int total) {
        if (this.progressListener != null) {
            this.progressListener.onCount(done, total);
        }
    }

    public ApkDecoderMine(ResTable resTable, byte[] dummyPng, byte[] dummyJpg, byte[] dummy9Png) {
        boolean z = false;
        this.resTable = resTable;
        this.dummyPng = dummyPng;
        this.dummyJpg = dummyJpg;
        this.dummy9Png = dummy9Png;
        if (dummyJpg == null && dummyPng == null) {
            z = true;
        }
        this.isFullDecoding = z;
        this.xmlDecoder = new XmlDecoder(this, getResPackage());
        this.mDecoders.setDecoder("xml", this.xmlDecoder);
        Res9patchStreamDecoder _9pathDecoder = new Res9patchStreamDecoder();
        this.mDecoders.setDecoder("9path", _9pathDecoder);
        ResRawStreamDecoder rawDecoder = new ResRawStreamDecoder();
        this.mDecoders.setDecoder("raw", rawDecoder);
    }

    private static String getPackageName(int id) {
        if ((id >>> 24) == 1) {
            return "android:";
        }
        return "";
    }

    public Map<String, String> getFileEntry2ZipEntry() {
        return this.fileEntry2ZipEntry;
    }

    public void decode(ExtFile apkFile, File outDir) throws Exception {
        Directory inApk = apkFile.getDirectory();
        if (!inApk.containsDir("res")) {
            this.protectedApk = true;
            this.xmlDecoder.setApkProtected(true);
        }
        progress(2);
        TimeDumper timer = new TimeDumper(false);
        if (!this.stopRunning) {
            this.packageName = decodeManifest(apkFile, outDir);
        }
        timer.lastTime("Manifest Decode Time");
        try {
            FileUtil.deleteAll(new File(outDir.getPath() + "/res"));
        } catch (Exception e) {
        }
        LOGGER.info(outDir.getPath() + "/res deleted!");
        File resDir = new File(outDir, "res");
        if (!resDir.exists()) {
            resDir.mkdirs();
        }
        timer.lastTime("Dir Prepare Time");
        if (!this.stopRunning) {
            progress(6);
            FileDirectory out = new FileDirectory(resDir);
            decodeFileResources(apkFile, out);
        }
        LOGGER.info("file-resources decoded!");
        timer.lastTime("Resource Decode Time");
        progress(3);
        if (!this.stopRunning) {
            decodeValues(resDir);
        }
        if (!this.stopRunning) {
            generatePublicXml(resDir);
        }
        timer.lastTime("Value Decode Time");
        if (this.isFullDecoding) {
            progress(4);
            if (!this.stopRunning) {
                decodeRawFiles(apkFile, outDir);
            }
            if (!this.stopRunning) {
                decodeUnknownFiles(apkFile, outDir, this.resTable);
            }
        }
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir, ResTable resTable) throws AndrolibException {
        File unknownOut = new File(outDir, "unknown");
        try {
            Directory unk = apkFile.getDirectory();
            Set<String> zipEntries = new HashSet<>(this.fileEntry2ZipEntry.values());
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isDecodedFileNames(file) && !zipEntries.contains(file)) {
                    unk.copyToDir(unknownOut, file);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean isDecodedFileNames(String file) {
        for (String apkFile : APK_STANDARD_FILES) {
            if (apkFile.equals(file)) {
                return true;
            }
        }
        for (String apkFile2 : APK_STANDARD_DIRS) {
            if (file.startsWith(apkFile2 + "/")) {
                return true;
            }
        }
        return file.endsWith(".dex") && !file.contains("/");
    }

    public void decodeRawFiles(ExtFile apkFile, File outDir) throws AndrolibException {
        try {
            Directory in = apkFile.getDirectory();
            if (in.containsDir("assets")) {
                in.copyToDir(outDir, "assets");
            }
            if (in.containsDir("lib")) {
                in.copyToDir(outDir, "lib");
            }
            if (in.containsDir("libs")) {
                in.copyToDir(outDir, "libs");
            }
            if (in.containsDir("kotlin")) {
                in.copyToDir(outDir, "kotlin");
            }
            Set<String> files = in.getFiles(false);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    in.copyToDir(outDir, file);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void generatePublicXml(File outDir) throws AndrolibException {
        for (ResPackage pkg : this.resTable.listMainPackages()) {
            if (!this.stopRunning) {
                try {
                    File file = new File(outDir, "values/public.xml");
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    Writer writer = new BufferedWriter(new OutputStreamWriter(fos, Encoding.DEFAULT_CHARSET_NAME), 65536);
                    writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                    writer.write("<resources>\n");
                    for (ResResSpec spec : pkg.listResSpecs()) {
                        PublicXmlWriter.writeEntry(writer, spec.getType().getName(), spec.getName(), spec.getId().id);
                    }
                    writer.write("</resources>");
                    writer.close();
                    fos.close();
                } catch (IOException ex) {
                    throw new AndrolibException("Could not generate public.xml file", ex);
                }
            } else {
                return;
            }
        }
    }

    // Restored during source recovery: body taken from the upstream source of this
    // class (string constants and call structure verified against the v175 DEX).
    private void decode(ResResource res, Directory inDir, Directory outDir)
            throws AndrolibException {
        ResFileValue fileValue = (ResFileValue) res.getValue();
        String inFileName = fileValue.getStrippedPath();
        String outResName = res.getFilePath();
        String typeName = res.getResSpec().getType().getName();

        String ext = null;
        String outFileName;
        String filename = inFileName.substring(inFileName.lastIndexOf("/") + 1);
        int extPos = filename.lastIndexOf(".");
        if (extPos == -1) {
            outFileName = outResName;
        } else {
            ext = filename.substring(extPos);
            outFileName = outResName + ext;
        }

        LOGGER.info("decoding " + outFileName);

        try {
            if (typeName.equals("raw")) {
                decode(inDir, inFileName, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("font") && !".xml".equals(ext)) {
                decode(inDir, inFileName, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("drawable") || typeName.equals("mipmap")) {
                // Special case: no ext, maybe protected
                if (ext == null) {
                    InputStream input = null;
                    try {
                        byte[] buffer = new byte[12];
                        input = inDir.getFileInput(inFileName);
                        input.read(buffer);
                        // Binary xml
                        if (buffer[0] == 0x03 && buffer[1] == 0x00
                                && buffer[2] == 0x08 && buffer[3] == 0x00) {
                            decode(inDir, inFileName, outDir, outFileName + ".xml", "xml");
                        }
                        // PNG
                        else if ((buffer[0] & 0xff) == 0x89 && buffer[1] == 0x50
                                && buffer[2] == 0x4e && buffer[3] == 0x47) {
                            decode(inDir, inFileName, outDir,
                                    outFileName + ".png", "raw");
                        }
                        // JPG
                        else if ((buffer[0] & 0xff) == 0xff
                                && (buffer[1] & 0xff) == 0xd8
                                && (buffer[2] & 0xff) == 0xff
                                && (buffer[3] & 0xff) == 0xe0) {
                            decode(inDir, inFileName, outDir,
                                    outFileName + ".jpg", "raw");
                        } else {
                            decode(inDir, inFileName, outDir, outFileName,
                                    "raw");
                        }
                    } catch (Exception e) {
                    } finally {
                        this.closeQuietly(input);
                    }
                    return;
                }
                if (inFileName.toLowerCase().endsWith(".9.png")) {
                    outFileName = outResName + ".9" + ext;

                    // check for htc .r.9.png
                    if (inFileName.toLowerCase().endsWith(".r.9.png")) {
                        outFileName = outResName + ".r.9" + ext;
                    }

                    try {
                        decode(inDir, inFileName, outDir, outFileName, "9path");
                        return;
                    } catch (CantFind9PatchChunk ex) {
                        LOGGER.warning(String.format(
                                "Cant find 9path chunk in file: \"%s\". Renaming it to *.png.",
                                inFileName));
                        outDir.removeFile(outFileName);
                        outFileName = outResName + ext;
                    }
                }
                if (!".xml".equals(ext)) {
                    decode(inDir, inFileName, outDir, outFileName, "raw");
                    return;
                }
            }

            decode(inDir, inFileName, outDir, outFileName, "xml");
        } catch (AndrolibException e) {
            res.replace(new ResBoolValue(false, 0, null));
            // Added by Pujiang: remove in-correct decoded file
            outDir.removeFile(outFileName);
        }
    }

    public void decode(Directory inDir, String inFileName, Directory outDir, String outFileName, String decoder) throws AndrolibException {
        InputStream in = null;
        OutputStream out = null;
        if ("xml".equals(decoder) && !outFileName.endsWith(".xml")) {
            outFileName = outFileName + ".xml";
        }
        boolean dummyDecoded = false;
        try {
            try {
                if (!this.isFullDecoding && "raw".equals(decoder) && !"".equals(this.resDirName)) {
                    if (outFileName.endsWith(".jpg")) {
                        out = outDir.getFileOutput(outFileName);
                        out.write(this.dummyJpg);
                        dummyDecoded = true;
                    } else if (outFileName.endsWith(".png") && !outFileName.endsWith(".9.png")) {
                        out = outDir.getFileOutput(outFileName);
                        out.write(this.dummyPng);
                        dummyDecoded = true;
                    }
                }
                if (!dummyDecoded) {
                    in = inDir.getFileInput(inFileName);
                    out = outDir.getFileOutput(outFileName);
                    this.mDecoders.decode(in, out, decoder);
                }
                String _fileEntry = "res/" + outFileName;
                String _zipEntry = this.resDirName + inFileName;
                if (!_fileEntry.equals(_zipEntry)) {
                    this.fileEntry2ZipEntry.put(_fileEntry, _zipEntry);
                }
            } catch (Exception ex) {
                throw new AndrolibException(ex);
            }
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:127:0x021e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:69:0x013e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:86:0x01bb A[Catch: all -> 0x0207, TryCatch #3 {all -> 0x0207, blocks: (B:114:0x01ee, B:117:0x01fa, B:57:0x00f2, B:59:0x00fe, B:63:0x0130, B:65:0x0134, B:70:0x013e, B:72:0x014b, B:75:0x0152, B:76:0x0184, B:83:0x01a9, B:84:0x01b5, B:86:0x01bb, B:87:0x01be, B:89:0x01cf, B:92:0x01d7, B:95:0x0167, B:102:0x010d, B:104:0x011a, B:107:0x0124), top: B:113:0x01ee }] */
    /* JADX WARN: Removed duplicated region for block: B:89:0x01cf A[Catch: all -> 0x0207, TryCatch #3 {all -> 0x0207, blocks: (B:114:0x01ee, B:117:0x01fa, B:57:0x00f2, B:59:0x00fe, B:63:0x0130, B:65:0x0134, B:70:0x013e, B:72:0x014b, B:75:0x0152, B:76:0x0184, B:83:0x01a9, B:84:0x01b5, B:86:0x01bb, B:87:0x01be, B:89:0x01cf, B:92:0x01d7, B:95:0x0167, B:102:0x010d, B:104:0x011a, B:107:0x0124), top: B:113:0x01ee }] */
    /* JADX WARN: Removed duplicated region for block: B:99:0x01b3 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    // Restored during source recovery from the upstream source of this class.
    // NOTE: the v175 original used parallel raw-resource workers (RawCandidate);
    // this restored version decodes serially (equivalent output, slower).
    private void decodeFileResources(ExtFile apkFile, Directory outDir)
            throws AndrolibException {
        Directory inDir = null;
        try {
            Directory inApk = apkFile.getDirectory();
            if (inApk.containsDir("res")) {
                inDir = inApk.getDir("res");
                this.resDirName = "res/";
            }

            if (inDir == null && inApk.containsDir("r")) {
                inDir = inApk.getDir("r");
                this.resDirName = "r/";
            }

            if (inDir == null && inApk.containsDir("R")) {
                inDir = inApk.getDir("R");
                this.resDirName = "R/";
            }

            if (inDir == null) {
                inDir = inApk;
            }

        } catch (Exception e1) {
            throw new AndrolibException(e1);
        }

        for (ResPackage pkg : resTable.listMainPackages()) {
            if (stopRunning) {
                break;
            }

            LOGGER.info("Decoding file-resources...");
            Collection<ResResource> fileResources = pkg.listFiles();
            int total = fileResources.size();
            int done = 0;
            for (ResResource res : fileResources) {
                if (stopRunning) {
                    break;
                }

                decode(res, inDir, outDir);
                done++;
                if ((done & 7) == 0 || done == total) {
                    progressCount(done, total);
                }
            }
            if (total > 0) {
                progressCount(done, total);
            }
        }
    }

    private static final class RawCandidate {
        final byte[] dummy;
        final String inputName;
        final String outputName;
        final boolean xml;

        RawCandidate(String inputName, String outputName, byte[] dummy, boolean xml) {
            this.inputName = inputName;
            this.outputName = outputName;
            this.dummy = dummy;
            this.xml = xml;
        }
    }

    private static final class RawTask {
        final RawCandidate candidate;
        final Future<Void> future;
        final ResResource resource;

        RawTask(ResResource resource, RawCandidate candidate, Future<Void> future) {
            this.resource = resource;
            this.candidate = candidate;
            this.future = future;
        }
    }

    private static boolean conflicts(Set<String> paths, String name) {
        for (String path : paths) {
            if (path.equals(name) || path.startsWith(name + "/") || name.startsWith(path + "/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean ordinaryPath(String name) {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.indexOf(92) >= 0) {
            return false;
        }
        int start = 0;
        for (int i = 0; i <= name.length(); i++) {
            if (i == name.length() || name.charAt(i) == '/') {
                int size = i - start;
                if (size == 0 || ((size == 1 && name.charAt(start) == '.') || (size == 2 && name.charAt(start) == '.' && name.charAt(start + 1) == '.'))) {
                    return false;
                }
                start = i + 1;
            }
        }
        return true;
    }

    private RawCandidate fileCandidate(ResResource resource, Directory inDir) throws AndrolibException {
        ResFileValue value = (ResFileValue) resource.getValue();
        String input = value.getStrippedPath();
        String name = input.substring(input.lastIndexOf(47) + 1);
        int dot = name.lastIndexOf(46);
        String ext = dot < 0 ? null : name.substring(dot);
        String type = resource.getResSpec().getType().getName();
        boolean raw = type.equals("raw") || (type.equals("font") && !".xml".equals(ext)) || !((!type.equals("drawable") && !type.equals("mipmap")) || ext == null || ".xml".equals(ext) || input.toLowerCase().endsWith(".9.png"));
        boolean xml = !raw && ".xml".equals(ext);
        if (!raw && (!xml || this.serialAfterResourceFailure)) {
            return null;
        }
        String output = resource.getFilePath() + (ext == null ? "" : ext);
        if (!ordinaryPath(input) || !ordinaryPath(output)) {
            return null;
        }
        byte[] dummy = null;
        if (raw && !this.isFullDecoding && !this.resDirName.isEmpty()) {
            if (output.endsWith(".jpg")) {
                if (this.dummyJpg == null) {
                    return null;
                }
                dummy = this.dummyJpg;
            } else if (output.endsWith(".png") && !output.endsWith(".9.png")) {
                if (this.dummyPng == null) {
                    return null;
                }
                dummy = this.dummyPng;
            }
        }
        if (dummy != null || inDir.containsFile(input)) {
            return new RawCandidate(input, output, dummy, xml);
        }
        return null;
    }

    private static boolean isXmlResource(ResResource resource) throws AndrolibException {
        String path = ((ResFileValue) resource.getValue()).getStrippedPath();
        String type = resource.getResSpec().getType().getName();
        if (type.equals("raw")) {
            return false;
        }
        if (!type.equals("drawable") && !type.equals("mipmap")) {
            return !type.equals("font") || path.endsWith(".xml");
        }
        String name = path.substring(path.lastIndexOf(47) + 1);
        return path.endsWith(".xml") || name.indexOf(46) < 0;
    }

    private int finishRawTasks(List<RawTask> tasks, Directory inDir, Directory outDir) throws AndrolibException {
        boolean failed = false;
        Iterator<RawTask> it = tasks.iterator();
        while (it.hasNext()) {
            try {
                it.next().future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AndrolibException(e);
            } catch (ExecutionException e2) {
                Throwable cause = e2.getCause();
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                failed = true;
            }
        }
        if (failed) {
            this.serialAfterResourceFailure = true;
            this.fallbackBatches++;
            Iterator<RawTask> it2 = tasks.iterator();
            while (it2.hasNext()) {
                decode(it2.next().resource, inDir, outDir);
                this.serialFiles++;
            }
        } else {
            for (RawTask task : tasks) {
                if (task.candidate.xml) {
                    this.parallelXmlFiles++;
                } else {
                    this.parallelRawFiles++;
                }
                String fileEntry = "res/" + task.candidate.outputName;
                String zipEntry = this.resDirName + task.candidate.inputName;
                if (!fileEntry.equals(zipEntry)) {
                    this.fileEntry2ZipEntry.put(fileEntry, zipEntry);
                }
            }
        }
        int done = tasks.size();
        tasks.clear();
        return done;
    }

    protected void decodeValues(File outDir) throws Exception {
        ApkDecoderMine apkDecoderMine = this;
        File file = outDir;
        Set<ResPackage> packages = apkDecoderMine.resTable.listMainPackages();
        for (ResPackage pkg : packages) {
            if (!apkDecoderMine.stopRunning) {
                Collection<ResValuesFile> files = pkg.listValuesFiles();
                int valueTotal = files.size();
                int valueDone = 0;
                for (ResValuesFile valueFile : files) {
                    if (apkDecoderMine.stopRunning) {
                        break;
                    }
                    String path = valueFile.getPath();
                    int pos = path.lastIndexOf(47);
                    if (pos != -1) {
                        File dir = new File(file, path.substring(0, pos));
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                    }
                    KXmlSerializer xmlSerializer = new KXmlSerializer();
                    File file2 = new File(file, path);
                    FileOutputStream fos = new FileOutputStream(file2);
                    Writer writer = new BufferedWriter(new OutputStreamWriter(fos, Encoding.DEFAULT_CHARSET_NAME), 32768);
                    xmlSerializer.setOutput(writer);
                    writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                    writer.write("<resources>\n");
                    Set<ResResource> resources = valueFile.listResources();
                    for (ResResource res : resources) {
                        if (!valueFile.isSynthesized(res)) {
                            ResValuesXmlSerializable value = (ResValuesXmlSerializable) res.getValue();
                            if (value instanceof ResValuesXmlSerializable) {
                                try {
                                    value.serializeToResValuesXml(xmlSerializer, res);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                            xmlSerializer.ignorableWhitespace("\n    ");
                        }
                    }
                    xmlSerializer.flush();
                    writer.write("</resources>\n");
                    writer.close();
                    fos.close();
                    apkDecoderMine = this;
                    file = outDir;
                    valueDone++;
                    if ((valueDone & 3) == 0 || valueDone == valueTotal) {
                        progressCount(valueDone, valueTotal);
                    }
                }
                apkDecoderMine = this;
                file = outDir;
                if (valueTotal > 0) {
                    progressCount(valueDone, valueTotal);
                }
            } else {
                return;
            }
        }
    }

    private ResPackage getResPackage() {
        Set<ResPackage> packages = this.resTable.listMainPackages();
        if (packages != null && !packages.isEmpty()) {
            return packages.iterator().next();
        }
        return null;
    }

    protected String decodeManifest(ExtFile apkFile, File outDir) throws AndrolibException {
        InputStream in = null;
        OutputStream out = null;
        try {
            try {
                in = apkFile.getDirectory().getFileInput("AndroidManifest.xml");
                out = new FileOutputStream(new File(outDir, "AndroidManifest.xml"));
                String decodeManifest = this.xmlDecoder.decodeManifest(in, out);
                try {
                    in.close();
                } catch (Exception e) {
                }
                try {
                    out.close();
                } catch (Exception e2) {
                }
                return decodeManifest;
            } finally {
            }
        } catch (Exception e3) {
            throw new AndrolibException("Could not decode AndroidManifest.xml", e3);
        }
    }

    @Override
    public String getResReference(int id) {
        try {
            ResResSpec spec = this.resTable.getResSpec(id);
            return "@" + getPackageName(id) + spec.getType() + "/" + spec.getName();
        } catch (AndrolibException e) {
            e.printStackTrace();
            return String.format("@%s%08X", getPackageName(id), Integer.valueOf(id));
        }
    }

    public ResTable getResTable() {
        return this.resTable;
    }

    @Override
    public String getAttributeById(int id) {
        try {
            ResResSpec spec = this.resTable.getResSpec(id);
            return "@" + getPackageName(id) + spec.getType() + "/" + spec.getName();
        } catch (AndrolibException e) {
            e.printStackTrace();
            return String.format("?%s%08X", getPackageName(id), Integer.valueOf(id));
        }
    }

    public void stopDecode() {
        this.stopRunning = true;
    }
}
