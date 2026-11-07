package com.gmail.heagoo.apkeditor.smali;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Regression for the lost DEX/class counter beside the conversion spinner. */
public class AsyncDecodeTaskTest {
    private static final class RecordingCallback implements AsyncDecodeTask.IDecodeTaskCallback {
        final List<int[]> updates = new ArrayList<>();
        int started;
        int finished;

        @Override
        public void dexDecodingStarted() {
            started++;
        }

        @Override
        public void dexDecodingProgress(int current, int total) {
            updates.add(new int[]{current, total});
        }

        @Override
        public void dexClassProgress(int dex, int dexTotal, int done, int total) {
            updates.add(new int[]{dex, dexTotal, done, total});
        }

        @Override
        public void dexDecodingFinished(boolean result, String error, String warning) {
            finished++;
        }
    }

    @Test
    public void routesFileAndClassCountersWithoutMixingThem() {
        RecordingCallback callback = new RecordingCallback();
        AsyncDecodeTask.dispatchProgress(callback, 1, 2);
        AsyncDecodeTask.dispatchProgress(callback, 1, 2, 128, 5000);
        AsyncDecodeTask.dispatchProgress(callback, 2, 2, 5000, 5000);
        assertEquals(3, callback.updates.size());
        assertArrayEquals(new int[]{1, 2}, callback.updates.get(0));
        assertArrayEquals(new int[]{1, 2, 128, 5000}, callback.updates.get(1));
        assertArrayEquals(new int[]{2, 2, 5000, 5000}, callback.updates.get(2));
    }

    @Test
    public void ignoresMissingAndIncompleteUpdates() {
        RecordingCallback callback = new RecordingCallback();
        AsyncDecodeTask.dispatchProgress(null, 1, 2, 3, 4);
        AsyncDecodeTask.dispatchProgress(callback, (Integer[]) null);
        AsyncDecodeTask.dispatchProgress(callback);
        AsyncDecodeTask.dispatchProgress(callback, 1);
        assertEquals(0, callback.updates.size());
    }

    @Test
    public void olderCallbacksStillCompileWithoutProgressOverrides() {
        AsyncDecodeTask.IDecodeTaskCallback callback = new AsyncDecodeTask.IDecodeTaskCallback() {
            @Override
            public void dexDecodingStarted() {
            }

            @Override
            public void dexDecodingFinished(boolean result, String error, String warning) {
            }
        };
        callback.dexDecodingProgress(1, 2);
        callback.dexClassProgress(1, 2, 128, 5000);
        AsyncDecodeTask.dispatchProgress(callback, 1, 2, 128, 5000);
    }
}
