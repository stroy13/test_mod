package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.common.ITaskCallback;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ComposeThread extends Thread {
    public abstract void setExtraMaker(IApkMaking iApkMaking);

    public abstract void setModification(boolean z, boolean z2, boolean z3, List<String> list, Map<String, String> map, Map<String, String> map2, Set<String> set, Map<String, String> map3, boolean z4);

    public abstract void setTaskCallback(ITaskCallback iTaskCallback);

    public abstract void stopRunning();
}
