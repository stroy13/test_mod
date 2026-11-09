package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.IGeneralCallback;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatchExecutor implements IGeneralCallback {

    private WeakReference<ApkInfoActivity> activityRef;
    private String patchPath;
    private IPatchContext patchContext;

    // patch and source zip file
    private Patch patch;
    private ZipFile sourceZip;

    public PatchExecutor(ApkInfoActivity activity, String patchPath,
                         IPatchContext logger) {
        this.activityRef = new WeakReference<>(activity);
        this.patchPath = patchPath;
        this.patchContext = logger;
    }

    public void applyPatch() {
        // Parse the patch
        try {
            this.sourceZip = new ZipFile(patchPath);
            ZipEntry entry = sourceZip.getEntry("patch.txt");
            if (entry == null) {
                sourceZip.close();
                sourceZip = null;
                patchContext.error(R.string.patch_error_no_entry, "patch.txt");
                // Without this return the code fell through to
                // sourceZip.getInputStream(entry) with both null -> NPE, and the
                // dialog stayed stuck on "Applying patch..." forever.
                patchContext.patchFinished();
                return;
            }

            InputStream input = sourceZip.getInputStream(entry);
            this.patch = PatchParser.parse(input, patchContext);
            input.close();
        } catch (Exception e) {
            patchContext.error(R.string.general_error, describe(e));
            e.printStackTrace();
            patchContext.patchFinished();
            return;
        }

        boolean needToDecode = false;

        ApkInfoActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            patchContext.patchFinished();
            return;
        }

        // Check if need to decode DEX file
        if (!activity.isDexDecoded()) {
            for (PatchRule rule : patch.rules) {
                needToDecode = rule.isSmaliNeeded();
                if (needToDecode) {
                    break;
                }
            }

            if (needToDecode) {
                patchContext.info(R.string.decode_dex_file, true);
                activity.decodeDex(this);
            }
        }

        // When do not need to decode DEX, directly apply patch
        if (!needToDecode) {
            applyRules(patch.rules, sourceZip);
        }
    }

    private void applyRules(final List<PatchRule> rules, final ZipFile sourceZip) {
        new Thread() {
            @Override
            public void run() {
                try {
                    // Apply all the rules
                    int index = 0;
                    // A GOTO chain can loop; bound the hops so a cyclic patch
                    // cannot spin this thread forever.
                    int hops = 0;
                    int maxHops = Math.max(1000, rules.size() * 100);
                    while (index >= 0 && index < rules.size()) {
                        if (++hops > maxHops) {
                            patchContext.error(R.string.patch_error_goto_loop);
                            break;
                        }
                        PatchRule rule = rules.get(index);
                        patchContext.info(R.string.patch_start_apply, true, rule.startLine);

                        String nextRule = null;
                        if (rule.isValid(patchContext)) {
                            nextRule = rule.executeRule(activityRef.get(), sourceZip, patchContext);
                        }
                        // Goto the target rule
                        if (nextRule != null) {
                            int target = findTargetRule(rules, nextRule);
                            if (target < 0) {
                                // findTargetRule returns -1 for an unknown name.
                                // The old code assigned it straight to index and
                                // continued, so the next rules.get(-1) threw
                                // IndexOutOfBoundsException on this thread. With
                                // no handler anywhere, that killed the process --
                                // the patch window simply disappeared.
                                patchContext.error(R.string.patch_error_no_rule, nextRule);
                                break;
                            }
                            index = target;
                            continue;
                        }

                        index += 1;
                    }
                    patchContext.info(R.string.all_rules_applied, true);
                } catch (Throwable t) {
                    // Any rule may throw (bad smali, IO error, a '%' in a path
                    // reaching String.format). Report it in the log instead of
                    // letting an uncaught worker-thread exception take the whole
                    // app down with the dialog.
                    patchContext.error(R.string.patch_error_rule_failed, describe(t));
                    t.printStackTrace();
                } finally {
                    // Always release the dialog, even on failure, so it never
                    // stays stuck on "Applying patch...".
                    try {
                        patchContext.patchFinished();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }.start();
    }

    /** getMessage() is null for many runtime exceptions; never log a bare null. */
    private static String describe(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        String message = t.getMessage();
        return message == null || message.length() == 0
                ? t.getClass().getSimpleName() : t.getClass().getSimpleName() + ": " + message;
    }

    // Get the index of the target rule
    private int findTargetRule(List<PatchRule> rules, String name) {
        for (int i = 0; i < rules.size(); ++i) {
            if (name.equals(rules.get(i).getRuleName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void callbackFunc() {
        if (patch != null && patch.rules != null && sourceZip != null) {
            applyRules(patch.rules, sourceZip);
        }
    }

    public List<String> getRuleNames() {
        List<String> names = new ArrayList<>();
        if (patch != null && patch.rules != null) {
            for (PatchRule rule : patch.rules) {
                names.add(rule.getRuleName());
            }
        }
        return names;
    }
}
