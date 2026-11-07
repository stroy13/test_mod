package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.Display;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmaliMethodWindowHelper {
    private WeakReference<ISmaliMethodClicked> callbackRef;
    private String methodComputedFrom;
    private PopupWindow popupWindow;

    public interface ISmaliMethodClicked {
        void gotoLine(int i);
    }

    public SmaliMethodWindowHelper(ISmaliMethodClicked callback) {
        this.callbackRef = new WeakReference<>(callback);
    }

    public String getFile() {
        return this.methodComputedFrom;
    }

    public void createPopWindow(Activity activity, String smaliFile, final List<SmaliMethodInfo> methodList) {
        this.methodComputedFrom = smaliFile;
        LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService("layout_inflater");
        View view = layoutInflater.inflate(R.layout.popup_list, (ViewGroup) null);
        ListView methodLv = (ListView) view.findViewById(R.id.lvGroup);
        SmaliMethodAdapter adapter = new SmaliMethodAdapter(activity, methodList);
        methodLv.setAdapter((ListAdapter) adapter);
        int height = (Display.getHeight(activity) * 2) / 5;
        int width = Display.getWidth(activity);
        this.popupWindow = new PopupWindow(view, width, height);
        methodLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view2, int position, long id) {
                if (position < methodList.size()) {
                    SmaliMethodInfo info = (SmaliMethodInfo) methodList.get(position);
                    if (SmaliMethodWindowHelper.this.callbackRef.get() != null) {
                        ((ISmaliMethodClicked) SmaliMethodWindowHelper.this.callbackRef.get()).gotoLine(info.lineIndex + 1);
                    }
                }
                if (SmaliMethodWindowHelper.this.popupWindow != null) {
                    SmaliMethodWindowHelper.this.popupWindow.dismiss();
                }
            }
        });
    }

    public void doPopWindowShow(View parent) {
        if (this.popupWindow != null) {
            this.popupWindow.setFocusable(true);
            this.popupWindow.setOutsideTouchable(true);
            this.popupWindow.setBackgroundDrawable(new BitmapDrawable());
            this.popupWindow.showAsDropDown(parent, 0, 0);
        }
    }

    public void asyncShowPopup(Activity activity, String filePath, String text, View parent) {
        new MethodAsyncLoader(activity, filePath, text, parent).execute(new Void[0]);
    }

    private class MethodAsyncLoader extends AsyncTask<Void, Integer, Boolean> {
        private WeakReference<Activity> activityRef;
        private String content;
        private int fileType;
        private View parentView;
        private String smaliFile;
        private final int FILE_TYPE_SMALI = 0;
        private final int FILE_TYPE_JAVA = 1;
        private List<SmaliMethodInfo> methodList = new ArrayList();

        MethodAsyncLoader(Activity activity, String filePath, String fileContent, View parent) {
            this.fileType = -1;
            this.activityRef = new WeakReference<>(activity);
            this.smaliFile = filePath;
            this.content = fileContent;
            this.parentView = parent;
            if (filePath.endsWith(".smali")) {
                this.fileType = 0;
            } else if (filePath.endsWith(".java")) {
                this.fileType = 1;
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override
        public Boolean doInBackground(Void... params) {
            BufferedReader br = new BufferedReader(new StringReader(this.content));
            int lineIndex = 0;
            try {
                switch (this.fileType) {
                    case 0:
                        while (true) {
                            String line = br.readLine();
                            if (line == null) {
                                break;
                            } else {
                                parseSmaliMethod(lineIndex, line);
                                lineIndex++;
                            }
                        }
                    case 1:
                        while (true) {
                            String line2 = br.readLine();
                            if (line2 == null) {
                                break;
                            } else {
                                parseJavaMethod(lineIndex, line2);
                                lineIndex++;
                            }
                        }
                }
            } catch (IOException e) {
                return null;
            }
            return null;
        }

        private void parseJavaMethod(int lineIndex, String line) {
            String line2 = line.trim();
            if (line2.length() <= 6 || line2.charAt(0) != 'p') {
                return;
            }
            if (line2.charAt(1) == 'u' || line2.charAt(1) == 'r') {
                Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])").matcher(line2);
                if (matcher.matches()) {
                    String prototype = matcher.group(0);
                    if (prototype.endsWith("{")) {
                        prototype = prototype.substring(0, prototype.length() - 1).trim();
                    }
                    this.methodList.add(new SmaliMethodInfo(lineIndex, prototype));
                }
            }
        }

        private void parseSmaliMethod(int lineIndex, String line) {
            if (line.startsWith(".method ")) {
                String prototype = line.substring(8);
                this.methodList.add(new SmaliMethodInfo(lineIndex, prototype));
            }
        }

        @Override
        public void onPostExecute(Boolean result) {
            SmaliMethodWindowHelper.this.createPopWindow(this.activityRef.get(), this.smaliFile, this.methodList);
            SmaliMethodWindowHelper.this.doPopWindowShow(this.parentView);
        }
    }
}
