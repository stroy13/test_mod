package com.gmail.heagoo.apkeditor.smali;

import android.content.Context;
import android.os.AsyncTask;

import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.SDCard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Extracts the APK's DEX files and reports actual disassembly progress to the UI. */
public class AsyncDecodeTask extends AsyncTask<Void, Integer, Boolean> {

    private final Context context;
    private final IDecodeTaskCallback callback;
    private final String decodeRootPath;
    private final String apkPath;
    private final List<String> dexFileList = new ArrayList<>();
    private String strError;
    private String strWarning;

    public AsyncDecodeTask(Context context, String apkPath,
                           String decodeRootPath, IDecodeTaskCallback callback) {
        this.context = context;
        this.callback = callback;
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null) {
            callback.dexDecodingStarted();
        }
    }

    private void prepareDexFiles(boolean mainOnly) throws Exception {
        dexFileList.clear();
        String tmpDirectory = SDCard.makeDir(context, "tmp");
        try (ZipFile zipFile = new ZipFile(apkPath)) {
            if (mainOnly) {
                ZipEntry entry = zipFile.getEntry("classes.dex");
                if (entry == null) {
                    throw new IOException("APK does not contain classes.dex");
                }
                unzipDex2File(zipFile, entry, tmpDirectory + "classes.dex");
                return;
            }
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                // Only root-level DEX files belong to this APK's code, not assets or libraries.
                if (name.endsWith(".dex") && !name.contains("/")) {
                    unzipDex2File(zipFile, entry, tmpDirectory + name);
                }
            }
            if (dexFileList.isEmpty()) {
                throw new IOException("APK does not contain DEX files");
            }
        }
    }

    private void unzipDex2File(ZipFile zipFile, ZipEntry entry, String filePath)
            throws IOException {
        try (InputStream in = zipFile.getInputStream(entry);
             FileOutputStream out = new FileOutputStream(filePath)) {
            IOUtils.copy(in, out);
            dexFileList.add(filePath);
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            doAllJobs();
            return true;
        } catch (Exception e) {
            strError = e.getMessage() == null ? e.toString() : e.getMessage();
            return false;
        } catch (OutOfMemoryError e) {
            strError = "Недостаточно памяти для разбора DEX. Закройте другие приложения и повторите.";
            return false;
        }
    }

    public void doAllJobs() throws Exception {
        decode(false);
    }

    /** Used by the common-edit flow, which changes only the main DEX. */
    public void decodeMainDex() throws Exception {
        decode(true);
    }

    private void decode(boolean mainOnly) throws Exception {
        try {
            prepareDexFiles(mainOnly);
            decodeDexFiles();
        } finally {
            // Also clean up partial extraction when disassembly fails.
            for (String filePath : dexFileList) {
                new File(filePath).delete();
            }
            dexFileList.clear();
        }
    }

    private void decodeDexFiles() throws Exception {
        Class<?> decoderClass = Class.forName(SmaliToolChoice.decoderClass(context));
        Method progressMethod = decoderClass.getMethod("setProgressListener", BiConsumer.class);
        Method decodeMethod = decoderClass.getMethod("dex2smali", String.class);
        Method warningMethod = decoderClass.getMethod("getWarning");
        int dexTotal = dexFileList.size();
        for (int i = 0; i < dexTotal; i++) {
            if (isCancelled()) {
                return;
            }
            String dexFilePath = dexFileList.get(i);
            final int dexNumber = i + 1;
            final int totalDex = dexTotal;
            // The file counter is visible immediately, including while the first
            // batch of classes is being prepared (the class callback batches by 128).
            publishProgress(dexNumber, totalDex);
            Object decoder = decoderClass.getConstructor(String.class).newInstance(dexFilePath);
            invoke(progressMethod, decoder, (BiConsumer<Integer, Integer>) (done, total) -> {
                if (callback != null) {
                    publishProgress(dexNumber, totalDex, done, total);
                }
            });

            String directory = decodeRootPath + "/smali";
            if (!dexFilePath.endsWith("/classes.dex")) {
                String dexName = new File(dexFilePath).getName();
                dexName = dexName.substring(0, dexName.length() - 4);
                directory = decodeRootPath + "/smali_" + dexName;
            }
            createDirectoryIfNotExist(directory);
            // Unlike RefInvoke, this helper propagates decoder failures instead of
            // swallowing them and incorrectly reporting a successful conversion.
            invoke(decodeMethod, decoder, directory);
            if (strWarning == null) {
                strWarning = (String) invoke(warningMethod, decoder);
            }
        }
    }

    private static Object invoke(Method method, Object target, Object... arguments)
            throws Exception {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IOException(cause);
        }
    }

    private static void createDirectoryIfNotExist(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Can't create the output directory " + directory);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dispatchProgress(callback, values);
    }

    /** Kept independent of the Android task instance so callback routing is unit-testable. */
    static void dispatchProgress(IDecodeTaskCallback callback, Integer... values) {
        if (callback == null || values == null) {
            return;
        }
        if (values.length >= 4) {
            callback.dexClassProgress(values[0], values[1], values[2], values[3]);
        } else if (values.length >= 2) {
            callback.dexDecodingProgress(values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (callback != null) {
            callback.dexDecodingFinished(result, result ? null : strError,
                    result ? strWarning : null);
        }
    }

    public interface IDecodeTaskCallback {
        void dexDecodingStarted();

        default void dexDecodingProgress(int current, int total) {
        }

        default void dexClassProgress(int dex, int dexTotal, int done, int total) {
        }

        void dexDecodingFinished(boolean result, String strError, String strWarning);
    }
}
