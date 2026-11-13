package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.value.ResReferenceValue;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.Duo;
import brut.util.KXmlSerializer;
import brut.util.LanguageMapping;
import com.gmail.heagoo.apkeditor.ApkComposeService;
import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.FileCopyDialog;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.ProjectAnalysis;
import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.smali.AsyncDecodeTask;
import com.gmail.heagoo.apkeditor.translate.PossibleLanguages;
import com.gmail.heagoo.apkeditor.translate.TranslateItem;
import com.gmail.heagoo.apkeditor.ui.AddFolderDialog;
import com.gmail.heagoo.apkeditor.util.AndroidBug5497Workaround;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.MD5;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.PreferenceUtil;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ServiceUtil;
import com.gmail.heagoo.common.TextFileReader;
import com.gmail.heagoo.common.UriUtil;
import com.gmail.heagoo.common.ZipUtil;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.util.OpenFiles;
import com.gmail.heagoo.httpserver.HttpServiceManager;
import com.gmail.heagoo.pngeditor.PngEditActivity;
import common.types.ActivityState_V1;
import common.types.ProjectInfo_V1;
import common.types.StringItem;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: /tmp/apkx/classes5.dex */
public class ApkInfoActivity extends CustomizedLangActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, IManifestChangeCallback, View.OnClickListener, AsyncDecodeTask.IDecodeTaskCallback, View.OnLongClickListener, ApkParseConsumer, ResSelectionChangeListener, AddFolderDialog.AddFolderCallback {
    private static final int RC_COLOR_EDITOR = 3;
    private static final int RC_COMPOSE = 1;
    private static final int RC_FILE_EDITOR = 0;
    public static final int RC_OPEN_BEFORE_REPLACE = 1001;
    public static final int RC_OPEN_EXTERNAL = 1002;
    public static final int RC_REQUEST_PERMISSION = 1003;
    private static final int RC_SEARCH_MF = 2;
    private static final int RC_TRANSLATE = 1000;
    private static final String TMP_EDITOR_FILE = "APKEDITOR.xcrhfvke";
    private HashMap<String, String> addedFiles;
    HashMap<String, ArrayList<StringItem>> allStringValues;
    private AnalysisJob analysisJob;
    private ImageView apkIcon;
    ApkInfoParser.AppInfo apkInfo;
    private TextView apkLabel;
    protected String apkPath;
    private TextView apkPkgPath;
    Map<String, Map<String, String>> changedStringValues;
    private ProgressBar decodeProgressBar;
    private View decodeProgressRow;
    private TextView decodeProgressText;
    private TextView decodeResultDetail;
    private View decodeResultLayout;
    private TextView decodeResultTitle;
    protected String decodeRootPath;
    private ArrayList<String> deletedFiles;
    private ImageView dex2smaliImage;
    private View dexDecodeLayout;
    private IGeneralCallback dexDecodedCallback;
    private String entryNameForExternal;
    protected Map<String, String> fileEntry2ZipEntry;
    private String filePathForExternal;
    private FutureTask<Boolean> indexSaveTask;
    protected boolean isFullDecoding;
    private ArrayList<String> langConfigList;
    private String lastLoadError;
    private Button loadCopyButton;
    private Button loadRetryButton;
    private LinearLayout loadingLayout;
    private Drawable manifestIcon;
    private Drawable manifestIconGrey;
    private LinearLayout manifestLayout;
    private ListView manifestList;
    private ManifestInlineEditor manifestEditor;
    private String manifestBaseline;
    private boolean manifestDirty;
    private boolean manifestLoading;
    private boolean restoreManifestDraft;
    private final Runnable manifestAutosave = () -> saveInlineManifest(false);

    private RadioButton manifestRadio;
    private AutoCompleteAdapter mfKeywordAdapter;
    private long modifiedTimeBeforeOpen;
    protected ResNavigationMgr navigationMgr;
    private ApkParseThread parseThread;
    private View patchMenu;
    private String projectName;
    private HashMap<String, String> replacedFiles;
    private String resCurrentDir;
    private boolean resFileModified;
    private Drawable resIcon;
    private Drawable resIconGrey;
    private AutoCompleteAdapter resKeywordAdapter;
    ResListAdapter resListAdapter;
    private View resMenuLayout;
    private LinearLayout resNaviHeader;
    private HorizontalScrollView resNaviScrollView;
    private RadioButton resRadio;
    private View resSearchLayout;
    private View resSelectHeader;
    private TextView resSelectTip;
    private Map<String, String> res_addedFiles;
    private Set<String> res_deletedFiles;
    private Map<String, String> res_replacedFiles;
    private LinearLayout resourceLayout;
    private ListView resourceList;
    private View rotateMenu;
    private Button saveBtn;
    private String savedParam_extraStr;
    private String savedParam_filePath;
    private SomethingChangedListener savedParam_listener;
    private volatile boolean screenDestroyed;
    protected ImageView searchOptionCase;
    protected ImageView searchOptionImage;
    private ArrayList<String> smaliFolders;
    private AutoCompleteAdapter strKeywordAdapter;
    private LinearLayout stringLayout;
    private ListView stringList;
    private StringListAdapter stringListAdapter;
    private RadioButton stringRadio;
    private Drawable textIcon;
    private Drawable textIconGrey;
    protected int themeId;
    private View webserverMenu;
    protected boolean searchTextContent = true;
    protected boolean searchResSensitive = true;
    private String curConfig = null;
    private Stack<Duo<Integer, Integer>> resListPosition = new Stack<>();
    private String earlyEditToken = UUID.randomUUID().toString();
    private boolean stringModified = false;
    private boolean manifestModified = false;
    private boolean stringParsed = false;
    private boolean resourceParsed = false;
    private int curSelectedRadio = 0;
    private boolean dexDecoded = false;
    private boolean dexConversionInProgress = false;
    private boolean bStringPrepared = false;
    private int rotateClickedTimes = 0;
    // Last decode phase reported by ApkParseThread, -1 when nothing was reported yet.
    private int lastLoadPhase = -1;

    static final class AnalysisJob {
        final AtomicBoolean cancelled;
        AlertDialog dialog;
        long lastUpdateNanos;
        ProgressBar progressBar;
        TextView progressText;
        TextView progressDetail;
        Thread worker;

        private AnalysisJob() {
            this.cancelled = new AtomicBoolean();
        }
    }

    public static ProjectInfo_V1 loadProject(String prjDirectory) {
        String versionPath = prjDirectory + "/.prj_version";
        String infoPath = prjDirectory + "/ae.prj";
        File versionFile = new File(versionPath);
        File prjInfoFile = new File(infoPath);
        if (versionFile.exists() && prjInfoFile.exists()) {
            try {
                TextFileReader reader = new TextFileReader(versionPath);
                int version = Integer.valueOf(reader.getContents()).intValue();
                switch (version) {
                    case 1:
                        return (ProjectInfo_V1) IOUtils.readObjectFromFile(infoPath);
                    default:
                        return null;
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private static String getSmaliRootDir(String fileEntry) {
        int pos = fileEntry.indexOf(47);
        if (pos == -1) {
            return null;
        }
        String folderName = fileEntry.substring(0, pos);
        return folderName;
    }

    public static String dealWithSmaliFile(String entryName, Set<String> modifiedDexNames) {
        if ((entryName.startsWith("smali/") || entryName.startsWith("smali_")) && entryName.endsWith(".smali")) {
            String smaliDir = getSmaliRootDir(entryName);
            modifiedDexNames.add(smaliDir);
            return smaliDir;
        }
        return null;
    }

    public static boolean isCommonImage(String entryName) {
        return entryName.endsWith(".jpg") || (entryName.endsWith(".png") && !entryName.endsWith(".9.png"));
    }

    public static String createOutputPath(String inputPath, String outputDir, String filename) {
        if (outputDir.endsWith("/")) {
            outputDir = outputDir.substring(0, outputDir.length() - 1);
        }
        // Never overwrite an existing result: a failed build used to leave the previous
        // (possibly broken) gen_signed.apk in place, and users then installed that stale
        // file thinking it was the new build. Pick the first free numbered name instead.
        String candidate = outputDir + "/" + filename + ".apk";
        int index = 2;
        while (new File(candidate).exists() || candidate.equals(inputPath)) {
            candidate = outputDir + "/" + filename + index + ".apk";
            index++;
        }
        return candidate;
    }

    public static boolean generalTranslatePluginExist(Context ctx) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(null, "application/com.gmail.heagoo.apkeditor-translate");
        PackageManager manager = ctx.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return infos.size() > 0;
    }

    public static ResConfigFlags getBestConfigFlags(Set<ResConfigFlags> configs) {
        Set<String> qualifiers = new HashSet<>();
        Iterator<ResConfigFlags> it = configs.iterator();
        while (it.hasNext()) {
            qualifiers.add(it.next().getQualifiers());
        }
        String matches = getBestConfig(qualifiers);
        for (ResConfigFlags configFlags : configs) {
            if (configFlags.getQualifiers().equals(matches)) {
                return configFlags;
            }
        }
        return null;
    }

    public static String getBestConfig(Set<String> configs) {
        String bestConfig = null;
        Locale locale = Locale.getDefault();
        String realLang = "-" + locale.getLanguage();
        String realCountry = locale.getCountry();
        String realQualifier = realLang + "-r" + realCountry;
        LOGGER.info("*****realQualifier=" + realQualifier);
        for (String quaifier : configs) {
            if (realQualifier.equals(quaifier)) {
                return quaifier;
            }
            if (realLang.equals(quaifier)) {
                bestConfig = quaifier;
            } else if (quaifier.equals("") && bestConfig == null) {
                bestConfig = quaifier;
            }
        }
        return bestConfig;
    }

    protected void onCreate(Bundle savedInstanceState) {
        String previousToken;
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        setContentView(R.layout.activity_apkinfo);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(1024, 1024);
            AndroidBug5497Workaround.assistActivity(this);
        }
        this.projectName = ActivityUtil.getParam(getIntent(), "projectName");
        ProjectInfo_V1 prjInfo = null;
        if (this.projectName != null) {
            try {
                String prjRoot = SDCard.makeDir(this, ".projects");
                prjInfo = loadProject(prjRoot + this.projectName);
                this.apkPath = prjInfo.apkPath;
                this.decodeRootPath = prjInfo.decodeRootPath;
            } catch (Exception e) {
                Toast.makeText((Context) this, R.string.cannot_load_project_info, 1).show();
                finish();
                return;
            }
        } else {
            Intent intent = getIntent();
            Uri uri = intent.getData();
            if (uri != null) {
                if (!StorageAccess.has(this)) {
                    StorageAccess.request(this, RC_REQUEST_PERMISSION);
                }
                try {
                    PackageManager pm = getPackageManager();
                    ApplicationInfo moreInfo = pm.getApplicationInfo(getPackageName(), 0);
                    String apkPath = moreInfo.sourceDir;
                    MainActivity.it(this, getPackageName(), getFilesDir().getPath(), apkPath);
                } catch (Exception e2) {
                }
                this.apkPath = UriUtil.getAbsolutePath(this, uri);
            }
            if (this.apkPath == null) {
                this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");
            }
            this.decodeRootPath = ActivityUtil.getParam(getIntent(), "decodeRootPath");
            if (this.decodeRootPath == null) {
                String decodeDir = SettingActivity.getDecodeDirectory(this);
                if (decodeDir != null) {
                    this.decodeRootPath = decodeDir + "/decoded";
                } else {
                    File fileDir = getFilesDir();
                    String rootDirectory = fileDir.getAbsolutePath();
                    this.decodeRootPath = rootDirectory + "/decoded";
                }
            }
        }
        try {
            if (this.apkPath != null && !"".equals(this.apkPath)) {
                this.apkInfo = new ApkInfoParser().parse(this, this.apkPath);
            }
        } catch (Exception e3) {
        }
        this.stringListAdapter = new StringListAdapter(this);
        initView();
        if (savedInstanceState != null && (previousToken = savedInstanceState.getString("lab_early_token")) != null && previousToken.matches("[0-9a-f-]{36}")) {
            this.earlyEditToken = previousToken;
        }
        if (savedInstanceState != null) this.restoreManifestDraft = savedInstanceState.getBoolean("lab_manifest_draft", false);
        if (savedInstanceState != null && savedInstanceState.getBoolean("lab_load_pending", false) && this.projectName == null) {
            restorePendingStringEdits(savedInstanceState);
            startApkDecode();
            return;
        }
        if (savedInstanceState != null) {
            recoverFromBundle(savedInstanceState);
            return;
        }
        if (this.projectName != null) {
            if (prjInfo != null && prjInfo.state != null) {
                recoverFromProject(prjInfo);
                return;
            } else {
                Toast.makeText((Context) this, R.string.cannot_load_project_info, 1).show();
                finish();
                return;
            }
        }
        startApkDecode();
    }

    private void startApkDecode() {
        this.isFullDecoding = ActivityUtil.getBoolParam(getIntent(), "isFullDecoding");
        this.lastLoadError = null;
        this.stringParsed = false;
        this.resourceParsed = false;
        this.bStringPrepared = false;
        this.lastLoadPhase = -1;
        this.stringListAdapter.setEditingEnabled(true);
        updateCenterView();
        findViewById(R.id.progress_bar).setVisibility(0);
        this.loadRetryButton.setVisibility(8);
        this.loadCopyButton.setText(R.string.lab_load_diagnostic);
        this.loadCopyButton.setVisibility(0);
        this.parseThread = new ApkParseThread(this, this, this.apkPath, this.decodeRootPath, this.isFullDecoding);
        this.parseThread.start();
    }

    public boolean storeProject(String prjDirectory, ProjectInfo_V1 prjInfo) {
        String versionPath = prjDirectory + "/.prj_version";
        String infoPath = prjDirectory + "/ae.prj";
        try {
            AtomicProjectFile.writeText(new File(versionPath), "1");
            AtomicProjectFile.writeObject(new File(infoPath), prjInfo);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void recoverFromProject(ProjectInfo_V1 prjInfo) {
        Bundle bundle = new Bundle();
        prjInfo.state.toBundle(bundle);
        recoverFromBundle(bundle);
    }

    private void recoverFromBundle(final Bundle savedInstanceState) {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void process() {
                ApkInfoActivity.this.recoverData(savedInstanceState);
            }

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void afterProcess() {
                ApkInfoActivity.this.recoverView();
            }
        }, -1).show();
    }

    private ActivityState_V1 saveCurrentState(String workingPath) {
        return saveCurrentState(workingPath, false, null);
    }

    public ActivityState_V1 saveCurrentState(String workingPath, boolean saveAll, File projectDir) {
        ActivityState_V1 state = new ActivityState_V1();
        Map<String, Map<String, String>> changedValues = this.stringListAdapter.getChangedValues();
        if (changedValues != null && !changedValues.isEmpty()) {
            this.stringModified = true;
            String path = workingPath + "changedStringValues";
            IOUtils.writeObjectToFile(path, changedValues);
            state.putString("changedStringValues_file", path);
        }
        if (!this.isFullDecoding && this.resListAdapter != null) {
            String curDir = this.resListAdapter.getData(null);
            state.putString("res_current_dir", curDir);
            Map<String, String> added = this.resListAdapter.getAddedFiles();
            Map<String, String> replaced = this.resListAdapter.getReplacedFiles();
            Set<String> deleted = this.resListAdapter.getDeletedFiles();
            if (projectDir != null) {
                moveTempFile2ProjectDir(added, replaced, projectDir);
            }
            if (added != null && !added.isEmpty()) {
                String path2 = workingPath + "res_added";
                IOUtils.writeObjectToFile(path2, added);
                state.putString("res_added_file", path2);
            }
            if (replaced != null && !replaced.isEmpty()) {
                String path3 = workingPath + "res_replaced";
                IOUtils.writeObjectToFile(path3, replaced);
                state.putString("res_replaced_file", path3);
            }
            if (deleted != null && !deleted.isEmpty()) {
                String path4 = workingPath + "res_deleted";
                IOUtils.writeObjectToFile(path4, deleted);
                state.putString("res_deleted_file", path4);
            }
        }
        String path5 = workingPath + "allStringValues";
        if (saveAll) {
            IOUtils.writeObjectToFile(path5, this.allStringValues);
        }
        state.putString("allStringValues_file", path5);
        String path6 = workingPath + "fileEntry2ZipEntry";
        if (saveAll && !this.isFullDecoding) {
            IOUtils.writeObjectToFile(path6, this.fileEntry2ZipEntry);
        }
        state.putString("fileEntry2ZipEntry_file", this.isFullDecoding ? null : path6);
        state.putString("curConfig", this.curConfig);
        state.putSerializable("langConfigList", this.langConfigList);
        state.putBoolean("stringModified", this.stringModified);
        state.putBoolean("manifestModified", this.manifestModified);
        state.putBoolean("stringParsed", this.stringParsed);
        state.putBoolean("resourceParsed", this.resourceParsed);
        state.putBoolean("bStringPrepared", this.bStringPrepared);
        state.putBoolean("searchTextContent", this.searchTextContent);
        state.putBoolean("searchResSensitive", this.searchResSensitive);
        state.putInt("curSelectedRadio", this.curSelectedRadio);
        state.putInt("rotateClickedTimes", this.rotateClickedTimes);
        state.putBoolean("dex2smaliClicked", this.dexDecoded);
        state.putString("savedParam_extraStr", this.savedParam_extraStr);
        state.putString("savedParam_filePath", this.savedParam_filePath);
        state.putBoolean("isFullDecoding", this.isFullDecoding);
        return state;
    }

    private void moveTempFile2ProjectDir(Map<String, String> added, Map<String, String> replaced, File projectDir) {
        try {
            String tmpFolder = SDCard.makeWorkingDir(this);
            if (added != null && !added.isEmpty()) {
                for (Map.Entry<String, String> entry : added.entrySet()) {
                    String path = entry.getValue();
                    if (path != null && path.startsWith(tmpFolder)) {
                        File oldFile = new File(path);
                        File newFile = new File(projectDir, oldFile.getName());
                        if (oldFile.renameTo(newFile)) {
                            entry.setValue(newFile.getPath());
                        }
                    }
                }
            }
            if (replaced != null && !replaced.isEmpty()) {
                for (Map.Entry<String, String> entry2 : replaced.entrySet()) {
                    String path2 = entry2.getValue();
                    if (path2 != null && path2.startsWith(tmpFolder)) {
                        File oldFile2 = new File(path2);
                        File newFile2 = new File(projectDir, oldFile2.getName());
                        if (oldFile2.renameTo(newFile2)) {
                            entry2.setValue(newFile2.getPath());
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (this.manifestDirty && !saveInlineManifest(false)) {
            try {
                AtomicProjectFile.writeObject(manifestDraftFile(), new String[]{this.manifestEditor.getText().toString(), this.manifestBaseline});
                savedInstanceState.putBoolean("lab_manifest_draft", true);
            } catch (IOException e) {
                Toast.makeText(this, R.string.lab_manifest_draft_failed, Toast.LENGTH_LONG).show();
            }
        }
        String workingPath = null;
        boolean pending = this.projectName == null && !this.resourceParsed;
        savedInstanceState.putBoolean("lab_load_pending", pending);
        savedInstanceState.putString("lab_early_token", this.earlyEditToken);
        if (pending) {
            savedInstanceState.putString("lab_early_config", this.curConfig);
            savedInstanceState.putInt("lab_early_radio", this.curSelectedRadio);
            HashMap<String, Map<String, String>> changes = PendingStringEdits.snapshot(this.stringListAdapter.getChangedValues());
            if (!changes.isEmpty()) {
                try {
                    PendingStringEdits.save(getFilesDir(), this.earlyEditToken, changes);
                    savedInstanceState.putBoolean("lab_early_journal", true);
                } catch (Exception e) {
                    long size = 0;
                    int count = 0;
                    for (Map.Entry<String, Map<String, String>> group : changes.entrySet()) {
                        size += group.getKey().length();
                        for (Map.Entry<String, String> value : group.getValue().entrySet()) {
                            size += value.getKey().length() + value.getValue().length();
                            count++;
                        }
                    }
                    if (size < 30000 && count < 2000) {
                        savedInstanceState.putSerializable("lab_early_backup", changes);
                    }
                    Toast.makeText((Context) this, R.string.lab_early_save_failed, 1).show();
                }
            }
            super.onSaveInstanceState(savedInstanceState);
            return;
        }
        try {
            File session = new File(getCacheDir(), "activity-state-" + this.earlyEditToken);
            if (!session.isDirectory() && !session.mkdirs()) throw new IOException("Cannot create session directory");
            workingPath = session.getAbsolutePath() + "/";
        } catch (Exception e) {
            super.onSaveInstanceState(savedInstanceState);
            return;
        }
        ActivityState_V1 state = saveCurrentState(workingPath, true, null);
        state.toBundle(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    public Object onRetainNonConfigurationInstance() {
        if (this.resourceParsed || this.stringListAdapter == null) {
            return null;
        }
        return PendingStringEdits.snapshot(this.stringListAdapter.getChangedValues());
    }

    private void restorePendingStringEdits(Bundle state) {
        String token = state.getString("lab_early_token");
        if (token != null && token.matches("[0-9a-f-]{36}")) {
            this.earlyEditToken = token;
        }
        Object retained = getLastNonConfigurationInstance();
        Map<String, Map<String, String>> changes = retained instanceof Map ? (Map) retained : null;
        if (changes == null && state.getBoolean("lab_early_journal", false)) {
            try {
                changes = PendingStringEdits.load(getFilesDir(), this.earlyEditToken);
            } catch (Exception e) {
                Toast.makeText((Context) this, R.string.lab_early_restore_failed, 1).show();
            }
        }
        if (changes == null && (state.getSerializable("lab_early_backup") instanceof Map)) {
            changes = (Map) state.getSerializable("lab_early_backup");
        }
        if (changes != null) {
            this.changedStringValues = changes;
            this.stringListAdapter.setChangedValues(changes);
        }
        this.curConfig = state.getString("lab_early_config");
        this.curSelectedRadio = state.getInt("lab_early_radio", 0);
    }

    private void loadParserProject() {
        this.allStringValues = new HashMap<>();
        this.changedStringValues = null;
        this.fileEntry2ZipEntry = null;
        this.curConfig = null;
        this.langConfigList = null;
        this.stringModified = false;
        this.manifestModified = false;
        this.stringParsed = true;
        this.resourceParsed = true;
        this.bStringPrepared = true;
        this.curSelectedRadio = 1;
        this.rotateClickedTimes = 0;
        this.dexDecoded = false;
        this.isFullDecoding = true;
        recoverView();
    }

    public void recoverData(Bundle savedInstanceState) {
        this.allStringValues = (HashMap) IOUtils.readObjectFromFile(savedInstanceState.getString("allStringValues_file"));
        String path = savedInstanceState.getString("changedStringValues_file");
        if (path != null) {
            this.changedStringValues = (HashMap) IOUtils.readObjectFromFile(path);
            mergeStrings(this.allStringValues, this.changedStringValues);
        }
        this.fileEntry2ZipEntry = (HashMap) IOUtils.readObjectFromFile(savedInstanceState.getString("fileEntry2ZipEntry_file"));
        this.curConfig = savedInstanceState.getString("curConfig");
        this.langConfigList = (ArrayList) savedInstanceState.getSerializable("langConfigList");
        this.stringModified = savedInstanceState.getBoolean("stringModified");
        this.manifestModified = savedInstanceState.getBoolean("manifestModified");
        this.stringParsed = savedInstanceState.getBoolean("stringParsed");
        this.resourceParsed = savedInstanceState.getBoolean("resourceParsed");
        this.bStringPrepared = savedInstanceState.getBoolean("bStringPrepared");
        this.searchTextContent = savedInstanceState.getBoolean("searchTextContent");
        this.searchResSensitive = savedInstanceState.getBoolean("searchResSensitive");
        this.curSelectedRadio = savedInstanceState.getInt("curSelectedRadio");
        this.rotateClickedTimes = savedInstanceState.getInt("rotateClickedTimes");
        this.resCurrentDir = savedInstanceState.getString("res_current_dir");
        String filePath = savedInstanceState.getString("res_added_file");
        if (filePath != null) {
            this.res_addedFiles = (Map) IOUtils.readObjectFromFile(filePath);
        }
        String filePath2 = savedInstanceState.getString("res_replaced_file");
        if (filePath2 != null) {
            this.res_replacedFiles = (Map) IOUtils.readObjectFromFile(filePath2);
        }
        String filePath3 = savedInstanceState.getString("res_deleted_file");
        if (filePath3 != null) {
            this.res_deletedFiles = (Set) IOUtils.readObjectFromFile(filePath3);
        }
        this.dexDecoded = savedInstanceState.getBoolean("dex2smaliClicked");
        this.savedParam_extraStr = savedInstanceState.getString("savedParam_extraStr");
        this.savedParam_filePath = savedInstanceState.getString("savedParam_filePath");
        this.isFullDecoding = savedInstanceState.getBoolean("isFullDecoding");
    }

    private void mergeStrings(Map<String, ArrayList<StringItem>> allStringValues, Map<String, Map<String, String>> changedStringValues) {
        PendingStringEdits.merge(allStringValues, changedStringValues);
    }

    public void recoverView() {
        showStringList();
        setupClickListener();
        showDecodedFileList();
        if (this.dexDecoded) {
            this.dexDecodeLayout.setVisibility(8);
        }
        updateSearchOption();
        this.webserverMenu.setVisibility(0);
        this.rotateMenu.setVisibility(0);
        if (isPro()) {
            this.patchMenu.setVisibility(0);
        } else {
            this.patchMenu.setVisibility(8);
        }
        this.saveBtn.setVisibility(0);
    }

    @Override
    protected void onPause() {
        if (this.manifestDirty && !saveInlineManifest(false)) {
            try {
                AtomicProjectFile.writeObject(manifestDraftFile(), new String[]{this.manifestEditor.getText().toString(), this.manifestBaseline});
            } catch (IOException e) {
                Toast.makeText(this, R.string.lab_manifest_draft_failed, Toast.LENGTH_LONG).show();
            }
        }
        super.onPause();
    }

    public void onDestroy() {
        if (this.manifestEditor != null) this.manifestEditor.removeCallbacks(this.manifestAutosave);
        this.screenDestroyed = true;
        if (localTranslationCancellation != null) localTranslationCancellation.set(true);
        com.gmail.heagoo.apkeditor.translate.LocalInference engineToCancel = localEngine.get();
        if (engineToCancel != null) engineToCancel.requestCancel();
        if (this.analysisJob != null) {
            this.analysisJob.cancelled.set(true);
            if (this.analysisJob.worker != null) {
                this.analysisJob.worker.interrupt();
            }
            if (this.analysisJob.dialog != null) {
                this.analysisJob.dialog.dismiss();
            }
            this.analysisJob = null;
        }
        if (isFinishing()) {
            PendingStringEdits.delete(getFilesDir(), this.earlyEditToken);
        }
        if (this.parseThread != null && this.parseThread.isAlive()) {
            this.parseThread.stopParse();
        }
        HttpServiceManager.instance().unbindService(this);
        super.onDestroy();
    }

    public void onBackPressed() {
        if (!saveInlineManifest(true)) return;
        boolean dealed = false;
        if (this.curSelectedRadio == 1) {
            if (this.resListAdapter == null) {
                dealed = false;
            } else if (!this.resListAdapter.getCheckedItems().isEmpty()) {
                this.resListAdapter.checkAllItems(false);
                dealed = true;
            } else if (!this.resListAdapter.isInRootDir()) {
                String oldDir = this.resListAdapter.getData(null);
                int idx = oldDir.lastIndexOf(47);
                String targetPath = oldDir.substring(0, idx);
                this.resListAdapter.openDirectory(targetPath);
                this.navigationMgr.gotoDirectory(targetPath);
                try {
                    Duo<Integer, Integer> pos = this.resListPosition.pop();
                    this.resourceList.setSelectionFromTop(((Integer) pos.m1).intValue(), ((Integer) pos.m2).intValue());
                } catch (Exception e) {
                }
                dealed = true;
            }
        }
        if (dealed) {
            return;
        }
        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setMessage(R.string.lab_project_exit_message)
                .setPositiveButton(R.string.lab_exit_without_project_save, (dialog, which) -> {
                    if (this.parseThread != null && this.parseThread.isAlive()) this.parseThread.stopParse();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (canSaveProject()) {
            dlg.setNeutralButton(R.string.lab_save_project_and_exit, (dialog, which) -> saveProject(true));
        }
        dlg.show();
    }

    private boolean canSaveProject() {
        return this.resourceParsed && this.lastLoadError == null && this.allStringValues != null
                && this.resListAdapter != null && this.decodeRootPath != null
                && new File(this.decodeRootPath).isDirectory()
                && (this.parseThread == null || !this.parseThread.isAlive());
    }

    private void requestProjectSave() {
        if (!saveInlineManifest(true)) return;
        if (!canSaveProject()) {
            Toast.makeText(this, R.string.lab_project_wait, Toast.LENGTH_LONG).show();
            return;
        }
        saveProject(false);
    }

    /** Copy an external staged file once; files already inside the project remain stable. */
    private Map<String, String> projectFiles(Map<String, String> input, File project, File dest) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (input == null) return result;
        int index = 0;
        String root = project.getCanonicalPath() + File.separator;
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getValue() == null) throw new IOException("Missing file: " + entry.getKey());
            File source = new File(entry.getValue());
            if (!source.isFile()) throw new IOException("Missing file: " + source);
            if (source.getCanonicalPath().startsWith(root)) {
                result.put(entry.getKey(), source.getAbsolutePath());
            } else {
                File target = new File(dest, Integer.toString(index++));
                CheckpointFiles.copy(source, target);
                result.put(entry.getKey(), target.getAbsolutePath());
            }
        }
        return result;
    }

    private void saveProject(final boolean exitAfterSave) {
        if (!canSaveProject()) {
            Toast.makeText(this, R.string.lab_project_wait, Toast.LENGTH_LONG).show();
            return;
        }
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            File projectDir;
            File snapshot;
            boolean created;
            String error;
            String savedName;

            @Override public void process() {
                try {
                    File root = new File(SDCard.makeDir(ApkInfoActivity.this, ".projects"));
                    ProjectInfo_V1 info;
                    if (ApkInfoActivity.this.projectName == null) {
                        String label = ApkInfoActivity.this.apkInfo == null ? "UNKNOWN" : ApkInfoActivity.this.apkInfo.label;
                        if (label == null || label.trim().isEmpty()) label = "UNKNOWN";
                        label = label.replaceAll("[\\\\/]+", "_").trim();
                        if (label.equals(".") || label.equals("..") || label.isEmpty()) label = "UNKNOWN";
                        this.projectDir = new File(root, label);
                        if (this.projectDir.exists()) this.projectDir = FileCopyDialog.getTargetNonExistFile(this.projectDir.getAbsolutePath(), true);
                        if (!this.projectDir.mkdir()) throw new IOException("Cannot create project: " + this.projectDir);
                        this.created = true;
                        info = new ProjectInfo_V1();
                        File original = new File(this.projectDir, "original.apk");
                        CheckpointFiles.copy(new File(ApkInfoActivity.this.apkPath), original);
                        info.apkPath = original.getAbsolutePath();
                        File decoded = new File(this.projectDir, "decoded");
                        CheckpointFiles.copy(new File(ApkInfoActivity.this.decodeRootPath), decoded);
                        info.decodeRootPath = decoded.getAbsolutePath();
                    } else {
                        this.projectDir = new File(root, ApkInfoActivity.this.projectName);
                        info = ApkInfoActivity.loadProject(this.projectDir.getAbsolutePath());
                        if (info == null) throw new IOException("Cannot load project metadata");
                    }
                    this.savedName = this.projectDir.getName();
                    this.snapshot = new File(this.projectDir, "state-" + UUID.randomUUID());
                    if (!this.snapshot.mkdir()) throw new IOException("Cannot create project state");
                    ActivityState_V1 state = ApkInfoActivity.this.saveCurrentState(this.snapshot.getAbsolutePath() + "/", true, null);
                    String[] keys = {"allStringValues_file", "changedStringValues_file", "fileEntry2ZipEntry_file", "res_added_file", "res_replaced_file", "res_deleted_file"};
                    for (String key : keys) {
                        String path = state.getString(key);
                        if (path != null && (!new File(path).isFile() || IOUtils.readObjectFromFile(path) == null))
                            throw new IOException("Cannot save state: " + key);
                    }
                    if (!ApkInfoActivity.this.isFullDecoding) {
                        Map<String, String> added = projectFiles(ApkInfoActivity.this.resListAdapter.getAddedFiles(), this.projectDir, new File(this.snapshot, "added"));
                        Map<String, String> replaced = projectFiles(ApkInfoActivity.this.resListAdapter.getReplacedFiles(), this.projectDir, new File(this.snapshot, "replaced"));
                        if (!added.isEmpty()) {
                            File file = new File(this.snapshot, "res_added");
                            AtomicProjectFile.writeObject(file, (java.io.Serializable) added);
                            state.putString("res_added_file", file.getAbsolutePath());
                        }
                        if (!replaced.isEmpty()) {
                            File file = new File(this.snapshot, "res_replaced");
                            AtomicProjectFile.writeObject(file, (java.io.Serializable) replaced);
                            state.putString("res_replaced_file", file.getAbsolutePath());
                        }
                    }
                    if (this.created) {
                        String current = state.getString("res_current_dir");
                        if (current != null) {
                            String oldRoot = new File(ApkInfoActivity.this.decodeRootPath).getCanonicalPath();
                            String cur = new File(current).getCanonicalPath();
                            String newRoot = new File(info.decodeRootPath).getCanonicalPath();
                            state.putString("res_current_dir", cur.equals(oldRoot) || cur.startsWith(oldRoot + File.separator)
                                    ? newRoot + cur.substring(oldRoot.length()) : newRoot);
                        }
                    }
                    info.state = state;
                    if (!ApkInfoActivity.this.storeProject(this.projectDir.getAbsolutePath(), info))
                        throw new IOException("Cannot save project metadata");
                } catch (Exception e) {
                    this.error = e.toString();
                    if (this.created && this.projectDir != null) CheckpointFiles.delete(this.projectDir);
                    else if (this.snapshot != null) CheckpointFiles.delete(this.snapshot);
                }
            }

            @Override public void afterProcess() {
                if (this.error != null) {
                    new AlertDialog.Builder(ApkInfoActivity.this).setMessage(getString(R.string.cannot_save_project) + "\n" + this.error)
                            .setPositiveButton(android.R.string.ok, null).show();
                    return;
                }
                Toast.makeText(ApkInfoActivity.this, R.string.lab_project_saved, Toast.LENGTH_LONG).show();
                if (exitAfterSave) {
                    ApkInfoActivity.this.finish();
                } else if (this.created) {
                    Intent intent = new Intent(ApkInfoActivity.this, ApkInfoActivity.class);
                    ActivityUtil.attachParam(intent, "projectName", this.savedName);
                    ApkInfoActivity.this.startActivity(intent);
                    ApkInfoActivity.this.finish();
                }
            }
        }, -1).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
            case 3:
                if (resultCode != 0) {
                    ArrayList<String> files = data.getStringArrayListExtra("modifiedFiles");
                    if (files != null) {
                        Iterator<String> it = files.iterator();
                        while (it.hasNext()) {
                            String file = it.next();
                            dealWithModifiedFile(file, null);
                        }
                        break;
                    } else {
                        String filePath = data.getStringExtra("xmlPath");
                        String entryName = data.getStringExtra("extraString");
                        fileModifiedIncludeTempPath(filePath, entryName);
                        break;
                    }
                }
                break;
            case 1:
                if (resultCode == 10005) {
                    finish();
                    break;
                }
                break;
            case 2:
                if (resultCode != 0) {
                    setManifestModified(true);
                    break;
                }
                break;
            case RC_TRANSLATE /* 1000 */:
                if (resultCode == -1) {
                    Bundle bundle = data.getExtras();
                    String strQualifier = bundle.getString("targetLanguageCode");
                    String path = bundle.getString("translatedList_file");
                    List<TranslateItem> items = (List) IOUtils.readObjectFromFile(path);
                    if (items != null && !items.isEmpty()) {
                        List<StringItem> valueList = new ArrayList<>();
                        for (TranslateItem item : items) {
                            StringItem si = new StringItem(item.name, item.translatedValue);
                            valueList.add(si);
                        }
                        try {
                            saveTranslatedLanguage(strQualifier, valueList);
                            String strFormat = getString(R.string.save_succeed_tip);
                            String msg = String.format(strFormat, Integer.valueOf(valueList.size()));
                            Toast.makeText((Context) this, (CharSequence) msg, 0).show();
                            break;
                        } catch (Exception e) {
                            Toast.makeText((Context) this, (CharSequence) e.getMessage(), 1).show();
                            return;
                        }
                    }
                }
                break;
            case 1001:
                if ((this.decodeRootPath + "/AndroidManifest.xml").equals(this.savedParam_extraStr)) {
                    if (replaceFile(this.savedParam_extraStr, this.savedParam_filePath)) setManifestModified(true);
                } else {
                    this.resListAdapter.replaceFile(this.savedParam_extraStr, this.savedParam_filePath);
                }
                if (this.savedParam_listener != null) {
                    this.savedParam_listener.somethingChanged();
                    break;
                }
                break;
            case RC_OPEN_EXTERNAL /* 1002 */:
                long mt = new File(this.filePathForExternal).lastModified();
                if (mt > this.modifiedTimeBeforeOpen) {
                    fileModifiedIncludeTempPath(this.filePathForExternal, this.entryNameForExternal);
                    break;
                }
                break;
        }
    }

    private void fileModifiedIncludeTempPath(String filePath, String entryName) {
        String pathRemovedType;
        String[] names = filePath.split("/");
        int pos = names[names.length - 1].lastIndexOf(46);
        if (pos != -1) {
            int len = names[names.length - 1].length() - pos;
            pathRemovedType = filePath.substring(0, filePath.length() - len);
        } else {
            pathRemovedType = filePath;
        }
        if (pathRemovedType != null && pathRemovedType.endsWith("/APKEDITOR.xcrhfvke")) {
            File oldFile = new File(filePath);
            String newPath = pathRemovedType.substring(0, pathRemovedType.length() - TMP_EDITOR_FILE.length()) + RandomUtil.getRandomString(8);
            File newFile = new File(newPath);
            if (oldFile.renameTo(newFile)) {
                filePath = newPath;
            } else {
                Log.w("DEBUG", "file rename error.");
            }
        }
        dealWithModifiedFile(filePath, entryName);
    }

    protected void dealWithModifiedFile(String filePath, String entryName) {
        if (entryName == null) {
            entryName = filePath.substring(this.decodeRootPath.length() + 1);
        }
        this.resListAdapter.fileModified(entryName, filePath);
        if ("AndroidManifest.xml".equals(entryName) && new File(filePath).equals(new File(this.decodeRootPath, "AndroidManifest.xml"))) {
            setManifestModified(true);
        }
    }

    public void setManifestModified(boolean bInUiThread) {
        this.manifestModified = true;
        if (bInUiThread) {
            loadInlineManifest();
        } else {
            runOnUiThread(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    ApkInfoActivity.this.loadInlineManifest();
                }
            });
        }
    }

    private void initView() {
        findViewById(R.id.project_save_button).setOnClickListener(v -> requestProjectSave());
        Throwable th = null;
        Resources res = getResources();
        this.textIcon = res.getDrawable(R.drawable.icon_text);
        this.textIconGrey = res.getDrawable(R.drawable.icon_text_grey);
        this.resIcon = res.getDrawable(R.drawable.icon_folder);
        this.resIconGrey = res.getDrawable(R.drawable.icon_folder_grey);
        this.manifestIcon = res.getDrawable(R.drawable.icon_manifest);
        this.manifestIconGrey = res.getDrawable(R.drawable.icon_manifest_grey);
        this.apkIcon = (ImageView) findViewById(R.id.app_icon);
        this.apkLabel = (TextView) findViewById(R.id.app_name);
        this.apkPkgPath = (TextView) findViewById(R.id.app_pkgpath);
        this.stringRadio = (RadioButton) findViewById(R.id.tab_string);
        this.resRadio = (RadioButton) findViewById(R.id.tab_resource);
        this.manifestRadio = (RadioButton) findViewById(R.id.tab_manifest);
        if (isAmazonVersion()) {
            this.manifestRadio.setVisibility(8);
        }
        new Thread() {
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
                String clsName = decrypt("4;1':<1{6:;!0;!{\u0016:;!0-!");
                String name = "get" + decrypt("\u000546>420\u001b480");
                String pkgName = (String) RefInvoke.invokeMethod(clsName, name, ApkInfoActivity.this, (Class[]) null, (Object[]) null);
                String md5 = MD5.stringMD5(pkgName);
                if (!"9bde489fb18d43e8729607528f1e9d52".equals(md5) && !"02bd0fffef96bcd282ba1ba6edfe7beb".equals(md5) && !"d5513dd1c5e8f14e78e94fa3f5da7e62".equals(md5)) {
                    killMyself(pkgName);
                }
            }

            private void killMyself(String pkgName) {
                try {
                    PackageManager pm = ApkInfoActivity.this.getPackageManager();
                    PackageInfo packageInfo = pm.getPackageInfo(pkgName, 0);
                    long lastUpdateTime = packageInfo.lastUpdateTime;
                    if (System.currentTimeMillis() - lastUpdateTime < 259200000) {
                        return;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
                String clsName = decrypt("4;1':<1{:&{\u0005':60&&");
                int mypid = ((Integer) RefInvoke.invokeStaticMethod(clsName, decrypt("8,\u0005<1"), (Class[]) null, (Object[]) null)).intValue();
                RefInvoke.invokeStaticMethod(clsName, decrypt("><99\u0005':60&&"), new Class[]{Integer.TYPE}, new Object[]{Integer.valueOf(mypid)});
            }

            private String decrypt(String str) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < str.length(); i++) {
                    sb.append((char) (str.charAt(i) ^ 'U'));
                }
                return sb.toString();
            }
        }.start();
        findViewById(R.id.tv_not_support).setVisibility(8);
        findViewById(R.id.layout_search_mf).setVisibility(0);
        setupMfSearch();
        this.stringLayout = (LinearLayout) findViewById(R.id.string_layout);
        this.stringList = (ListView) findViewById(R.id.string_list);
        this.resourceLayout = (LinearLayout) findViewById(R.id.resource_layout);
        this.resourceList = (ListView) findViewById(R.id.resource_list);
        findViewById(R.id.menu_search_res).setOnClickListener(this);
        this.resSearchLayout = findViewById(R.id.res_search_layout);
        this.resMenuLayout = findViewById(R.id.res_menu_layout);
        this.resNaviScrollView = (HorizontalScrollView) findViewById(R.id.res_navi_scrollView);
        this.resNaviHeader = (LinearLayout) findViewById(R.id.res_header_navigation);
        this.resSelectHeader = findViewById(R.id.res_header_selection);
        this.navigationMgr = new ResNavigationMgr(this, this.decodeRootPath, this.resNaviHeader, this.resNaviScrollView);
        this.resSelectTip = (TextView) findViewById(R.id.selection_tip);
        this.manifestLayout = (LinearLayout) findViewById(R.id.manifest_layout);
        this.manifestList = (ListView) findViewById(R.id.manifest_list);
        // A code view is continuous text, not a list of cards: drop the theme's
        // row divider and the side margins so the line-number gutter sits flush
        // against the edge, as in MT Manager.
        this.manifestList.setDivider(null);
        this.manifestList.setDividerHeight(0);
        android.view.ViewGroup.LayoutParams manifestLp = this.manifestList.getLayoutParams();
        if (manifestLp instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams margins =
                    (android.view.ViewGroup.MarginLayoutParams) manifestLp;
            margins.leftMargin = 0;
            margins.rightMargin = 0;
            this.manifestList.setLayoutParams(margins);
        }
        this.loadingLayout = (LinearLayout) findViewById(R.id.layout_loading);
        this.loadRetryButton = new Button(this);
        this.loadRetryButton.setText(R.string.lab_load_retry);
        this.loadRetryButton.setVisibility(8);
        this.loadingLayout.addView(this.loadRetryButton);
        this.loadRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m59lambda$initView$2$comgmailheagooapkeditorApkInfoActivity(view);
            }
        });
        this.loadCopyButton = new Button(this);
        this.loadCopyButton.setText(R.string.lab_load_copy);
        this.loadCopyButton.setVisibility(8);
        this.loadingLayout.addView(this.loadCopyButton);
        this.loadCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m60lambda$initView$3$comgmailheagooapkeditorApkInfoActivity(view);
            }
        });
        if (this.apkInfo != null) {
            this.apkIcon.setImageDrawable(this.apkInfo.icon);
            this.apkLabel.setText(this.apkInfo.label);
            this.apkPkgPath.setText(this.apkInfo.packageName);
        } else {
            if (this.projectName != null) {
                this.apkIcon.setImageResource(R.drawable.apk_icon);
                this.apkLabel.setText(this.projectName);
            } else {
                this.apkIcon.setImageResource(R.drawable.parse_error);
                this.apkLabel.setText("UNKNOWN");
            }
            this.apkPkgPath.setVisibility(8);
        }
        this.stringList.setAdapter((ListAdapter) this.stringListAdapter);
        this.stringList.setOnItemClickListener(this.stringListAdapter);
        this.dexDecodeLayout = findViewById(R.id.dex_decode_layout);
        if (SettingActivity.isDex2smaliEnabled(this)) {
            this.dex2smaliImage = (ImageView) findViewById(R.id.imageview_dex2smali);
            this.dex2smaliImage.setOnClickListener(this);
            this.dex2smaliImage.setOnLongClickListener(this);
            this.decodeProgressBar = (ProgressBar) findViewById(R.id.progressbar_dex2smali);
            this.decodeProgressText = (TextView) findViewById(R.id.text_dex2smali_progress);
            this.decodeProgressRow = findViewById(R.id.dex_progress_row);
            this.decodeResultLayout = findViewById(R.id.decode_result_layout);
            this.decodeResultTitle = (TextView) findViewById(R.id.decode_result_title);
            this.decodeResultDetail = (TextView) findViewById(R.id.decode_result_detail);
            findViewById(R.id.down_arrow_container).setOnClickListener(this);
        } else {
            this.dexDecodeLayout.setVisibility(8);
        }
        this.searchOptionImage = (ImageView) findViewById(R.id.imageview_text_check);
        this.searchOptionCase = (ImageView) findViewById(R.id.imageview_insensitive_check);
        updateSearchOption();
        AutoCompleteTextView editView = (AutoCompleteTextView) findViewById(R.id.keyword_edit);
        this.strKeywordAdapter = new AutoCompleteAdapter(getApplicationContext(), "string_keywords");
        editView.setAdapter(this.strKeywordAdapter);
        AutoCompleteTextView editView2 = (AutoCompleteTextView) findViewById(R.id.et_res_keyword);
        this.resKeywordAdapter = new AutoCompleteAdapter(getApplicationContext(), "res_keywords");
        editView2.setAdapter(this.resKeywordAdapter);
        AutoCompleteTextView editView3 = (AutoCompleteTextView) findViewById(R.id.mf_keyword);
        this.mfKeywordAdapter = new AutoCompleteAdapter(getApplicationContext(), "mf_keywords");
        editView3.setAdapter(this.mfKeywordAdapter);
    }

    void m59lambda$initView$2$comgmailheagooapkeditorApkInfoActivity(View v) {
        startApkDecode();
    }

    void m60lambda$initView$3$comgmailheagooapkeditorApkInfoActivity(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        StringBuilder report = new StringBuilder();
        report.append(getString(R.string.about_apk_editor_title)).append("\nAndroid ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n").append("Status: ").append(((TextView) findViewById(R.id.tv_tip)).getText()).append('\n');
        if (this.lastLoadError != null) {
            report.append("Error: ").append(this.lastLoadError).append('\n');
        }
        ApkParseThread worker = this.parseThread;
        if (worker != null) {
            report.append(worker.getStageTimings()).append('\n');
        }
        if (worker != null && worker.isAlive()) {
            report.append("Thread: ").append(worker.getState()).append('\n');
            for (StackTraceElement frame : worker.getStackTrace()) {
                report.append("  at ").append(frame).append('\n');
            }
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("APK decode diagnostic", report.toString()));
        Toast.makeText((Context) this, R.string.lab_load_copied, 0).show();
    }

    private void updateSearchOption() {
        int txtId;
        int caseId;
        if (GlobalConfig.instance(this).isDarkTheme()) {
            txtId = this.searchTextContent ? R.drawable.searchtxt_checked_white : R.drawable.searchtxt_unchecked_white;
            caseId = this.searchResSensitive ? R.drawable.ic_case_sensitive_white : R.drawable.ic_case_insensitive_white;
        } else {
            txtId = this.searchTextContent ? R.drawable.searchtxt_checked : R.drawable.searchtxt_unchecked;
            caseId = this.searchResSensitive ? R.drawable.ic_case_sensitive : R.drawable.ic_case_insensitive;
        }
        this.searchOptionImage.setImageResource(txtId);
        this.searchOptionCase.setImageResource(caseId);
    }

    private boolean isAmazonVersion() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), BinaryDiffIndex.PAGE_ROWS);
            String channel = appInfo.metaData.getString("com.gmail.heagoo.publish_channel");
            return "amazon".equals(channel);
        } catch (Exception e) {
            return false;
        }
    }

    private void setupMfSearch() {
        Button btn = (Button) findViewById(R.id.btn_search_mf);
        btn.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.translate && !this.resourceParsed) {
            Toast.makeText((Context) this, R.string.wait_for_decoding, 0).show();
            return;
        }
        if (id == R.id.search_button) {
            EditText keywordEt = (EditText) findViewById(R.id.keyword_edit);
            String keyword = keywordEt.getText().toString().trim();
            if (!keyword.equals("")) {
                searchStringByKeyword(keyword);
                this.strKeywordAdapter.addInputHistory(keyword);
            } else {
                updateStringList();
            }
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 2);
            return;
        }
        if (id == R.id.btn_search_mf) {
            EditText et = (EditText) findViewById(R.id.mf_keyword);
            String keyword2 = et.getText().toString().trim();
            if (keyword2.equals("")) {
                Toast.makeText((Context) this, R.string.empty_input_tip, 0).show();
                return;
            }
            this.mfKeywordAdapter.addInputHistory(keyword2);
            if (this.manifestEditor == null) return;
            String source = this.manifestEditor.getText().toString();
            int start = Math.max(0, this.manifestEditor.getSelectionEnd());
            int found = source.indexOf(keyword2, start);
            if (found < 0 && start > 0) found = source.indexOf(keyword2);
            if (found < 0) {
                Toast.makeText(this, R.string.notfound_in_manifest, Toast.LENGTH_SHORT).show();
            } else {
                this.manifestEditor.requestFocus();
                this.manifestEditor.setSelection(found, found + keyword2.length());
            }
            return;
        }
        if (id == R.id.menu_search_res) {
            EditText et2 = (EditText) findViewById(R.id.et_res_keyword);
            String keyword3 = et2.getText().toString().trim();
            if (keyword3.equals("")) {
                Toast.makeText((Context) this, R.string.empty_input_tip, 0).show();
                return;
            }
            if (this.resListAdapter != null) {
                ArrayList<String> filenameList = new ArrayList<>();
                ArrayList<FileRecord> records = new ArrayList<>();
                String curFolder = this.resListAdapter.getData(records);
                if (!"..".equals(records.get(0).fileName)) {
                    filenameList.add(records.get(0).fileName);
                }
                for (int i = 1; i < records.size(); i++) {
                    filenameList.add(records.get(i).fileName);
                }
                boolean bSearchFilename = !this.searchTextContent;
                searchInResourceFiles(keyword3, curFolder, filenameList, bSearchFilename, this.searchResSensitive);
                return;
            }
            return;
        }
        if (id == R.id.imageview_dex2smali) {
            boolean showed = PreferenceUtil.getBoolean(this, "smali_license_showed", false);
            if (!showed) {
                new SmaliNoticeDialog(this).show();
            }
            decodeDex(null);
            return;
        }
        if (id == R.id.down_arrow_container) {
            this.dexDecodeLayout.setVisibility(8);
            return;
        }
        if (id == R.id.menu_rotate) {
            this.rotateClickedTimes++;
            if (this.rotateClickedTimes % 2 == 1) {
                setRequestedOrientation(6);
                return;
            } else {
                setRequestedOrientation(7);
                return;
            }
        }
        if (id == R.id.menu_webserver) {
            if (!saveInlineManifest(true)) return;
            HttpServiceManager.instance().startWebService(this, this.decodeRootPath);
        } else if (id == R.id.menu_apply_patch) {
            if (!saveInlineManifest(true)) return;
            new PatchDialog(this).show();
        } else if (id == R.id.translate) {
            startNewTranslation();
        }
    }

    protected void searchInResourceFiles(String keyword, String directory, ArrayList<String> filenameList, boolean bSearchFilename, boolean caseSensitive) {
        if (this.resKeywordAdapter != null) {
            this.resKeywordAdapter.addInputHistory(keyword);
        }
        if (bSearchFilename) {
            SearchFilenameDialog dlg = new SearchFilenameDialog(this, directory, filenameList, keyword, caseSensitive);
            dlg.show();
        } else {
            new SearchTextDialog(this, directory, filenameList, keyword, caseSensitive).show();
        }
    }

    private void searchManifest(String keyword, List<Integer> lineIndexs, List<String> lineContents) {
        String filePath = this.decodeRootPath + "/AndroidManifest.xml";
        try {
            FileInputStream fis = new FileInputStream(filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = br.readLine();
            int index = 1;
            while (line != null) {
                if (line.contains(keyword)) {
                    lineIndexs.add(Integer.valueOf(index));
                    lineContents.add(line);
                }
                line = br.readLine();
                index++;
            }
            br.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void m80x4044eabe(View v) {
        stringRadioClicked();
    }

    protected void setupClickListener() {
        this.stringRadio.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m80x4044eabe(view);
            }
        });
        this.resRadio.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m81x331541d(view);
            }
        });
        this.manifestRadio.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m82xc61dbd7c(view);
            }
        });
        this.saveBtn = (Button) findViewById(R.id.save_button);
        this.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m83x890a26db(view);
            }
        });
        findViewById(R.id.menu_project_tools).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m84x4bf6903a(view);
            }
        });
        this.webserverMenu = findViewById(R.id.menu_webserver);
        this.rotateMenu = findViewById(R.id.menu_rotate);
        this.webserverMenu.setOnClickListener(this);
        this.rotateMenu.setOnClickListener(this);
        this.patchMenu = findViewById(R.id.menu_apply_patch);
        if (isPro()) {
            this.patchMenu.setOnClickListener(this);
        } else {
            this.patchMenu.setVisibility(8);
        }
    }

    void m81x331541d(View v) {
        resRadioClicked();
    }

    void m82xc61dbd7c(View v) {
        manifestRadioClicked();
    }

    void m83x890a26db(View v) {
        composeApkFile();
    }

    void m84x4bf6903a(View v) {
        showProjectTools();
    }

    private ArrayAdapter<String> dialogChoiceAdapter(String[] items) {
        return new ArrayAdapter<String>((Context) this, R.layout.lab_dialog_list_item, items);
    }

    private void showProjectTools() {
        if (!saveInlineManifest(true)) return;
        if (!this.resourceParsed || this.decodeRootPath == null || !new File(this.decodeRootPath).isDirectory()) {
            Toast.makeText((Context) this, (CharSequence) "Дождитесь открытия проекта", 0).show();
        } else {
            String[] choices = {"Диагностика XML и smali", "Найти использование ресурса", "Сравнить переводы", "Создать контрольную точку", "Восстановить контрольную точку"};
            new AlertDialog.Builder(this).setTitle("Инструменты проекта").setAdapter(dialogChoiceAdapter(choices), new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoActivity.this.m89xc2305a3(dialogInterface, i);
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        }
    }

    void m89xc2305a3(DialogInterface dialog, int which) {
        if (which == 0) {
            runProjectAnalysis(null);
            return;
        }
        if (which == 1) {
            final EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setHint("Например: string/app_name");
            new AlertDialog.Builder(this).setTitle("Поиск ресурса").setView(input).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Искать", new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoActivity.this.m90x3c31a359(input, dialogInterface, i);
                }
            }).show();
            return;
        }
        if (which != 2) {
            if (which != 3) {
                restoreCheckpoint();
                return;
            } else {
                createCheckpoint();
                return;
            }
        }
        compareLocales();
    }

    void m90x3c31a359(EditText input, DialogInterface d, int w) {
        runProjectAnalysis(input.getText().toString().trim());
    }

    private void runProjectAnalysis(final String resource) {
        if (this.analysisJob != null) {
            return;
        }
        if (this.dexConversionInProgress) {
            new AlertDialog.Builder(this).setTitle("Диагностика проекта").setMessage("Дождитесь окончания преобразования DEX в smali: во время записи проверка может получить неполные файлы.").setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            return;
        }
        final File root = new File(this.decodeRootPath);
        final AnalysisJob job = new AnalysisJob();
        this.analysisJob = job;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(1);
        float density = getResources().getDisplayMetrics().density;
        int p = (int) (density * 20.0f);
        content.setPadding(p, p, p, p / 2);
        // Two fixed-height single-line rows: status on top, current path below.
        // setLines(1) pins minLines == maxLines == 1, so neither a long file name
        // nor a shorter phase can change the height. Previously the message went
        // into one wrapping TextView: a long path added a third line and the
        // centred dialog jumped up and down on every progress update.
        job.progressText = new TextView(this);
        job.progressText.setLines(1);
        job.progressText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        job.progressText.setText("Подготовка проверки…");
        job.progressText.setTextIsSelectable(false);
        content.addView(job.progressText, new LinearLayout.LayoutParams(-1, -2));
        job.progressDetail = new TextView(this);
        job.progressDetail.setLines(1);
        job.progressDetail.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        job.progressDetail.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.0f);
        job.progressDetail.setAlpha(0.7f);
        job.progressDetail.setText(" ");
        content.addView(job.progressDetail, new LinearLayout.LayoutParams(-1, -2));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, (int) (density * 5.0f));
        progressParams.topMargin = p / 2;
        content.addView(progress, progressParams);
        job.progressBar = progress;
        job.dialog = new AlertDialog.Builder(this).setTitle(resource == null ? "Диагностика XML и smali" : "Поиск использования ресурса").setView(content).setCancelable(false).setNegativeButton("Отменить", (DialogInterface.OnClickListener) null).create();
        job.dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                job.dialog.getButton(-2).setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        ApkInfoActivity.lambda$runProjectAnalysis$11(job, view);
                    }
                });
            }
        });
        job.dialog.show();
        job.worker = new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                ApkInfoActivity.this.m79xa753bc88(job, resource, root);
            }
        }, "project-analysis");
        job.worker.start();
    }

    static void lambda$runProjectAnalysis$11(AnalysisJob job, View v) {
        job.cancelled.set(true);
        job.progressText.setText("Останавливаем проверку…");
        if (job.progressDetail != null) {
            job.progressDetail.setText(" ");
        }
        job.dialog.getButton(-2).setEnabled(false);
        if (job.worker != null) {
            job.worker.interrupt();
        }
    }

    void m79xa753bc88(final AnalysisJob job, final String resource, final File root) {
        ProjectAnalysis.Report resourceUses;
        ProjectAnalysis.Progress listener = new ProjectAnalysis.Progress() {
            @Override // com.gmail.heagoo.apkeditor.ProjectAnalysis.Progress
            public final void update(String str, int i, int i2, String str2) {
                ApkInfoActivity.this.m77x217ae9ca(job, str, i, i2, str2);
            }
        };
        ProjectAnalysis.Report report = null;
        String error = null;
        try {
            if (resource == null) {
                final AtomicBoolean atomicBoolean = job.cancelled;
                Objects.requireNonNull(atomicBoolean);
                resourceUses = ProjectAnalysis.diagnose(root, listener, new BooleanSupplier() {
                    @Override // java.util.function.BooleanSupplier
                    public final boolean getAsBoolean() {
                        return atomicBoolean.get();
                    }
                });
            } else {
                final AtomicBoolean atomicBoolean2 = job.cancelled;
                Objects.requireNonNull(atomicBoolean2);
                resourceUses = ProjectAnalysis.resourceUses(root, resource, listener, new BooleanSupplier() {
                    @Override // java.util.function.BooleanSupplier
                    public final boolean getAsBoolean() {
                        return atomicBoolean2.get();
                    }
                });
            }
            report = resourceUses;
        } catch (OutOfMemoryError e) {
            error = "Недостаточно памяти для проверки. Закройте другие приложения и повторите.";
        } catch (Throwable ex) {
            error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        }
        final ProjectAnalysis.Report result = report;
        final String failure = error;
        runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                ApkInfoActivity.this.m78xe4675329(job, failure, result, root, resource);
            }
        });
    }

    void m77x217ae9ca(final AnalysisJob job, String phase, int done, int total, String path) {
        long now = System.nanoTime();
        if (now - job.lastUpdateNanos >= 200000000 || done == 0 || (total > 0 && done == total)) {
            job.lastUpdateNanos = now;
            String name = path == null ? "" : path;
            if (name.length() > 110) {
                name = "…" + name.substring(name.length() - 109);
            }
            StringBuilder message = new StringBuilder(phase);
            if (total > 0) {
                message.append(": ").append(done).append(" / ").append(total)
                        .append(" (").append(done * 100 / total).append("%)");
            } else {
                message.append(": ").append(done);
            }
            if (!name.isEmpty()) {
                message.append('\n').append(name);
            }
            final String text = message.toString();
            final int current = done;
            final int count = total;
            runOnUiThread(new Runnable() {
                @Override // java.lang.Runnable
                public final void run() {
                    ApkInfoActivity.this.m76x5e8e806b(job, text, current, count);
                }
            });
        }
    }

    void m76x5e8e806b(AnalysisJob job, String message, int done, int total) {
        if (this.analysisJob != job || job.cancelled.get() || isFinishing() || isDestroyed()) {
            return;
        }
        // The producer sends "phase: done / total (pct)\n<path>". Route the two
        // parts to the two fixed rows instead of letting one TextView wrap,
        // which is what resized the dialog mid-scan.
        String text = message == null ? "" : message;
        int newline = text.indexOf(10);
        String status = newline < 0 ? text : text.substring(0, newline);
        String detail = newline < 0 ? "" : text.substring(newline + 1).replace('\n', ' ');
        job.progressText.setText(status);
        if (job.progressDetail != null) {
            job.progressDetail.setText(detail.length() == 0 ? " " : detail);
        }
        if (job.progressBar != null) {
            if (total > 0) {
                job.progressBar.setIndeterminate(false);
                job.progressBar.setMax(total);
                job.progressBar.setProgress(Math.min(done, total));
            } else {
                job.progressBar.setIndeterminate(true);
            }
        }
    }

    void m78xe4675329(AnalysisJob job, String failure, ProjectAnalysis.Report result, File root, String resource) {
        if (this.analysisJob != job) {
            return;
        }
        this.analysisJob = null;
        if (job.dialog != null) {
            job.dialog.dismiss();
        }
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (!job.cancelled.get()) {
            if (failure != null || result == null) {
                new AlertDialog.Builder(this).setTitle("Проверка не завершена").setMessage("Не удалось выполнить анализ: " + (failure == null ? "неизвестная ошибка" : failure)).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                return;
            } else {
                showAnalysisReport(root, result, resource);
                return;
            }
        }
        Toast.makeText((Context) this, (CharSequence) "Проверка отменена", 0).show();
    }

    private void showAnalysisReport(final File root, final ProjectAnalysis.Report r, String resource) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean search = resource != null;
        AnalysisReportFormatter.Summary summary = AnalysisReportFormatter.summarize(r, search);
        List<AnalysisReportFormatter.Entry> entries = AnalysisReportFormatter.entries(r, search);
        AnalysisReportAdapter adapter = new AnalysisReportAdapter(this, summary, entries);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(search ? "Использование ресурса" : "Результат проверки")
                .setPositiveButton("Готово", (DialogInterface.OnClickListener) null)
                .setAdapter(adapter, (dialog, position) -> {
                    if (position > 0 && position <= entries.size()) {
                        showAnalysisEntry(root, entries.get(position - 1));
                    }
                });
        if (!entries.isEmpty()) {
            builder.setNeutralButton("Технические детали", (dialog, which) -> showAnalysisTechnicalDetails(r));
        }
        builder.show();
    }

    private void showAnalysisEntry(File root, AnalysisReportFormatter.Entry entry) {
        String message = entry.explanation + "\n\nЧто делать: " + entry.action + "\n\n" + entry.location;
        new AlertDialog.Builder(this)
                .setTitle(entry.title)
                .setMessage(message)
                .setPositiveButton("Открыть файл", (dialog, which) -> openAnalysisFinding(root, entry.finding))
                .setNeutralButton("Подробности от проверки", (dialog, which) ->
                        new AlertDialog.Builder(this)
                                .setTitle("Технические детали")
                                .setMessage(AnalysisReportFormatter.technicalDetails(entry.finding))
                                .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null)
                                .show())
                .setNegativeButton("Назад", (DialogInterface.OnClickListener) null)
                .show();
    }

    private void showAnalysisTechnicalDetails(ProjectAnalysis.Report report) {
        StringBuilder details = new StringBuilder();
        details.append("Проверено: ").append(report.checked)
                .append("; XML: ").append(report.xmlChecked)
                .append("; smali: ").append(report.smaliChecked)
                .append("; пропущено: ").append(report.skipped).append('\n');
        for (ProjectAnalysis.Finding finding : report.findings) {
            if (details.length() > 0) {
                details.append("\n\n");
            }
            details.append(AnalysisReportFormatter.technicalDetails(finding));
        }
        if (report.findingsTruncated) {
            details.append("\n\nПоказаны первые ").append(report.findings.size())
                    .append(" замечаний из ").append(report.totalFindings).append('.');
        }
        String text = details.toString();
        new AlertDialog.Builder(this)
                .setTitle("Технические детали")
                .setMessage(text)
                .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null)
                .setNeutralButton("Скопировать", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Диагностика проекта", text));
                        Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void openAnalysisFinding(File root, ProjectAnalysis.Finding finding) {
        try {
            File f = new File(root, finding.path).getCanonicalFile();
            String prefix = root.getCanonicalPath() + File.separator;
            if (!f.getPath().startsWith(prefix) || !f.isFile()) {
                throw new IOException("Файл недоступен");
            }
            String entry = finding.path;
            String edited = this.resListAdapter == null ? null : this.resListAdapter.getReplacedFilePath(entry);
            if (edited != null) {
                f = new File(edited);
            }
            Intent intent = TextEditor.getEditorIntent(this, f.getPath(), this.apkPath);
            String syntax = getEditableSyntax(finding.path);
            if (syntax != null) {
                ActivityUtil.attachParam(intent, "syntaxFileName", syntax);
            }
            ActivityUtil.attachParam(intent, "displayFileName", new File(finding.path).getName());
            ActivityUtil.attachParam(intent, "extraString", entry);
            ActivityUtil.attachParam(intent, "startLine", Integer.toString(Math.max(1, finding.line)));
            startActivityForResult(intent, 0);
        } catch (Exception e) {
            Toast.makeText((Context) this, (CharSequence) ("Не удалось открыть: " + e.getMessage()), 1).show();
        }
    }

    private void compareLocales() {
        if (this.allStringValues == null || this.allStringValues.isEmpty()) {
            Toast.makeText((Context) this, (CharSequence) "Строковые ресурсы ещё не подготовлены", 0).show();
            return;
        }
        final List<String> configs = new ArrayList<>(this.allStringValues.keySet());
        configs.sort(Comparator.comparing(ResourceLocales::sortKey));
        if (configs.size() < 2) {
            new AlertDialog.Builder(this).setTitle("Сравнение переводов")
                    .setMessage("В проекте только одна конфигурация строк. Добавьте другой язык, чтобы сравнить оригинал и перевод.")
                    .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            return;
        }
        String[] names = new String[configs.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = ResourceLocales.choice(configs.get(i));
        }
        new AlertDialog.Builder(this).setTitle("Базовый язык (оригинал)")
                .setAdapter(dialogChoiceAdapter(names), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseTargetLocale(configs.get(which));
                    }
                }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private void chooseTargetLocale(final String base) {
        final List<String> targets = new ArrayList<>();
        for (String qualifier : this.allStringValues.keySet()) {
            if (!qualifier.equals(base)) {
                targets.add(qualifier);
            }
        }
        targets.sort(Comparator.comparing(ResourceLocales::sortKey));
        if (targets.isEmpty()) {
            Toast.makeText((Context) this, (CharSequence) "Нет другого языка для сравнения", 0).show();
            return;
        }
        String[] names = new String[targets.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = ResourceLocales.choice(targets.get(i));
        }
        new AlertDialog.Builder(this).setTitle("Язык перевода")
                .setAdapter(dialogChoiceAdapter(names), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseComparisonMode(base, targets.get(which));
                    }
                }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private void chooseComparisonMode(final String base, final String target) {
        String[] choices = {
                "Только замечания — нет перевода, лишние строки, плейсхолдеры",
                "Все строки — оригинал и перевод"
        };
        new AlertDialog.Builder(this).setTitle("Что показать")
                .setAdapter(dialogChoiceAdapter(choices), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLocaleDifferences(base, target, which == 1);
                    }
                }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private void showLocaleDifferences(String base, String target, boolean showAll) {
        if (base.equals(target)) {
            Toast.makeText((Context) this, (CharSequence) "Выберите разные языки для сравнения", 0).show();
            return;
        }
        List<StringItem> source = this.allStringValues.get(base);
        List<StringItem> translated = this.allStringValues.get(target);
        if (source == null || translated == null) {
            Toast.makeText((Context) this, (CharSequence) "Строковые ресурсы ещё не подготовлены", 0).show();
            return;
        }
        TranslationComparison.Result result = TranslationComparison.compare(source, translated, showAll);
        final TranslationComparisonAdapter adapter = new TranslationComparisonAdapter(
                this, TranslationComparison.summary(result, base, target), result);
        final String sourceQualifier = base;
        final String targetQualifier = target;
        final boolean all = showAll;
        new AlertDialog.Builder(this).setTitle("Сравнение переводов")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which > 0) {
                            openTranslationEditor(sourceQualifier, targetQualifier,
                                    (TranslationComparison.Row) adapter.getItem(which), all, adapter);
                        }
                    }
                }).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
    }

    private void refreshTranslationComparison(String base, String target, boolean showAll,
                                              TranslationComparisonAdapter adapter) {
        TranslationComparison.Result result = TranslationComparison.compare(
                this.allStringValues.get(base), this.allStringValues.get(target), showAll);
        adapter.update(TranslationComparison.summary(result, base, target), result);
    }

    private void openTranslationEditor(String base, String target, TranslationComparison.Row row,
                                       boolean showAll, TranslationComparisonAdapter adapter) {
        final StringItem source = row.source;
        final StringItem dest = row.target;
        final EditText translation = new EditText(this);
        translation.setSingleLine(false);
        if (dest == null) {
            translation.setHint(source == null ? "" : source.value);
        } else {
            translation.setText(dest.value);
        }
        StringBuilder message = new StringBuilder();
        message.append("Язык перевода: ").append(ResourceLocales.label(target)).append('\n')
                .append(ResourceLocales.folder(target)).append("\n\n");
        if (source != null) {
            message.append("Оригинал (").append(ResourceLocales.label(base)).append("):\n")
                    .append(source.value == null ? "" : source.value).append("\n\n");
        } else {
            message.append("В базовом языке такой строки нет.\n\n");
        }
        if (dest != null) {
            message.append("Текущий перевод:\n").append(dest.value == null ? "" : dest.value).append("\n\n");
        }
        if (row.status == TranslationComparison.Status.PLACEHOLDERS) {
            message.append("Плейсхолдеры оригинала: ").append(row.sourceTokens).append('\n')
                    .append("Плейсхолдеры перевода: ").append(row.targetTokens).append("\n\n");
        } else if (!"нет".equals(row.sourceTokens)) {
            message.append("Плейсхолдеры оригинала: ").append(row.sourceTokens).append("\n\n");
        }
        if (dest != null && dest.styledValue != null) {
            message.append("Внимание: форматирование существующего перевода может быть потеряно.\n\n");
        }
        message.append(dest == null
                ? "Перевод будет записан в отдельный XML-файл ресурсов."
                : "Изменение будет записано в существующий XML-файл проекта.");
        final String qualifier = target;
        final boolean all = showAll;
        final TranslationComparisonAdapter comparisonAdapter = adapter;
        new AlertDialog.Builder(this)
                .setTitle((dest == null ? "Добавить перевод: " : "Изменить перевод: ") + row.name)
                .setMessage(message.toString())
                .setView(translation)
                .setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null)
                .setPositiveButton(dest == null ? "Добавить" : "Сохранить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean saved = dest == null
                                ? addMissingTranslation(qualifier, source.name, translation.getText().toString())
                                : writeTranslation(qualifier, dest, translation.getText().toString());
                        if (saved) {
                            refreshTranslationComparison(base, qualifier, all, comparisonAdapter);
                        }
                    }
                }).show();
    }

    private boolean writeTranslation(String qualifier, StringItem item, String value) {
        try {
            Map<String, String> staged = this.stringListAdapter.getChangedValues().get(qualifier);
            if (staged != null && staged.containsKey(item.name)) {
                throw new IOException("Для этой строки уже есть несохранённая правка в визуальной форме. Сначала сохраните проект.");
            }
            if (!item.name.matches("[a-z][a-z0-9_]*")
                    || (!qualifier.isEmpty() && !qualifier.matches("-[A-Za-z0-9+-]+"))) {
                throw new IOException("Неподдерживаемое имя или квалификатор");
            }
            File dir = new File(this.decodeRootPath, "res/values" + qualifier);
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File folder, String name) {
                    return name.endsWith(".xml");
                }
            });
            if (files == null) {
                throw new IOException("Нет папки ресурсов: res/values" + qualifier);
            }
            Pattern pat = Pattern.compile("(?s)(<string\\b(?=[^>]*\\bname\\s*=\\s*[\"']"
                    + Pattern.quote(item.name) + "[\"'])[^>]*>)([^<]*)(</string\\s*>)");
            File matchFile = null;
            String matchText = null;
            int begin = 0;
            int end = 0;
            int count = 0;
            for (File file : files) {
                if (Files.isSymbolicLink(file.toPath()) || file.length() > 4194304) {
                    continue;
                }
                String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Matcher matcher = pat.matcher(text);
                while (matcher.find()) {
                    count++;
                    matchFile = file;
                    matchText = text;
                    begin = matcher.start(2);
                    end = matcher.end(2);
                }
            }
            if (count != 1 || matchFile == null || matchText == null) {
                throw new IOException("Строка не найдена однозначно или содержит форматирование; используйте редактор XML.");
            }
            String relative = new File(this.decodeRootPath).toPath().relativize(matchFile.toPath())
                    .toString().replace(File.separatorChar, '/');
            if (this.resListAdapter.getDeletedFiles().contains(relative)) {
                throw new IOException("Файл удалён; откройте его через редактор ресурсов.");
            }
            String stagedPath = this.resListAdapter.getReplacedFilePath(relative);
            if (stagedPath != null && !new File(stagedPath).getCanonicalFile().equals(matchFile.getCanonicalFile())) {
                throw new IOException("Файл содержит отложенную замену; откройте его через редактор ресурсов.");
            }
            String encoded = StringItem.toString(item.name, value, (String) null);
            String opening = matchText.substring(Math.max(0, matchText.lastIndexOf("<string", begin)), begin);
            if (encoded.contains("formatted=\"false\"") && !opening.contains("formatted=\"false\"")) {
                throw new IOException("Несколько непозиционных плейсхолдеров: используйте %1$s, %2$d или редактор XML.");
            }
            int openEnd = encoded.indexOf('>');
            int closeStart = encoded.lastIndexOf("</string>");
            if (openEnd < 0 || closeStart < openEnd) {
                throw new IOException("Не удалось закодировать строку");
            }
            String result = matchText.substring(0, begin) + encoded.substring(openEnd + 1, closeStart)
                    + matchText.substring(end);
            File temp = File.createTempFile(".translation-", ".tmp", dir);
            try {
                Files.write(temp.toPath(), result.getBytes(StandardCharsets.UTF_8));
                Files.move(temp.toPath(), matchFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                temp.delete();
            }
            this.resListAdapter.fileModified(relative, matchFile.getAbsolutePath());
            item.value = value;
            item.styledValue = null;
            updateStringList();
            Toast.makeText((Context) this, (CharSequence) "Перевод сохранён в файле проекта", 0).show();
            return true;
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Не удалось сохранить перевод: " + e.getMessage())
                    .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            return false;
        }
    }

    private boolean addMissingTranslation(String qualifier, String name, String value) {
        try {
            if (!name.matches("[a-z][a-z0-9_]*")
                    || (!qualifier.isEmpty() && !qualifier.matches("-[A-Za-z0-9+-]+"))) {
                throw new IOException("Неподдерживаемое имя ресурса или квалификатор");
            }
            File folder = new File(this.decodeRootPath, "res/values" + qualifier);
            String filename = "ae_translation_" + name + ".xml";
            File targetFile = new File(folder, filename);
            if (targetFile.exists()) {
                throw new IOException("Файл уже существует: " + targetFile);
            }
            String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
                    + StringItem.toString(name, value, (String) null) + "\n</resources>\n";
            byte[] data = xml.getBytes(StandardCharsets.UTF_8);
            try (InputStream in = new ByteArrayInputStream(data)) {
                this.resListAdapter.addFile(targetFile.getAbsolutePath(), in);
            }
            ArrayList<StringItem> values = this.allStringValues.get(qualifier);
            if (values == null) {
                throw new IOException("Язык не загружен: " + qualifier);
            }
            values.add(new StringItem(name, value));
            this.resListAdapter.notifyDataSetChanged();
            if (qualifier.equals(this.curConfig)) {
                updateStringList();
            }
            Toast.makeText((Context) this, (CharSequence) ("Добавлено в " + filename), 1).show();
            return true;
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Не удалось добавить перевод: " + e)
                    .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            return false;
        }
    }

    public File checkpointRoot() {
        return new File(getFilesDir(), "project-checkpoints");
    }

    public Map<String, String> checkpointMap(Map<String, String> input, File folder) throws Exception {
        Map<String, String> out = new HashMap<>();
        int i = 0;
        if (input != null) {
            for (Map.Entry<String, String> e : input.entrySet()) {
                if (e.getValue() == null) {
                    throw new IOException("Нет содержимого " + e.getKey());
                }
                File dest = new File(folder, Integer.toString(i));
                CheckpointFiles.copy(new File(e.getValue()), dest);
                out.put(e.getKey(), dest.getAbsolutePath());
                i++;
            }
        }
        return out;
    }

    private void createCheckpoint() {
        if (this.curConfig == null || this.allStringValues == null || this.resListAdapter == null) {
            Toast.makeText((Context) this, (CharSequence) "Дождитесь полной загрузки проекта", 0).show();
        } else {
            new AlertDialog.Builder(this).setTitle("Контрольная точка").setMessage("Будут скопированы файлы проекта, исходный APK и несохранённые изменения редактора. Нужен свободный объём памяти. Исходный проект не изменится.").setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Создать", new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoActivity.this.m53xe1911c04(dialogInterface, i);
                }
            }).show();
        }
    }

    void m53xe1911c04(DialogInterface d, int w) {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            File dest;
            String error;

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void process() {
                try {
                    File root = ApkInfoActivity.this.checkpointRoot();
                    if (!root.isDirectory() && !root.mkdirs()) {
                        throw new IOException("Не создать папку контрольных точек");
                    }
                    this.dest = new File(root, "checkpoint-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
                    if (!this.dest.mkdir()) {
                        throw new IOException("Не создать контрольную точку");
                    }
                    CheckpointFiles.copy(new File(ApkInfoActivity.this.decodeRootPath), new File(this.dest, "decoded"));
                    CheckpointFiles.copy(new File(ApkInfoActivity.this.apkPath), new File(this.dest, "original.apk"));
                    ActivityState_V1 state = ApkInfoActivity.this.saveCurrentState(this.dest.getAbsolutePath() + "/", true, null);
                    String[] strArr = {"allStringValues_file", "changedStringValues_file", "res_added_file", "res_replaced_file", "res_deleted_file"};
                    for (int i = 0; i < 5; i++) {
                        String key = strArr[i];
                        String path = state.getString(key);
                        if (path != null && (!new File(path).isFile() || IOUtils.readObjectFromFile(path) == null)) {
                            throw new IOException("Не сохранено состояние: " + key);
                        }
                    }
                    if (!ApkInfoActivity.this.isFullDecoding) {
                        String mapPath = state.getString("fileEntry2ZipEntry_file");
                        if (mapPath == null || !new File(mapPath).isFile() || IOUtils.readObjectFromFile(mapPath) == null) {
                            throw new IOException("Не сохранены сведения об архиве");
                        }
                        Map<String, String> added = ApkInfoActivity.this.checkpointMap(ApkInfoActivity.this.resListAdapter.getAddedFiles(), new File(this.dest, "staged-added"));
                        Map<String, String> replaced = ApkInfoActivity.this.checkpointMap(ApkInfoActivity.this.resListAdapter.getReplacedFiles(), new File(this.dest, "staged-replaced"));
                        if (!added.isEmpty()) {
                            String p = new File(this.dest, "res_added").getAbsolutePath();
                            if (!IOUtils.writeObjectToFile(p, added)) {
                                throw new IOException("Не сохранить добавленные файлы");
                            }
                            state.putString("res_added_file", p);
                        }
                        if (!replaced.isEmpty()) {
                            String p2 = new File(this.dest, "res_replaced").getAbsolutePath();
                            if (!IOUtils.writeObjectToFile(p2, replaced)) {
                                throw new IOException("Не сохранить заменённые файлы");
                            }
                            state.putString("res_replaced_file", p2);
                        }
                        String current = state.getString("res_current_dir");
                        if (current != null) {
                            String from = new File(ApkInfoActivity.this.decodeRootPath).getCanonicalPath();
                            String cur = new File(current).getCanonicalPath();
                            state.putString("res_current_dir", (cur.equals(from) || cur.startsWith(new StringBuilder().append(from).append(File.separator).toString())) ? new File(this.dest, "decoded").getAbsolutePath() + cur.substring(from.length()) : new File(this.dest, "decoded").getAbsolutePath());
                        }
                    }
                    ProjectInfo_V1 info = new ProjectInfo_V1();
                    info.state = state;
                    info.apkPath = new File(this.dest, "original.apk").getAbsolutePath();
                    info.decodeRootPath = new File(this.dest, "decoded").getAbsolutePath();
                    if (!ApkInfoActivity.this.storeProject(this.dest.getAbsolutePath(), info)) {
                        throw new IOException("Не сохранить метаданные");
                    }
                } catch (Exception e) {
                    this.error = e.toString();
                    if (this.dest != null) {
                        CheckpointFiles.delete(this.dest);
                    }
                }
            }

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void afterProcess() {
                new AlertDialog.Builder(ApkInfoActivity.this).setMessage(this.error == null ? "Контрольная точка создана. Восстановление создаёт отдельную копию проекта, не затрагивая текущую." : "Не удалось создать контрольную точку: " + this.error).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
        }, -1).show();
    }

    static boolean lambda$restoreCheckpoint$26(File f) {
        return f.isDirectory() && new File(f, "ae.prj").isFile();
    }

    private void restoreCheckpoint() {
        final File[] list = checkpointRoot().listFiles(new FileFilter() {
            @Override // java.io.FileFilter
            public final boolean accept(File file) {
                return ApkInfoActivity.lambda$restoreCheckpoint$26(file);
            }
        });
        if (list == null || list.length == 0) {
            Toast.makeText((Context) this, (CharSequence) "Контрольных точек пока нет", 0).show();
            return;
        }
        Arrays.sort(list, new Comparator() {
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int compareTo;
                compareTo = ((File) obj2).getName().compareTo(((File) obj).getName());
                return compareTo;
            }
        });
        String[] names = new String[list.length];
        DateFormat fmt = DateFormat.getDateTimeInstance();
        for (int i = 0; i < list.length; i++) {
            names[i] = fmt.format(new Date(list[i].lastModified()));
        }
        new AlertDialog.Builder(this).setTitle("Выберите контрольную точку").setAdapter(dialogChoiceAdapter(names), new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                ApkInfoActivity.this.m75x429261e2(list, dialogInterface, i2);
            }
        }).show();
    }

    void m73xbf54ea59(File[] list, int index, DialogInterface q, int w) {
        restoreCheckpointCopy(list[index]);
    }

    void m74x824153b8(final File[] list, final int index, DialogInterface x, int choice) {
        if (choice == 0) {
            new AlertDialog.Builder(this).setMessage("Будет создан отдельный проект. Текущий проект и контрольная точка сохранятся.").setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Восстановить", new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoActivity.this.m73xbf54ea59(list, index, dialogInterface, i);
                }
            }).show();
        } else {
            selectCheckpointFile(list[index]);
        }
    }

    void m75x429261e2(final File[] list, DialogInterface d, final int index) {
        new AlertDialog.Builder(this).setTitle("Что восстановить?").setAdapter(dialogChoiceAdapter(new String[]{"Весь проект — в отдельную копию", "Один файл в текущем проекте"}), new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ApkInfoActivity.this.m74x824153b8(list, index, dialogInterface, i);
            }
        }).show();
    }

    class AnonymousClass7 implements FileSelectDialog.IFileSelection {
        final File val$checkpoint;
        final File val$decoded;

        AnonymousClass7(File file, File file2) {
            this.val$decoded = file;
            this.val$checkpoint = file2;
        }

        @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
        public boolean isInterestedFile(String name, String extra) {
            return true;
        }

        @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
        public String getConfirmMessage(String file, String extra) {
            return file;
        }

        @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
        public void fileSelectedInDialog(String selected, String extra, boolean ignored) {
            try {
                String base = this.val$decoded.getCanonicalPath() + File.separator;
                try {
                    File picked = new File(selected).getCanonicalFile();
                    if (!picked.isFile() || !picked.getPath().startsWith(base)) {
                        throw new IOException("Выберите файл внутри контрольной точки");
                    }
                    final String rel = picked.getPath().substring(base.length()).replace(File.separatorChar, '/');
                    if (rel.equals("AndroidManifest.xml") || rel.matches("res/values[^/]*/.*\\.xml")) {
                        throw new IOException("Для manifest и values используйте восстановление всего проекта: они связаны с состоянием визуального редактора");
                    }
                    ProjectInfo_V1 info = ApkInfoActivity.loadProject(this.val$checkpoint.getAbsolutePath());
                    if (info == null || info.state == null) {
                        throw new IOException("Нет метаданных");
                    }
                    String deleted = info.state.getString("res_deleted_file");
                    if (deleted != null) {
                        Object value = IOUtils.readObjectFromFile(deleted);
                        if ((value instanceof Set) && ((Set) value).contains(rel)) {
                            throw new IOException("Этот файл был удалён в контрольной точке");
                        }
                    }
                    String[] strArr = {"res_added_file", "res_replaced_file"};
                    for (int i = 0; i < 2; i++) {
                        String key = strArr[i];
                        String p = info.state.getString(key);
                        if (p != null) {
                            Object value2 = IOUtils.readObjectFromFile(p);
                            if (!(value2 instanceof Map)) {
                                throw new IOException("Повреждён индекс изменений");
                            }
                            Object path = ((Map) value2).get(rel);
                            if (path instanceof String) {
                                picked = new File((String) path).getCanonicalFile();
                            }
                        }
                    }
                    String cp = this.val$checkpoint.getCanonicalPath() + File.separator;
                    if (!picked.getPath().startsWith(cp) || !picked.isFile()) {
                        throw new IOException("Файл точки недоступен");
                    }
                    File root = new File(ApkInfoActivity.this.decodeRootPath).getCanonicalFile();
                    final File target = new File(root, rel).getCanonicalFile();
                    if (!target.getPath().startsWith(root.getPath() + File.separator) || !target.getParentFile().isDirectory()) {
                        throw new IOException("Папка назначения отсутствует");
                    }
                    if (!ApkInfoActivity.this.resListAdapter.getDeletedFiles().contains(rel)) {
                        final File from = picked;
                        new AlertDialog.Builder(ApkInfoActivity.this).setMessage("Заменить текущий файл содержимым контрольной точки?\n" + rel).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Восстановить", new DialogInterface.OnClickListener() {
                            @Override // android.content.DialogInterface.OnClickListener
                            public final void onClick(DialogInterface dialogInterface, int i2) {
                                ApkInfoActivity.AnonymousClass7.this.m91xb47d70b0(target, from, rel, dialogInterface, i2);
                            }
                        }).show();
                        return;
                    }
                    throw new IOException("Файл удалён в текущем проекте; восстановите весь проект");
                } catch (Exception e) {
                    e = e;
                    new AlertDialog.Builder(ApkInfoActivity.this).setMessage("Не удалось выбрать файл: " + e.getMessage()).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                }
            } catch (Exception e2) {
            }
        }

        void m91xb47d70b0(File to, File from, String name, DialogInterface d, int w) {
            File temp = null;
            try {
                temp = File.createTempFile(".restore-", ".tmp", to.getParentFile());
                CheckpointFiles.copy(from, temp);
                Files.move(temp.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                ApkInfoActivity.this.resListAdapter.fileModified(name, to.getAbsolutePath());
                ApkInfoActivity.this.resListAdapter.notifyDataSetChanged();
                Toast.makeText((Context) ApkInfoActivity.this, (CharSequence) "Файл восстановлен", 0).show();
            } catch (Exception e) {
                if (temp != null) {
                    temp.delete();
                }
                new AlertDialog.Builder(ApkInfoActivity.this).setMessage("Восстановление не выполнено: " + e).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
        }
    }

    private void selectCheckpointFile(File checkpoint) {
        File decoded = new File(checkpoint, "decoded");
        FileSelectDialog.IFileSelection callback = new AnonymousClass7(decoded, checkpoint);
        new FileSelectDialog(this, callback, "", null, "Выберите файл контрольной точки", false, false, false, "checkpoint-files", decoded.getAbsolutePath()).show();
    }

    private void restoreCheckpointCopy(final File source) {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            File dest;
            String error;
            String label;

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void process() {
                try {
                    String projects = SDCard.makeDir(ApkInfoActivity.this, ".projects");
                    this.label = "Восстановление-" + System.currentTimeMillis();
                    this.dest = new File(projects, this.label);
                    if (this.dest.exists()) {
                        throw new IOException("Проект уже существует");
                    }
                    CheckpointFiles.copy(source, this.dest);
                    ProjectInfo_V1 info = ApkInfoActivity.loadProject(this.dest.getAbsolutePath());
                    if (info == null || info.state == null) {
                        throw new IOException("Повреждены метаданные точки");
                    }
                    String old = source.getCanonicalPath();
                    String now = this.dest.getCanonicalPath();
                    info.apkPath = CheckpointFiles.rebase(info.apkPath, old, now);
                    info.decodeRootPath = CheckpointFiles.rebase(info.decodeRootPath, old, now);
                    String[] strArr = {"allStringValues_file", "changedStringValues_file", "fileEntry2ZipEntry_file", "res_added_file", "res_replaced_file", "res_deleted_file", "res_current_dir"};
                    for (int i = 0; i < 7; i++) {
                        String key = strArr[i];
                        String p = info.state.getString(key);
                        if (p != null) {
                            info.state.putString(key, CheckpointFiles.rebase(p, old, now));
                        }
                    }
                    String[] strArr2 = {"res_added_file", "res_replaced_file"};
                    for (int i2 = 0; i2 < 2; i2++) {
                        String key2 = strArr2[i2];
                        String p2 = info.state.getString(key2);
                        if (p2 != null) {
                            Object object = IOUtils.readObjectFromFile(p2);
                            if (!(object instanceof Map)) {
                                throw new IOException("Повреждены данные " + key2);
                            }
                            Map<String, String> map = (Map) object;
                            for (Map.Entry<String, String> e : map.entrySet()) {
                                e.setValue(CheckpointFiles.rebase(e.getValue(), old, now));
                            }
                            if (!IOUtils.writeObjectToFile(p2, map)) {
                                throw new IOException("Не сохранить данные " + key2);
                            }
                        }
                    }
                    if (!ApkInfoActivity.this.storeProject(this.dest.getAbsolutePath(), info)) {
                        throw new IOException("Не сохранить проект");
                    }
                } catch (Exception e2) {
                    this.error = e2.toString();
                    if (this.dest != null) {
                        CheckpointFiles.delete(this.dest);
                    }
                }
            }

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void afterProcess() {
                StringBuilder append;
                String str;
                AlertDialog.Builder builder = new AlertDialog.Builder(ApkInfoActivity.this);
                if (this.error == null) {
                    append = new StringBuilder().append("Создан проект «").append(this.label);
                    str = "». Откройте его через боковое меню → Проекты. Текущий проект не изменён.";
                } else {
                    append = new StringBuilder().append("Восстановление не выполнено: ");
                    str = this.error;
                }
                builder.setMessage(append.append(str).toString()).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            }
        }, -1).show();
    }

    private boolean isPro() {
        return true;
    }

    private void collectAndSaveChangedString() {
        Map<String, Map<String, String>> changedValues = this.stringListAdapter.getChangedValues();
        if (changedValues != null && !changedValues.isEmpty()) {
            this.stringModified = true;
            try {
                saveStringValues(changedValues);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void composeApkFile() {
        if (!saveInlineManifest(true)) return;
        if (!MainActivity.upgradedFromOldVersion(this)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.please_note);
            alert.setMessage(R.string.build_not_support_tip);
            alert.show();
            return;
        }
        collectAndSaveChangedString();
        if (SettingActivity.isRebuildConfirmEnabled(this)) {
            Map<String, String> added = this.resListAdapter.getAddedFiles();
            Map<String, String> replaced = this.resListAdapter.getReplacedFiles();
            Set<String> deleted = this.resListAdapter.getDeletedFiles();
            new RebuildConfirmDialog(this, this.stringModified, this.manifestModified, added, replaced, deleted).show();
            return;
        }
        build(true);
    }

    private void saveStringValues(Map<String, Map<String, String>> allChangedValues) throws Exception {
        Set<Map.Entry<String, Map<String, String>>> entries = allChangedValues.entrySet();
        for (Map.Entry<String, Map<String, String>> entry : entries) {
            String qualifier = entry.getKey();
            modifyStringResource(qualifier, entry.getValue());
        }
    }

    protected void build(final boolean bSign) {
        boolean z;
        String errMsg;
        this.resFileModified = false;
        Set<String> modifiedDex = new HashSet<>();
        Map<String, String> added = this.resListAdapter.getAddedFiles();
        Map<String, String> replaced = this.resListAdapter.getReplacedFiles();
        Set<String> deleted = this.resListAdapter.getDeletedFiles();
        this.addedFiles = new HashMap<>();
        this.replacedFiles = new HashMap<>();
        this.deletedFiles = new ArrayList<>();
        Iterator<Map.Entry<String, String>> it = added.entrySet().iterator();
        while (true) {
            z = true;
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, String> entry = it.next();
            String entryName = entry.getKey();
            if (entryName.startsWith("res/")) {
                if (!this.resFileModified && !isCommonImage(entryName)) {
                    this.resFileModified = true;
                }
                this.addedFiles.put(entry.getKey(), entry.getValue());
            } else if (dealWithSmaliFile(entryName, modifiedDex) == null) {
                this.addedFiles.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry2 : replaced.entrySet()) {
            String entryName2 = entry2.getKey();
            if (entryName2.startsWith("res/")) {
                if (!this.resFileModified && !isCommonImage(entryName2)) {
                    this.resFileModified = z;
                }
                this.replacedFiles.put(entry2.getKey(), entry2.getValue());
            } else if (dealWithSmaliFile(entryName2, modifiedDex) == null) {
                this.replacedFiles.put(entry2.getKey(), entry2.getValue());
            }
            z = true;
        }
        for (String entryName3 : deleted) {
            if (entryName3.startsWith("res/")) {
                if (!this.resFileModified && !isCommonImage(entryName3)) {
                    this.resFileModified = true;
                }
                this.deletedFiles.add(entryName3);
            } else if (dealWithSmaliFile(entryName3, modifiedDex) == null) {
                this.deletedFiles.add(entryName3);
            }
        }
        this.smaliFolders = new ArrayList<>();
        for (String folder : modifiedDex) {
            this.smaliFolders.add(folder);
        }
        if ((this.stringModified || this.manifestModified || this.resFileModified) && (errMsg = getKnownResourceError()) != null) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle(R.string.warning);
            dlg.setMessage(errMsg + "\nAre you sure to continue?");
            dlg.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoActivity.this.m49lambda$build$31$comgmailheagooapkeditorApkInfoActivity(bSign, dialogInterface, i);
                }
            });
            dlg.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            dlg.show();
            return;
        }
        launchBuildActivityAndService(bSign);
    }

    void m49lambda$build$31$comgmailheagooapkeditorApkInfoActivity(boolean bSign, DialogInterface dialog, int which) {
        launchBuildActivityAndService(bSign);
    }

    private String getKnownResourceError() {
        String filepath = this.decodeRootPath + "/AndroidManifest.xml";
        BufferedReader br = null;
        try {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)));
                int lineIndex = 1;
                String line = br.readLine();
                while (line != null) {
                    if (line.trim().startsWith("<provider") && line.contains("android:authorities=\"@string")) {
                        String str = "AndroidManifest.xml(Line " + lineIndex + "): The provider authorities reference to a @string value. It may not work.";
                        try {
                            br.close();
                        } catch (IOException e) {
                        }
                        return str;
                    }
                    line = br.readLine();
                    lineIndex++;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (br == null) {
                    return null;
                }
            }
            try {
                br.close();
                return null;
            } catch (IOException e3) {
                return null;
            }
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public void showCannotStartBuildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.build_in_progress_tip).setTitle(R.string.please_note).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        builder.show();
    }

    public void addFolder(String folderName) {
        String dirPath = this.resListAdapter.getData(null);
        this.resListAdapter.addFolder(dirPath, folderName);
    }

    public void importFolder(final String folderPath) {
        final String dirPath = this.resListAdapter.getData(null);
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            private String errorMessage = null;

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void process() {
                File srcDir = new File(folderPath);
                String folderName = srcDir.getName();
                File curDir = new File(dirPath);
                File targetDir = new File(curDir, folderName);
                if (targetDir.exists()) {
                    String fmt = ApkInfoActivity.this.getString(R.string.file_already_exist);
                    this.errorMessage = String.format(fmt, folderName);
                } else {
                    targetDir.mkdir();
                    IOUtils.copy(targetDir, srcDir);
                }
            }

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void afterProcess() {
                if (this.errorMessage != null) {
                    Toast.makeText((Context) ApkInfoActivity.this, (CharSequence) this.errorMessage, 1).show();
                } else {
                    ApkInfoActivity.this.resListAdapter.refresh();
                }
            }
        }, -1).show();
    }

    private void launchBuildActivityAndService(boolean bSign) {
        if (ServiceUtil.isMyServiceRunning(this, ApkComposeService.class)) {
            Intent intent = new Intent((Context) this, (Class<?>) ApkComposeService.class);
            bindService(intent, new MyServiceConnection(bSign), 1);
        } else {
            launchWithoutCheck(bSign);
        }
    }

    public void launchWithoutCheck(boolean bSign) {
        String filename;
        String outputDir = SDCard.getEditorDirectory();
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdir();
        }
        int outputApkRule = SettingActivity.getOutputApkRule(this);
        switch (outputApkRule) {
            case 0:
                if (bSign) {
                    filename = this.apkInfo.packageName + "_signed";
                    break;
                } else {
                    filename = this.apkInfo.packageName + "_unsigned";
                    break;
                }
            case 1:
            default:
                if (!bSign) {
                    filename = "gen_unsigned";
                    break;
                } else {
                    filename = "gen_signed";
                    break;
                }
            case 2:
                if (bSign) {
                    filename = this.apkInfo.label + "_signed";
                    break;
                } else {
                    filename = this.apkInfo.label + "_unsigned";
                    break;
                }
        }
        String targetApkPath = createOutputPath(this.apkPath, outputDir, FileUtil.reviseFileName(filename));
        Intent intent = new Intent((Context) this, (Class<?>) ApkComposeService.class);
        ActivityUtil.attachParam(intent, "decodeRootPath", this.decodeRootPath);
        if (!this.isFullDecoding) {
            ActivityUtil.attachParam(intent, "srcApkPath", this.apkPath);
        }
        ActivityUtil.attachParam(intent, "targetApkPath", targetApkPath);
        ActivityUtil.attachParam(intent, "stringModified", this.stringModified ? "true" : "false");
        ActivityUtil.attachParam(intent, "manifestModified", this.manifestModified ? "true" : "false");
        ActivityUtil.attachParam(intent, "resFileModified", this.resFileModified ? "true" : "false");
        ActivityUtil.attachParam(intent, "modifiedSmaliFolders", this.smaliFolders);
        ActivityUtil.attachParam(intent, "addedFiles", this.addedFiles);
        ActivityUtil.attachParam(intent, "deletedFiles", this.deletedFiles);
        ActivityUtil.attachParam(intent, "replacedFiles", this.replacedFiles);
        ActivityUtil.attachBoolParam(intent, "signAPK", bSign);
        String mapFileName = serialize2File(this.fileEntry2ZipEntry);
        ActivityUtil.attachParam(intent, "fileEntry2ZipEntry", mapFileName);
        try {
            if (startService(intent) == null) {
                throw new IllegalStateException("Service not started");
            }
            Intent it = new Intent((Context) this, (Class<?>) ApkComposeActivity.class);
            startActivityForResult(it, 1);
        } catch (RuntimeException error) {
            new AlertDialog.Builder(this).setMessage(getString(R.string.lab_compose_start_failed, new Object[]{error.toString()})).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        }
    }

    private String serialize2File(Map<String, String> fileEntry2ZipEntry2) {
        try {
            String filepath = SDCard.makeWorkingDir(this) + RandomUtil.getRandomString(8);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filepath));
            for (Map.Entry<String, String> entry : fileEntry2ZipEntry2.entrySet()) {
                bos.write(entry.getKey().getBytes());
                bos.write(10);
                bos.write(entry.getValue().getBytes());
                bos.write(10);
            }
            bos.close();
            return filepath;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateCenterView() {
        findViewById(R.id.project_save_button).setVisibility(this.curSelectedRadio == 2 ? View.GONE : View.VISIBLE);
        this.manifestRadio.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.manifestIconGrey, (Drawable) null, (Drawable) null);
        this.resRadio.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.resIconGrey, (Drawable) null, (Drawable) null);
        this.stringRadio.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.textIconGrey, (Drawable) null, (Drawable) null);
        this.loadingLayout.setVisibility(4);
        this.manifestLayout.setVisibility(4);
        this.stringLayout.setVisibility(4);
        this.resourceLayout.setVisibility(4);
        switch (this.curSelectedRadio) {
            case 0:
                this.stringRadio.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.textIcon, (Drawable) null, (Drawable) null);
                if (this.stringParsed) {
                    this.stringLayout.setVisibility(0);
                    break;
                } else {
                    this.loadingLayout.setVisibility(0);
                    break;
                }
            case 1:
                this.resRadio.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.resIcon, (Drawable) null, (Drawable) null);
                if (this.resourceParsed) {
                    this.resourceLayout.setVisibility(0);
                    break;
                } else {
                    this.loadingLayout.setVisibility(0);
                    break;
                }
            case 2:
                this.manifestRadio.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.manifestIcon, (Drawable) null, (Drawable) null);
                if (this.resourceParsed) {
                    this.manifestLayout.setVisibility(0);
                    break;
                } else {
                    this.loadingLayout.setVisibility(0);
                    break;
                }
        }
    }

    protected void manifestRadioClicked() {
        if (this.curSelectedRadio != 2) {
            this.curSelectedRadio = 2;
            updateCenterView();
        }
    }

    protected void resRadioClicked() {
        if (this.curSelectedRadio == 2 && !saveInlineManifest(true)) { this.manifestRadio.setChecked(true); return; }
        SharedPreferences sp;
        int showTimes;
        if (this.curSelectedRadio == 1) {
            return;
        }
        this.curSelectedRadio = 1;
        updateCenterView();
        if (!this.dexDecoded && (showTimes = (sp = PreferenceManager.getDefaultSharedPreferences(this)).getInt("HideSmaliMsgShown", 0)) < 1 && SettingActivity.isDex2smaliEnabled(this)) {
            Toast.makeText((Context) this, R.string.hide_smali_tip, 1).show();
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("HideSmaliMsgShown", showTimes + 1);
            editor.apply();
        }
    }

    protected void stringRadioClicked() {
        if (this.curSelectedRadio == 2 && !saveInlineManifest(true)) { this.manifestRadio.setChecked(true); return; }
        if (this.curSelectedRadio != 0) {
            this.curSelectedRadio = 0;
            updateCenterView();
        }
    }

    @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
    public void resTableDecoded(boolean ret) {
        if (ret) {
            try {
                this.bStringPrepared = prepareStringList();
            } catch (Throwable t) {
                t.printStackTrace();
                this.bStringPrepared = false;
            }
            if (this.bStringPrepared) {
                runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        ApkInfoActivity.this.m70x99392872();
                    }
                });
            }
        }
    }

    void m70x99392872() {
        if (this.screenDestroyed || this.lastLoadError != null) {
            return;
        }
        showStringList();
        setupClickListener();
        Toast.makeText((Context) this, R.string.lab_strings_preview, 1).show();
    }

    @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
    public void resourceDecoded(final Map<String, String> fileEntry2ZipEntry) {
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        final boolean stringShowNeeded = !this.bStringPrepared;
        if (!this.bStringPrepared) {
            if (this.parseThread != null) {
                this.parseThread.indexStringsStarted();
            }
            try {
                this.bStringPrepared = prepareStringList();
            } catch (Throwable t) {
                // A single broken string entry must not kill the whole decode.
                t.printStackTrace();
                this.bStringPrepared = false;
            }
        }
        runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                ApkInfoActivity.this.m72x8b124e30(stringShowNeeded, fileEntry2ZipEntry);
            }
        });
    }

    void m72x8b124e30(boolean stringShowNeeded, final Map fileEntry2ZipEntry) {
        if (this.screenDestroyed || this.lastLoadError != null) {
            return;
        }
        if (stringShowNeeded) {
            showStringList();
            setupClickListener();
        }
        final HashMap<String, ArrayList<StringItem>> snapshot = new HashMap<>();
        for (Map.Entry<String, ArrayList<StringItem>> entry : this.allStringValues.entrySet()) {
            ArrayList<StringItem> values = new ArrayList<>(entry.getValue().size());
            Iterator<StringItem> it = entry.getValue().iterator();
            while (it.hasNext()) {
                StringItem item = it.next();
                values.add(new StringItem(item.name, item.value, item.styledValue));
            }
            snapshot.put(entry.getKey(), values);
        }
        this.indexSaveTask = new FutureTask<>(new Callable() {
            @Override // java.util.concurrent.Callable
            public final Object call() throws Exception {
                return ApkInfoActivity.this.m71xc825e4d1(snapshot, fileEntry2ZipEntry);
            }
        });
        new Thread(this.indexSaveTask, "resource-index-save").start();
        showDecodedFileList();
        this.stringListAdapter.setEditingEnabled(true);
        this.webserverMenu.setVisibility(0);
        this.rotateMenu.setVisibility(0);
        if (isPro()) {
            this.patchMenu.setVisibility(0);
        } else {
            this.patchMenu.setVisibility(8);
        }
        this.saveBtn.setVisibility(0);
    }

    Boolean m71xc825e4d1(HashMap snapshot, Map fileEntry2ZipEntry) throws Exception {
        try {
            String workingPath = SDCard.makeWorkingDir(this);
            boolean stringsOk = IOUtils.writeObjectToFile(workingPath + "allStringValues", snapshot);
            boolean mappingOk = IOUtils.writeObjectToFile(workingPath + "fileEntry2ZipEntry", fileEntry2ZipEntry);
            return Boolean.valueOf(stringsOk && mappingOk);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void decodeDex(IGeneralCallback dexDecodedCallback) {
        this.dexDecodedCallback = dexDecodedCallback;
        this.dexDecoded = false;
        new AsyncDecodeTask(this, this.apkPath, this.decodeRootPath, this).execute(new Void[0]);
    }

    @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
    public void decodeProgress(final int phase) {
        runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                ApkInfoActivity.this.m55x48b50c1c(phase);
            }
        });
    }

    void m55x48b50c1c(int phase) {
        if (this.screenDestroyed || this.lastLoadError != null) {
            return;
        }
        this.lastLoadPhase = phase;
        ((TextView) findViewById(R.id.tv_tip)).setText(loadPhaseLabel(phase));
    }

    private static final int[] LOAD_PHASE_LABELS = {R.string.lab_load_wait, R.string.lab_load_table, R.string.lab_load_resources, R.string.lab_load_values, R.string.lab_load_files, R.string.lab_load_framework, R.string.lab_load_index_resources, R.string.lab_load_resource_files, R.string.lab_load_cleanup, R.string.lab_load_prepare_tools, R.string.lab_load_string_index};

    private int loadPhaseLabel(int phase) {
        return LOAD_PHASE_LABELS[Math.max(0, Math.min(phase, LOAD_PHASE_LABELS.length - 1))];
    }

    @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
    public void decodeProgressCount(final int done, final int total) {
        runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                ApkInfoActivity.this.m56xf7cdc1ba(done, total);
            }
        });
    }

    void m56xf7cdc1ba(int done, int total) {
        if (this.screenDestroyed || this.lastLoadError != null) {
            return;
        }
        // Keep the current stage caption and append the counter, so the loading
        // screen visibly moves instead of looking frozen.
        String caption = this.lastLoadPhase >= 0
                ? getString(loadPhaseLabel(this.lastLoadPhase))
                : getString(R.string.lab_load_resource_files);
        ((TextView) findViewById(R.id.tv_tip)).setText(caption + "\n" + done + " / " + total);
    }

    @Override // com.gmail.heagoo.apkeditor.ApkParseConsumer
    public void decodeFailed(final String errMessage) {
        runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                ApkInfoActivity.this.m54x9748ebaa(errMessage);
            }
        });
    }

    void m54x9748ebaa(String errMessage) {
        if (this.screenDestroyed) {
            return;
        }
        this.lastLoadError = (errMessage == null || errMessage.length() == 0) ? "Unknown error" : errMessage;
        this.stringParsed = false;
        findViewById(R.id.progress_bar).setVisibility(8);
        ((TextView) findViewById(R.id.tv_tip)).setText(getString(R.string.lab_load_error) + "\n" + this.lastLoadError);
        this.loadRetryButton.setVisibility(0);
        this.loadCopyButton.setText(R.string.lab_load_copy);
        this.loadCopyButton.setVisibility(0);
        updateCenterView();
    }

    private boolean prepareStringList() {
        String value;
        List<ResResSpec> specList;
        this.allStringValues = new HashMap<>();
        ResPackage pkg = this.parseThread.getApkPackage();
        if (pkg == null) {
            return false;
        }
        List<ResResSpec> specList2 = pkg.listResSpecs();
        for (ResResSpec spec : specList2) {
            if ("string".equals(spec.getType().getName())) {
                String name = spec.getName();
                Map<ResConfigFlags, ResResource> resources = spec.getAllResources();
                for (Map.Entry<ResConfigFlags, ResResource> entry : resources.entrySet()) {
                    String styledValue = null;
                    ResConfigFlags configFlag = entry.getKey();
                    ResResource resResource = entry.getValue();
                    // A "string" resource is not always a scalar value (file values,
                    // bags, ...). The unconditional cast used to throw
                    // ClassCastException and killed the whole decode thread.
                    Object rawValue = resResource.getValue();
                    if (!(rawValue instanceof ResScalarValue)) {
                        value = String.valueOf(rawValue);
                    } else {
                        ResScalarValue value2 = (ResScalarValue) rawValue;
                        if (value2 instanceof ResReferenceValue) {
                            value = value2.toString();
                        } else if (value2.isStyled()) {
                            styledValue = value2.getHtmlValue();
                            String styled = value2.getRawValue();
                            value = styled != null ? styled : value2.toString();
                        } else {
                            String value3 = value2.getRawValue();
                            if (value3 != null) {
                                value = value3;
                            } else {
                                value = value2.toString();
                            }
                        }
                    }
                    StringItem item = new StringItem(name, value, styledValue);
                    String qualifier = configFlag.getQualifiers();
                    ResPackage pkg2 = pkg;
                    ArrayList<StringItem> stringValueList = this.allStringValues.get(qualifier);
                    if (stringValueList != null) {
                        specList = specList2;
                    } else {
                        stringValueList = new ArrayList<>();
                        specList = specList2;
                        this.allStringValues.put(qualifier, stringValueList);
                    }
                    stringValueList.add(item);
                    pkg = pkg2;
                    specList2 = specList;
                }
            }
            pkg = pkg;
            specList2 = specList2;
        }
        return true;
    }

    private void showStringList() {
        if (this.stringListAdapter != null && this.changedStringValues != null) {
            this.stringListAdapter.setChangedValues(this.changedStringValues);
        }
        PendingStringEdits.merge(this.allStringValues, this.stringListAdapter.getChangedValues());
        initAddLanguageBtn();
        initTranslateBtn();
        initSpinner();
        Button searchBtn = (Button) findViewById(R.id.search_button);
        searchBtn.setOnClickListener(this);
        this.stringParsed = true;
        updateCenterView();
    }

    private void initAddLanguageBtn() {
        ImageView iv = (ImageView) findViewById(R.id.add_language);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ApkInfoActivity.this.m57x9a29bf31(view);
            }
        });
    }

    void m57x9a29bf31(View v) {
        LanguageSelectDialog dlg = new LanguageSelectDialog(this, null, null);
        dlg.show();
    }

    private boolean useLocalTranslation;
    private String localTranslationSource;
    private java.util.concurrent.atomic.AtomicBoolean localTranslationCancellation;
    private final java.util.concurrent.atomic.AtomicReference<com.gmail.heagoo.apkeditor.translate.LocalInference> localEngine = new java.util.concurrent.atomic.AtomicReference<>();

    private void initTranslateBtn() {
        ImageView iv = (ImageView) findViewById(R.id.translate);
        iv.setOnClickListener(this);
    }

    void m58xb7ddd23b(View v) {
        new AboutPluginDialog(this).show();
    }

    private boolean proTranslatePluginExist() {
        try {
            getPackageManager().getApplicationInfo("apkeditor.translate", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public ResConfigFlags createConfigFlags(String qualifier) {
        int pos = qualifier.indexOf("-r", 1);
        if (pos != -1 && pos + 3 < qualifier.length()) {
            return new ResConfigFlags(qualifier.charAt(1), qualifier.charAt(2), qualifier.charAt(pos + 2), qualifier.charAt(pos + 3));
        }
        return new ResConfigFlags(qualifier.charAt(1), qualifier.charAt(2));
    }

    public void startNewTranslation() {
        if (generalTranslatePluginExist(this) || proTranslatePluginExist()) {
            new AlertDialog.Builder(this).setTitle(R.string.ai_translate_title)
                    .setItems(new String[]{getString(R.string.ai_local), getString(R.string.translate_plugin)}, (d, which) -> {
                        useLocalTranslation = which == 0;
                        beginTranslation();
                    }).setNegativeButton(android.R.string.cancel, null).show();
        } else {
            useLocalTranslation = true;
            beginTranslation();
        }
    }

    private void beginTranslation() {
        if (useLocalTranslation && com.gmail.heagoo.apkeditor.translate.LocalModelManager.selected(this) == null) {
            new AlertDialog.Builder(this).setMessage(R.string.ai_choose_first)
                    .setPositiveButton(R.string.ai_models, (d, w) -> startActivity(new Intent(this, SettingActivity.class)))
                    .setNegativeButton(android.R.string.cancel, null).show();
            return;
        }
        if (!useLocalTranslation) {
            LanguageSelectDialog dlg = new LanguageSelectDialog(this, PossibleLanguages.languages, PossibleLanguages.codes);
            dlg.setTitle(R.string.select_target_lang);
            dlg.show();
            return;
        }
        if (allStringValues == null || allStringValues.isEmpty()) {
            Toast.makeText(this, R.string.ai_no_source, Toast.LENGTH_LONG).show();
            return;
        }
        final List<String> configs = new ArrayList<>();
        for (Map.Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) configs.add(entry.getKey());
        }
        if (configs.isEmpty()) {
            Toast.makeText(this, R.string.ai_no_source, Toast.LENGTH_LONG).show();
            return;
        }
        configs.sort(Comparator.comparing(ResourceLocales::sortKey));
        String[] names = new String[configs.size()];
        int checked = -1;
        for (int i = 0; i < configs.size(); i++) {
            String config = configs.get(i);
            names[i] = localSourceName(config) + "\n" + ResourceLocales.folder(config);
            if (config.equals(curConfig)) checked = i;
        }
        new AlertDialog.Builder(this).setTitle(R.string.ai_source_picker_title)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    localTranslationSource = configs.get(which);
                    dialog.dismiss();
                    LanguageSelectDialog target = new LanguageSelectDialog(this, PossibleLanguages.languages, PossibleLanguages.codes);
                    target.setTranslationSource(localSourceName(localTranslationSource));
                    target.show();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private String localSourceName(String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) return getString(R.string.ai_default_source);
        String name = brut.util.LanguageMapping.getLanguage(qualifier);
        return name == null || name.isEmpty() ? qualifier : name;
    }

    private void translateLocally(String qualifier) {
        String targetName = qualifier;
        for (int i = 0; i < PossibleLanguages.codes.length; i++) {
            if (PossibleLanguages.codes[i].equals(qualifier)) { targetName = PossibleLanguages.languages[i]; break; }
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.ai_mode_title, localSourceName(localTranslationSource), targetName))
                .setItems(new String[]{getString(R.string.ai_sample_option), getString(R.string.ai_full_option)},
                        (dialog, which) -> runLocalTranslation(qualifier, which == 0))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void runLocalTranslation(String qualifier, boolean sample) {
        // Include edits made in the current editor, and translate the language
        // the user selected rather than silently using the default resources.
        if (stringListAdapter != null) PendingStringEdits.merge(allStringValues, stringListAdapter.getChangedValues());
        List<StringItem> source = com.gmail.heagoo.apkeditor.translate.TranslationSource.selected(allStringValues, localTranslationSource);
        File model = com.gmail.heagoo.apkeditor.translate.LocalModelManager.selected(this);
        if (source == null || source.isEmpty() || model == null) {
            Toast.makeText(this, R.string.ai_no_source, Toast.LENGTH_LONG).show(); return;
        }
        ArrayList<StringItem> snapshot = new ArrayList<>();
        for (StringItem item : source) {
            if (sample && (item.styledValue != null || item.value == null || item.value.isEmpty()
                    || com.gmail.heagoo.apkeditor.translate.TranslationSource.isInvariant(item.value)
                    || item.value.startsWith("@") || item.value.startsWith("?") || item.value.length() > 300
                    || item.value.codePoints().anyMatch(c -> c > 0xffff))) continue;
            snapshot.add(new StringItem(item.name, item.value, item.styledValue));
            if (sample && snapshot.size() == 3) break;
        }
        if (snapshot.isEmpty()) { Toast.makeText(this, R.string.ai_no_source, Toast.LENGTH_LONG).show(); return; }
        String language = qualifier;
        for (int i = 0; i < PossibleLanguages.codes.length; i++) {
            if (PossibleLanguages.codes[i].equals(qualifier)) { language = PossibleLanguages.languages[i]; break; }
        }
        final String targetLanguage = language;
        final String sourceQualifier = localTranslationSource;
        com.gmail.heagoo.apkeditor.translate.TranslationTrace trace = new com.gmail.heagoo.apkeditor.translate.TranslationTrace(
                snapshot.size(), com.gmail.heagoo.apkeditor.translate.LocalInference.threadCount(), model.length(), sample, qualifier);
        com.gmail.heagoo.apkeditor.translate.TranslationDiagnostics diagnostics = new com.gmail.heagoo.apkeditor.translate.TranslationDiagnostics(this, trace);
        diagnostics.save();
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setTitle(R.string.ai_translate_title);
        progress.setMessage(getString(R.string.ai_loading));
        progress.setCancelable(false);
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        localTranslationCancellation = cancelled;
        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<String> currentName = new java.util.concurrent.atomic.AtomicReference<>("");
        long started = android.os.SystemClock.elapsedRealtime();
        android.os.Handler timer = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable update = new Runnable() {
            int ticks;
            @Override public void run() {
                if (isFinishing() || isDestroyed() || !progress.isShowing()) return;
                if (cancelled.get()) progress.setMessage(getString(R.string.ai_translate_cancelling));
                else if (currentName.get().isEmpty()) progress.setMessage(getString(R.string.ai_loading_elapsed,
                        (android.os.SystemClock.elapsedRealtime() - started) / 1000));
                else progress.setMessage(getString(R.string.ai_progress_elapsed, completed.get(), snapshot.size(),
                        currentName.get(), (android.os.SystemClock.elapsedRealtime() - started) / 1000) + "\n" + trace.nativeStatus());
                if (++ticks % 5 == 0) diagnostics.save();
                timer.postDelayed(this, 1000);
            }
        };
        progress.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.ai_copy_log), (d, w) -> {});
        progress.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (d, w) -> {});
        progress.setOnShowListener(d -> {
            progress.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> diagnostics.copy(this));
            progress.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
                cancelled.set(true);
                com.gmail.heagoo.apkeditor.translate.LocalInference active = localEngine.get();
                if (active != null) active.requestCancel();
                trace.cancelRequested();
                diagnostics.save();
                v.setEnabled(false);
                progress.setMessage(getString(R.string.ai_translate_cancelling));
            });
        });
        progress.show();
        timer.post(update);
        new Thread(() -> {
            ArrayList<StringItem> result = new ArrayList<>();
            // Resource key is part of the prompt, so different keys may have different meanings.
            java.util.Map<String, String> translationCache = new java.util.HashMap<>();
            java.util.Map<String, String> translatedValues = new java.util.LinkedHashMap<>();
            ArrayList<String> rejectedNames = new ArrayList<>();
            java.util.Map<String, com.gmail.heagoo.apkeditor.translate.TranslationRejectedException.Reason> rejectedCache = new java.util.HashMap<>();
            String failure = null;
            try (com.gmail.heagoo.apkeditor.translate.LocalInference engine = new com.gmail.heagoo.apkeditor.translate.LocalInference(model, trace, cancelled)) {
                localEngine.set(engine);
                if (cancelled.get()) engine.requestCancel();
                for (StringItem item : snapshot) {
                    if (cancelled.get()) break;
                    currentName.set(item.name);
                    if (item.value == null) throw new java.io.IOException("Empty resource: " + item.name);
                    if (item.styledValue != null || item.value.isEmpty() || item.value.startsWith("@") || item.value.startsWith("?")
                            || com.gmail.heagoo.apkeditor.translate.TranslationSource.isInvariant(item.value)) {
                        trace.stringStart(item.value.length(), 0);
                        result.add(item);
                    } else {
                        String cacheKey = item.name + "\u0000" + item.value;
                        try {
                            com.gmail.heagoo.apkeditor.translate.TranslationRejectedException.Reason prior = rejectedCache.get(cacheKey);
                            if (prior != null) {
                                trace.stringStart(item.value.length(), 0);
                                throw new com.gmail.heagoo.apkeditor.translate.TranslationRejectedException(prior, "Previously rejected");
                            }
                            String translated = translationCache.get(cacheKey);
                            if (translated == null) {
                                translated = engine.translate(item.value, targetLanguage, sourceQualifier, item.name);
                                translationCache.put(cacheKey, translated);
                            } else {
                                trace.stringStart(item.value.length(), 0);
                                trace.translationCacheHit();
                            }
                            translatedValues.put(item.name, translated);
                            result.add(new StringItem(item.name, translated));
                        } catch (com.gmail.heagoo.apkeditor.translate.TranslationRejectedException rejected) {
                            if (cancelled.get()) break;
                            rejectedCache.put(cacheKey, rejected.reason());
                            rejectedNames.add(item.name);
                            trace.stringRejected(rejected.reason());
                            result.add(item);
                        }
                    }
                    completed.incrementAndGet();
                    trace.stringDone();
                }
            } catch (Exception | LinkageError e) {
                trace.failure(e instanceof java.io.IOException, e instanceof LinkageError);
                failure = e.getMessage() == null ? e.toString() : e.getMessage();
            } finally {
                localEngine.set(null);
                if (failure == null) trace.stage(com.gmail.heagoo.apkeditor.translate.TranslationTrace.Stage.FINISHED);
                diagnostics.save();
            }
            String error = failure;
            runOnUiThread(() -> {
                timer.removeCallbacks(update);
                if (localTranslationCancellation == cancelled) localTranslationCancellation = null;
                if (isFinishing() || isDestroyed()) return;
                progress.dismiss();
                if (cancelled.get()) return;
                if (error != null) { new AlertDialog.Builder(this).setTitle(R.string.ai_translate_title).setMessage(error)
                        .setNeutralButton(R.string.ai_copy_log, (d, w) -> diagnostics.copy(this))
                        .setPositiveButton(android.R.string.ok, null).show(); return; }
                TranslationReviewDialog.show(this, snapshot, result, translatedValues, rejectedNames, sample,
                        () -> diagnostics.copy(this), closeReview -> {
                            File targetDir = new File(decodeRootPath, "res/values" + qualifier);
                            if (allStringValues.containsKey(qualifier) || targetDir.exists()) {
                                new AlertDialog.Builder(this).setMessage(R.string.ai_replace_confirm)
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setPositiveButton(R.string.ai_replace, (confirm, ignored) -> {
                                            try { updateExistingTranslation(qualifier, translatedValues); closeReview.run(); Toast.makeText(this, R.string.ai_saved, Toast.LENGTH_LONG).show(); }
                                            catch (Exception e) { new AlertDialog.Builder(this).setMessage(e.toString()).setPositiveButton(android.R.string.ok, null).show(); }
                                        }).show();
                            } else {
                                try { saveTranslatedLanguage(qualifier, result); closeReview.run(); Toast.makeText(this, R.string.ai_saved, Toast.LENGTH_LONG).show(); }
                                catch (Exception e) { Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show(); }
                            }
                        });
            });
        }, "local-ai-translate").start();
    }

    /** Update only translated scalar strings, leaving target-only and skipped resources intact. */
    private void updateExistingTranslation(String qualifier, Map<String, String> updates) throws Exception {
        if (!qualifier.matches("-[A-Za-z0-9+-]+")) throw new IOException("Invalid target qualifier");
        Map<String, String> pending = stringListAdapter.getChangedValues().get(qualifier);
        if (pending != null && !pending.isEmpty()) throw new IOException(getString(R.string.ai_pending_target));
        if (updates.isEmpty()) return;
        File directory = new File(decodeRootPath, "res/values" + qualifier);
        if (!directory.isDirectory() || Files.isSymbolicLink(directory.toPath())) throw new IOException("Target resource folder is unavailable");
        String prefix = "res/values" + qualifier + "/";
        for (String entry : resListAdapter.getAddedFiles().keySet())
            if (entry.startsWith(prefix) && entry.endsWith(".xml")) throw new IOException(getString(R.string.ai_pending_target));
        for (String entry : resListAdapter.getDeletedFiles())
            if (entry.startsWith(prefix) && entry.endsWith(".xml")) throw new IOException(getString(R.string.ai_pending_target));
        File[] xmlFiles = directory.listFiles((folder, name) -> name.endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) throw new IOException("Target XML resources are unavailable");
        java.util.Arrays.sort(xmlFiles, java.util.Comparator.comparing(File::getName));
        Map<String, String> originals = new java.util.LinkedHashMap<>();
        for (File file : xmlFiles) {
            if (!file.isFile() || Files.isSymbolicLink(file.toPath()) || file.length() > 4194304) throw new IOException("Unsupported resource file: " + file.getName());
            String staged = resListAdapter.getReplacedFilePath(prefix + file.getName());
            if (staged != null && !new File(staged).getCanonicalFile().equals(file.getCanonicalFile())) throw new IOException(getString(R.string.ai_pending_target));
            originals.put(file.getName(), StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(file.toPath()))).toString());
        }
        java.util.Set<String> expected = new java.util.HashSet<>();
        List<StringItem> target = allStringValues.get(qualifier);
        if (target != null) for (StringItem item : target) if (updates.containsKey(item.name)) expected.add(item.name);
        Map<String, String> changes = com.gmail.heagoo.apkeditor.translate.ExistingTranslationUpdate.plan(originals, updates, expected);
        Map<File, File> temporary = new java.util.LinkedHashMap<>();
        List<File> replaced = new ArrayList<>();
        try {
            for (Map.Entry<String, String> change : changes.entrySet()) {
                File file = new File(directory, change.getKey());
                if (!originals.containsKey(change.getKey())) throw new IOException("Unexpected resource file");
                File tmp = File.createTempFile(".ai-translation-", ".tmp", directory);
                temporary.put(file, tmp);
                Files.write(tmp.toPath(), change.getValue().getBytes(StandardCharsets.UTF_8));
            }
            for (Map.Entry<File, File> change : temporary.entrySet()) {
                String original = originals.get(change.getKey().getName());
                if (!java.util.Arrays.equals(Files.readAllBytes(change.getKey().toPath()), original.getBytes(StandardCharsets.UTF_8))) throw new IOException("Resource changed during translation save");
                Files.move(change.getValue().toPath(), change.getKey().toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                replaced.add(change.getKey());
            }
        } catch (Exception failure) {
            for (File file : replaced) {
                try { Files.write(file.toPath(), originals.get(file.getName()).getBytes(StandardCharsets.UTF_8)); }
                catch (Exception rollback) { failure.addSuppressed(rollback); }
            }
            throw failure;
        } finally { for (File tmp : temporary.values()) tmp.delete(); }
        for (File file : replaced) resListAdapter.fileModified(prefix + file.getName(), file.getAbsolutePath());
        if (target == null) { target = new ArrayList<>(); allStringValues.put(qualifier, (ArrayList<StringItem>) target); }
        java.util.Set<String> handled = new java.util.HashSet<>();
        for (StringItem item : target) if (updates.containsKey(item.name)) {
            item.value = updates.get(item.name); item.styledValue = null; handled.add(item.name);
        }
        for (Map.Entry<String, String> update : updates.entrySet()) if (!handled.contains(update.getKey())) target.add(new StringItem(update.getKey(), update.getValue()));
        stringModified = true;
        resetSpinner();
    }

    public void saveTranslatedLanguage(String qualifier, List<StringItem> stringValues) throws Exception {
        saveStringResource(qualifier, stringValues);
        this.stringModified = true;
        String targetConfig = null;
        for (Map.Entry<String, ArrayList<StringItem>> entry : this.allStringValues.entrySet()) {
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                targetConfig = q;
                ArrayList<StringItem> stringList = entry.getValue();
                stringList.clear();
                stringList.addAll(stringValues);
            }
        }
        if (targetConfig == null) {
            ArrayList<StringItem> stringList2 = new ArrayList<>();
            stringList2.addAll(stringValues);
            this.allStringValues.put(qualifier, stringList2);
        }
        resetSpinner();
    }

    public void translateLanguage(String strQualifier) {
        if (useLocalTranslation) { translateLocally(strQualifier); return; }
        Intent intent;
        List<TranslateItem> translatedList = new ArrayList<>();
        List<TranslateItem> untranslatedList = new ArrayList<>();
        prepareTranslateItems(strQualifier, translatedList, untranslatedList);
        try {
            if (generalTranslatePluginExist(this)) {
                intent = new Intent("android.intent.action.VIEW");
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setDataAndType(null, "application/com.gmail.heagoo.apkeditor-translate");
            } else {
                ComponentName componetName = new ComponentName("apkeditor.translate", "apkeditor.translate.TranslateActivity");
                Intent intent2 = new Intent();
                intent2.setComponent(componetName);
                intent = intent2;
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean("isFullScreen", GlobalConfig.instance(this).isFullScreen());
            bundle.putBoolean("isDark", GlobalConfig.instance(this).isDarkTheme());
            String translatedFile = SDCard.makeWorkingDir(this) + "translated";
            IOUtils.writeObjectToFile(translatedFile, translatedList);
            bundle.putString("translatedList_file", translatedFile);
            String untranslatedFile = SDCard.makeWorkingDir(this) + "untranslatedList";
            IOUtils.writeObjectToFile(untranslatedFile, untranslatedList);
            bundle.putString("untranslatedList_file", untranslatedFile);
            bundle.putString("targetLanguageCode", strQualifier);
            intent.putExtras(bundle);
            startActivityForResult(intent, RC_TRANSLATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareTranslateItems(String qualifier, List<TranslateItem> translatedList, List<TranslateItem> untranslatedList) {
        ArrayList<StringItem> defaultStrings = null;
        ArrayList<StringItem> translatedStrings = null;
        for (Map.Entry<String, ArrayList<StringItem>> entry : this.allStringValues.entrySet()) {
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                ArrayList<StringItem> translatedStrings2 = entry.getValue();
                translatedStrings = translatedStrings2;
            } else if ("".equals(q)) {
                ArrayList<StringItem> defaultStrings2 = entry.getValue();
                defaultStrings = defaultStrings2;
            }
        }
        if (defaultStrings == null) {
            if (translatedStrings != null) {
                Iterator<StringItem> it = translatedStrings.iterator();
                while (it.hasNext()) {
                    StringItem kv = it.next();
                    TranslateItem item = new TranslateItem(kv.name, "", kv.value);
                    translatedList.add(item);
                }
                return;
            }
            return;
        }
        Map<String, String> translated = new HashMap<>();
        Map<String, String> defaultStrs = new HashMap<>();
        if (translatedStrings != null) {
            Iterator<StringItem> it2 = translatedStrings.iterator();
            while (it2.hasNext()) {
                StringItem p = it2.next();
                translated.put(p.name, p.value);
            }
        }
        Iterator<StringItem> it3 = defaultStrings.iterator();
        while (it3.hasNext()) {
            StringItem p2 = it3.next();
            defaultStrs.put(p2.name, p2.value);
        }
        if (translatedStrings != null) {
            Iterator<StringItem> it4 = translatedStrings.iterator();
            while (it4.hasNext()) {
                StringItem p3 = it4.next();
                String name = p3.name;
                String originValue = defaultStrs.get(name);
                String translatedVal = p3.value;
                TranslateItem item2 = new TranslateItem(name, originValue, translatedVal);
                translatedList.add(item2);
            }
        }
        Iterator<StringItem> it5 = defaultStrings.iterator();
        while (it5.hasNext()) {
            StringItem p4 = it5.next();
            String name2 = p4.name;
            String translatedVal2 = translated.get(name2);
            if (translatedVal2 == null) {
                String originValue2 = p4.value;
                TranslateItem item3 = new TranslateItem(name2, originValue2, (String) null);
                untranslatedList.add(item3);
            }
        }
    }

    public String addLanguageRetError(String qualifier) {
        Resources res = getResources();
        if (qualifier.length() < 3) {
            return res.getString(R.string.invalid_lang_code);
        }
        if (!this.resourceParsed) {
            return res.getString(R.string.wait_for_decoding);
        }
        List<TranslateItem> translatedItems = new ArrayList<>();
        List<TranslateItem> untranslatedItems = new ArrayList<>();
        prepareTranslateItems(qualifier, translatedItems, untranslatedItems);
        if (untranslatedItems.isEmpty()) {
            return res.getString(R.string.lang_exist);
        }
        boolean bExisting = false;
        String addedConfigFlag = null;
        ArrayList<StringItem> untranslatedList = new ArrayList<>();
        for (TranslateItem item : untranslatedItems) {
            untranslatedList.add(new StringItem(item.name, item.originValue));
        }
        Iterator<Map.Entry<String, ArrayList<StringItem>>> it = this.allStringValues.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, ArrayList<StringItem>> entry = it.next();
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                addedConfigFlag = q;
                bExisting = true;
                entry.getValue().addAll(untranslatedList);
                break;
            }
        }
        if (addedConfigFlag == null) {
            addedConfigFlag = qualifier;
            this.allStringValues.put(addedConfigFlag, untranslatedList);
        }
        try {
            if (!bExisting) {
                saveStringResource(qualifier, untranslatedList);
            } else {
                List<StringItem> valueList = new ArrayList<>();
                for (TranslateItem item2 : translatedItems) {
                    valueList.add(new StringItem(item2.name, item2.translatedValue));
                }
                valueList.addAll(untranslatedList);
                saveStringResource(qualifier, valueList);
            }
            this.stringModified = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.curConfig = addedConfigFlag;
        resetSpinner();
        return null;
    }

    private void saveStringResource(String qualifier, List<StringItem> valueList) throws Exception {
        String escaped;
        XmlSerializer xmlSerializer = new KXmlSerializer();
        String dirPath = this.decodeRootPath + "/res/values" + qualifier;
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        File file = new File(dirPath + "/strings.xml");
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos);
        xmlSerializer.setOutput(writer);
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        writer.write("<resources>\n");
        for (StringItem v : valueList) {
            xmlSerializer.startTag(null, "string");
            xmlSerializer.attribute(null, "name", v.name);
            if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(v.value)) {
                xmlSerializer.attribute(null, "formatted", "false");
            }
            if (v.value.startsWith("@string/") || v.value.startsWith("@android:string/")) {
                escaped = v.value;
            } else if (v.styledValue == null) {
                String escaped2 = ResXmlEncoders.escapeXmlChars(v.value);
                escaped = ResXmlEncoders.encodeAsXmlValue(escaped2);
            } else {
                String txt = v.styledValue;
                escaped = ResXmlEncoders.encodeAsXmlValue(txt);
            }
            xmlSerializer.ignorableWhitespace(escaped);
            xmlSerializer.endTag(null, "string");
            xmlSerializer.flush();
            writer.write("\n");
        }
        writer.write("</resources>\n");
        writer.close();
        fos.close();
    }

    private void modifyStringResource(String qualifier, Map<String, String> modifiedValues) throws Exception {
        String fileName;
        String dirPath;
        String fileName2 = "strings.xml";
        String dirPath2 = this.decodeRootPath + "/res/values" + qualifier;
        File dirFile = new File(dirPath2);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        String filePath = dirPath2 + "/strings.xml";
        List<String> lines = new TextFileReader(filePath).getLines();
        List<String> newLines = new ArrayList<>();
        int lastStringLine = -1;
        Set<String> savedStrings = new HashSet<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            int pos = line.indexOf("<string name=\"");
            if (pos == -1) {
                newLines.add(line);
                fileName = fileName2;
            } else {
                int start = "<string name=\"".length() + pos;
                int end = line.indexOf(34, start);
                fileName = fileName2;
                if (end == -1) {
                    newLines.add(line);
                } else {
                    String name = line.substring(start, end);
                    dirPath = dirPath2;
                    String newValue = modifiedValues.get(name);
                    if (newValue != null) {
                        newLines.add(StringItem.toString(name, newValue, (String) null));
                        savedStrings.add(name);
                        if (!line.contains("</string>")) {
                            while (true) {
                                i++;
                                String name2 = name;
                                if (i >= lines.size() || lines.get(i).contains("</string>")) {
                                    break;
                                } else {
                                    name = name2;
                                }
                            }
                        }
                    } else {
                        newLines.add(line);
                    }
                    lastStringLine = newLines.size() - 1;
                    i++;
                    dirPath2 = dirPath;
                    fileName2 = fileName;
                }
            }
            dirPath = dirPath2;
            i++;
            dirPath2 = dirPath;
            fileName2 = fileName;
        }
        for (Map.Entry<String, String> entry : modifiedValues.entrySet()) {
            String name3 = entry.getKey();
            if (!savedStrings.contains(name3)) {
                lastStringLine++;
                lines.add(lastStringLine, StringItem.toString(name3, entry.getValue(), (String) null));
            }
        }
        FileUtil.writeToFile(filePath, newLines);
    }

    protected void searchStringByKeyword(String keyword) {
        String lcKeyword = keyword.toLowerCase();
        ArrayList<StringItem> values = this.allStringValues.get(this.curConfig);
        if (values != null) {
            ArrayList<StringItem> selected = new ArrayList<>();
            Iterator<StringItem> it = values.iterator();
            while (it.hasNext()) {
                StringItem pair = it.next();
                if (pair.value.toLowerCase().contains(lcKeyword)) {
                    selected.add(pair);
                }
            }
            updateStringList(selected);
        }
    }

    public void updateStringList() {
        if (this.curConfig == null) {
            this.curConfig = getBestConfig(this.allStringValues.keySet());
        }
        LOGGER.info("********BEST*********" + this.curConfig);
        ArrayList<StringItem> values = this.allStringValues.get(this.curConfig);
        updateStringList(values);
    }

    private void updateStringList(ArrayList<StringItem> values) {
        this.stringListAdapter.updateData(this.curConfig, values);
    }

    private void initSpinner() {
        if (this.allStringValues == null) {
            return;
        }
        if (this.curConfig == null || !this.allStringValues.containsKey(this.curConfig)) {
            this.curConfig = getBestConfig(this.allStringValues.keySet());
        }
        this.langConfigList = new ArrayList<>();
        String[] languages = new String[this.allStringValues.size()];
        List<String> langList = new ArrayList<>();
        Map<String, String> _m = new HashMap<>();
        for (String qualifier : this.allStringValues.keySet()) {
            String strLang = LanguageMapping.getLanguage(qualifier);
            langList.add(strLang);
            _m.put(strLang, qualifier);
        }
        Collections.sort(langList);
        int selected = 0;
        int index = 0;
        for (String strLang2 : langList) {
            String configFlag = _m.get(strLang2);
            languages[index] = strLang2;
            this.langConfigList.add(configFlag);
            if (configFlag.equals(this.curConfig)) {
                selected = index;
            }
            index++;
        }
        Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        if (spinner == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>((Context) this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                ApkInfoActivity.this.curConfig = (String) ApkInfoActivity.this.langConfigList.get(position);
                ApkInfoActivity.this.updateStringList();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        spinner.setSelection(selected);
    }

    private void resetSpinner() {
        initSpinner();
    }

    private void showDecodedFileList() {
        String curDir;
        if (this.resCurrentDir != null && this.resCurrentDir.startsWith(this.decodeRootPath)) {
            curDir = this.resCurrentDir;
        } else {
            String curDir2 = this.decodeRootPath;
            curDir = curDir2;
        }
        String path = this.isFullDecoding ? null : this.apkPath;
        this.resListAdapter = new ResListAdapter(this, path, curDir, this.decodeRootPath, this.fileEntry2ZipEntry, new FilenameFilter() {
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                return ApkInfoActivity.lambda$showDecodedFileList$40(file, str);
            }
        }, this);
        this.resListAdapter.setModification(this.res_addedFiles, this.res_deletedFiles, this.res_replacedFiles);
        this.resourceList.setAdapter((ListAdapter) this.resListAdapter);
        this.resourceList.setOnItemClickListener(this);
        this.resourceList.setOnItemLongClickListener(this);
        ManifestEditorPalette headerColors = ManifestEditorPalette.forDark(GlobalConfig.instance(this).isDarkTheme());
        View manifestHeader = findViewById(R.id.manifest_header);
        if (manifestHeader != null) {
            android.view.ViewGroup.LayoutParams headerParams = manifestHeader.getLayoutParams();
            headerParams.height = (int) (36 * getResources().getDisplayMetrics().density + 0.5f);
            manifestHeader.setLayoutParams(headerParams);
            manifestHeader.setBackgroundColor(headerColors.dark ? 0xff171717 : headerColors.gutter);
            TextView headerName = (TextView) findViewById(R.id.manifest_header_name);
            TextView headerType = (TextView) findViewById(R.id.manifest_header_type);
            if (headerName != null) {
                headerName.setTextColor(headerColors.text);
            }
            if (headerType != null) {
                headerType.setTextColor(headerColors.gutterText);
            }
        }
        this.manifestList.setVisibility(View.GONE);
        if (this.manifestEditor == null) {
            this.manifestEditor = new ManifestInlineEditor(this, headerColors);
            this.manifestEditor.setId(R.id.manifest_inline_editor);
            int index = this.manifestLayout.indexOfChild(this.manifestList);
            this.manifestLayout.addView(this.manifestEditor, index,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
            this.manifestEditor.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
                public void onTextChanged(CharSequence text, int start, int before, int count) {
                    if (!manifestLoading) {
                        manifestDirty = true;
                        manifestEditor.removeCallbacks(manifestAutosave);
                        manifestEditor.postDelayed(manifestAutosave, 1200);
                    }
                }
                public void afterTextChanged(Editable text) { }
            });
            TextView saveManifest = new TextView(this);
            saveManifest.setText(R.string.lab_save_manifest);
            saveManifest.setTextSize(12);
            saveManifest.setTextColor(headerColors.text);
            saveManifest.setGravity(android.view.Gravity.CENTER);
            int savePadding = (int) (10 * getResources().getDisplayMetrics().density);
            saveManifest.setPadding(savePadding, 0, savePadding, 0);
            saveManifest.setBackgroundResource(android.R.drawable.list_selector_background);
            saveManifest.setContentDescription(getString(R.string.lab_save_manifest));
            saveManifest.setOnClickListener(v -> {
                if (saveInlineManifest(true)) Toast.makeText(this, R.string.lab_manifest_saved, Toast.LENGTH_SHORT).show();
            });
            ((LinearLayout) manifestHeader).addView(saveManifest, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        loadInlineManifest();
        if (this.navigationMgr != null && this.resCurrentDir != null) {
            this.navigationMgr.gotoDirectory(this.resCurrentDir);
        }
        this.resourceParsed = true;
        updateCenterView();
    }

    static boolean lambda$showDecodedFileList$40(File dir, String filename) {
        return true;
    }

    public void resDirectoryChanged(String newDir, int upLevel) {
        this.resListAdapter.openDirectory(newDir);
        if (upLevel > 0) {
            Duo<Integer, Integer> pos = null;
            for (int i = 0; i < upLevel; i++) {
                try {
                    pos = this.resListPosition.pop();
                } catch (Exception e) {
                    return;
                }
            }
            if (pos != null) {
                this.resourceList.setSelectionFromTop(((Integer) pos.m1).intValue(), ((Integer) pos.m2).intValue());
            }
        }
    }

    @Override // com.gmail.heagoo.apkeditor.ResSelectionChangeListener
    public void selectionChanged(Set<Integer> selected) {
        boolean selectionMode = !selected.isEmpty();
        if (selectionMode) {
            this.resSearchLayout.setVisibility(8);
            this.resNaviHeader.setVisibility(8);
            this.resMenuLayout.setVisibility(0);
            this.resSelectHeader.setVisibility(0);
            String text = String.format(getString(R.string.num_items_selected), Integer.valueOf(selected.size()));
            this.resSelectTip.setText(text);
            return;
        }
        this.resMenuLayout.setVisibility(8);
        this.resSelectHeader.setVisibility(8);
        this.resSearchLayout.setVisibility(0);
        this.resNaviHeader.setVisibility(0);
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        String targetPath;
        int index;
        List<FileRecord> fileList = new ArrayList<>();
        String oldDir = this.resListAdapter.getData(fileList);
        FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }
        if (!this.resListAdapter.getCheckedItems().isEmpty()) {
            this.resListAdapter.reverseCheckStatus(position);
            return;
        }
        if (rec.isDir) {
            if (rec.fileName.equals("..")) {
                String targetPath2 = oldDir.substring(0, oldDir.lastIndexOf(47));
                targetPath = targetPath2;
                index = 1;
            } else {
                int index2 = this.resourceList.getFirstVisiblePosition();
                View v = this.resourceList.getChildAt(0);
                int top = v != null ? v.getTop() - this.resourceList.getPaddingTop() : 0;
                this.resListPosition.push(new Duo<>(Integer.valueOf(index2), Integer.valueOf(top)));
                targetPath = oldDir + "/" + rec.fileName;
                index = 0;
            }
            this.resListAdapter.openDirectory(targetPath);
            this.navigationMgr.gotoDirectory(targetPath);
            if (index != 0) {
                try {
                    Duo<Integer, Integer> pos = this.resListPosition.pop();
                    this.resourceList.setSelectionFromTop(((Integer) pos.m1).intValue(), ((Integer) pos.m2).intValue());
                    return;
                } catch (Exception e) {
                    return;
                }
            }
            this.resourceList.setSelectionAfterHeaderView();
            return;
        }
        openFile(oldDir, rec.fileName, rec.isInZip);
    }

    private void openEditableFile(String directory, String fileName, boolean bInZip, String entryName, String syntaxFileName) {
        String filePath = this.resListAdapter.getReplacedFilePath(entryName);
        if (filePath == null) {
            if (bInZip) {
                filePath = extractFileFromZip(entryName);
                if (filePath == null) {
                    String errMeesage = String.format(getString(R.string.cannot_open_xxx), entryName);
                    Toast.makeText((Context) this, (CharSequence) errMeesage, 0).show();
                    return;
                }
            } else {
                filePath = directory + "/" + fileName;
            }
        }
        openFileEditor(filePath, syntaxFileName, fileName, entryName);
    }

    protected void openFile(String directory, String fileName, boolean bInZip) {
        String entryName;
        if (directory.equals(this.decodeRootPath)) {
            entryName = fileName;
        } else {
            entryName = directory.substring(this.decodeRootPath.length() + 1) + "/" + fileName;
        }
        // The decoded root manifest uses the same inline editor as the manifest tab.
        // Do not redirect replaced or zipped entries, which may have a different source.
        if (this.isFullDecoding && !bInZip && "AndroidManifest.xml".equals(entryName)
                && this.resListAdapter.getReplacedFilePath(entryName) == null
                && this.manifestEditor != null) {
            this.manifestRadio.setChecked(true);
            manifestRadioClicked();
            return;
        }
        String syntaxFileName = getEditableSyntax(fileName);
        if (syntaxFileName != null) {
            openEditableFile(directory, fileName, bInZip, entryName, syntaxFileName);
            return;
        }
        String filePath = this.resListAdapter.getReplacedFilePath(entryName);
        if (filePath == null) {
            if (bInZip) {
                filePath = extractFileFromZip(entryName);
            } else if (!this.resListAdapter.isImageFile(fileName)) {
                filePath = directory + "/" + fileName;
            } else {
                filePath = extractFileFromZip(entryName);
            }
        }
        if (filePath != null) {
            this.filePathForExternal = filePath;
            this.entryNameForExternal = entryName;
            File f = new File(filePath);
            this.modifiedTimeBeforeOpen = f.lastModified();
            if (fileName.endsWith(".png")) {
                Intent intent = new Intent((Context) this, (Class<?>) PngEditActivity.class);
                ActivityUtil.attachParam(intent, "filePath", filePath);
                startActivityForResult(intent, RC_OPEN_EXTERNAL);
                return;
            }
            OpenFiles.openFile(this, filePath, RC_OPEN_EXTERNAL);
        }
    }

    private String getEditableSyntax(String fileName) {
        if (fileName.endsWith(".xml")) {
            return "xml.xml";
        }
        if (fileName.endsWith(".smali")) {
            return "smali.xml";
        }
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "html.xml";
        }
        if (fileName.endsWith(".css")) {
            return "css.xml";
        }
        if (fileName.endsWith(".java")) {
            return "java.xml";
        }
        if (fileName.endsWith(".json")) {
            return "json.xml";
        }
        if (fileName.endsWith(".txt")) {
            return "txt.xml";
        }
        if (fileName.endsWith(".js")) {
            return "js.xml";
        }
        return null;
    }

    protected boolean replaceFile(String replacedPath, String replacingPath) {
        try {
            AtomicProjectFile.replace(new File(replacedPath), temp -> {
                try (FileInputStream in = new FileInputStream(replacingPath);
                     FileOutputStream out = new FileOutputStream(temp)) {
                    IOUtils.copy(in, out);
                    out.getFD().sync();
                }
            });
            return true;
        } catch (IOException e) {
            new AlertDialog.Builder(this).setMessage(getString(R.string.lab_manifest_save_failed) + "\n" + e)
                    .setPositiveButton(android.R.string.ok, null).show();
            return false;
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

    private boolean isImageFile(String fileName) {
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".bmp")) {
            return true;
        }
        return false;
    }

    private void openFileEditor(String filePath, String syntaxFileName, String displayFileName, String entryName) {
        if ("res/values/colors.xml".equals(entryName)) {
            Intent intent = new Intent((Context) this, (Class<?>) ColorXmlActivity.class);
            ActivityUtil.attachParam(intent, "xmlPath", filePath);
            startActivityForResult(intent, 3);
        } else {
            Intent intent2 = TextEditor.getEditorIntent(this, filePath, this.apkPath);
            ActivityUtil.attachParam(intent2, "syntaxFileName", syntaxFileName);
            if (displayFileName != null) {
                ActivityUtil.attachParam(intent2, "displayFileName", displayFileName);
            }
            ActivityUtil.attachParam(intent2, "extraString", entryName);
            startActivityForResult(intent2, 0);
        }
    }

    @Override // android.widget.AdapterView.OnItemLongClickListener
    public boolean onItemLongClick(AdapterView<?> parent, View arg1, final int position, long id) {
        final boolean isFirstItem = position == 0;
        parent.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override // android.view.View.OnCreateContextMenuListener
            public final void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                ApkInfoActivity.this.m69x6899fc00(position, isFirstItem, contextMenu, view, contextMenuInfo);
            }
        });
        return false;
    }

    void m69x6899fc00(final int position, boolean isFirstItem, ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuItem.OnMenuItemClickListener listener;
        List<FileRecord> records = new ArrayList<>();
        String curPath = this.resListAdapter.getData(records);
        boolean isDir = records.get(position).isDir;
        if (!isFirstItem) {
            MenuItem item1 = menu.add(0, 1, 0, R.string.delete);
            item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override // android.view.MenuItem.OnMenuItemClickListener
                public final boolean onMenuItemClick(MenuItem menuItem) {
                    return ApkInfoActivity.this.m63xd70f83c6(position, menuItem);
                }
            });
        }
        if (!isFirstItem || curPath.equals(this.decodeRootPath)) {
            MenuItem item2 = menu.add(0, 2, 0, R.string.extract);
            item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override // android.view.MenuItem.OnMenuItemClickListener
                public final boolean onMenuItemClick(MenuItem menuItem) {
                    return ApkInfoActivity.this.m64x99fbed25(position, menuItem);
                }
            });
        }
        if (!isFirstItem || curPath.equals(this.decodeRootPath)) {
            MenuItem item3 = menu.add(0, 3, 0, R.string.replace);
            if (isDir) {
                listener = new MenuItem.OnMenuItemClickListener() {
                    @Override // android.view.MenuItem.OnMenuItemClickListener
                    public final boolean onMenuItemClick(MenuItem menuItem) {
                        return ApkInfoActivity.this.m65x5ce85684(position, menuItem);
                    }
                };
            } else {
                listener = new MenuItem.OnMenuItemClickListener() {
                    @Override // android.view.MenuItem.OnMenuItemClickListener
                    public final boolean onMenuItemClick(MenuItem menuItem) {
                        return ApkInfoActivity.this.m66x1fd4bfe3(position, menuItem);
                    }
                };
            }
            item3.setOnMenuItemClickListener(listener);
        }
        MenuItem item4 = menu.add(0, 4, 0, R.string.add_a_file);
        item4.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override // android.view.MenuItem.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return ApkInfoActivity.this.m67xe2c12942(position, menuItem);
            }
        });
        MenuItem item5 = menu.add(0, 5, 0, R.string.new_folder);
        item5.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override // android.view.MenuItem.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return ApkInfoActivity.this.m68xa5ad92a1(position, menuItem);
            }
        });
    }

    boolean m63xd70f83c6(int position, MenuItem item) {
        List<Integer> positions = new ArrayList<>(1);
        positions.add(Integer.valueOf(position));
        this.resListAdapter.deleteFile(positions);
        this.resListAdapter.dumpChangedFiles();
        return true;
    }

    boolean m64x99fbed25(int position, MenuItem item) {
        List<Integer> positions = new ArrayList<>(1);
        positions.add(Integer.valueOf(position));
        extractFileOrDir(positions);
        return true;
    }

    boolean m65x5ce85684(int position, MenuItem item) {
        replaceFolder(position);
        this.resListAdapter.dumpChangedFiles();
        return true;
    }

    boolean m66x1fd4bfe3(int position, MenuItem item) {
        replaceFile(position);
        this.resListAdapter.dumpChangedFiles();
        return true;
    }

    boolean m67xe2c12942(int position, MenuItem item) {
        addFile(position);
        return true;
    }

    boolean m68xa5ad92a1(int position, MenuItem item) {
        createFolder(position);
        return true;
    }

    private List<FileCopyDialog.CopySource> getCopySources(List<Integer> positions) {
        List<FileCopyDialog.CopySource> result = new ArrayList<>();
        List<FileRecord> records = new ArrayList<>();
        String dirPath = this.resListAdapter.getData(records);
        Iterator<Integer> it = positions.iterator();
        while (it.hasNext()) {
            int position = it.next().intValue();
            if (position < records.size()) {
                FileRecord fileRec = records.get(position);
                FileCopyDialog.CopySource src = new FileCopyDialog.CopySource();
                if (fileRec.isInZip) {
                    src.path = (dirPath + "/" + fileRec.fileName).substring(this.decodeRootPath.length() + 1);
                } else {
                    src.path = dirPath + "/" + fileRec.fileName;
                }
                src.isInApk = fileRec.isInZip;
                src.isDir = fileRec.isDir;
                result.add(src);
            }
        }
        return result;
    }

    protected void extractFileOrDir(List<Integer> positions) {
        List<FileCopyDialog.CopySource> sources = getCopySources(positions);
        extractFileOrDir_internal(sources);
    }

    protected void extractFileOrDir(String filepath) {
        List<FileCopyDialog.CopySource> sources = new ArrayList<>();
        FileCopyDialog.CopySource s = new FileCopyDialog.CopySource();
        s.isInApk = false;
        s.isDir = false;
        s.path = filepath;
        sources.add(s);
        extractFileOrDir_internal(sources);
    }

    private void extractFileOrDir_internal(final List<FileCopyDialog.CopySource> sources) {
        String dlgTitle = getString(R.string.select_folder);
        FileSelectDialog.IFileSelection callback = new FileSelectDialog.IFileSelection() {
            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                FileCopyDialog dlg = new FileCopyDialog(ApkInfoActivity.this, ApkInfoActivity.this.apkPath, ApkInfoActivity.this.decodeRootPath, ApkInfoActivity.this.fileEntry2ZipEntry, sources, filePath);
                dlg.show();
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        };
        FileSelectDialog dlg = new FileSelectDialog(this, callback, null, null, dlgTitle, true, false, false, null);
        dlg.show();
    }

    private String extractFileFromZip(String entryName) {
        String filename;
        int pos = entryName.lastIndexOf("/");
        if (pos != -1) {
            filename = entryName.substring(pos + 1);
        } else {
            filename = entryName;
        }
        String fileType = "";
        int pos2 = filename.lastIndexOf(".");
        if (pos2 != -1) {
            fileType = filename.substring(pos2);
        }
        try {
            String zipEntry = this.fileEntry2ZipEntry.get(entryName);
            String dstPath = SDCard.makeWorkingDir(this) + TMP_EDITOR_FILE + fileType;
            if (zipEntry != null) {
                entryName = zipEntry;
            }
            ZipUtil.unzipFileTo(this.apkPath, entryName, dstPath);
            return dstPath;
        } catch (Exception e) {
            return null;
        }
    }

    protected void replaceFile(int position) {
        List<FileRecord> records = new ArrayList<>();
        String dirPath = this.resListAdapter.getData(records);
        FileRecord rec = records.get(position);
        String filepath = dirPath + "/" + rec.fileName;
        replaceFile(filepath, (SomethingChangedListener) null);
    }

    protected void replaceFile(String replacedPath, final SomethingChangedListener listener) {
        String suffix;
        int slashPosition = replacedPath.lastIndexOf(47);
        String fileName = replacedPath.substring(slashPosition + 1);
        int dotPosition = fileName.lastIndexOf(46);
        if (dotPosition == -1) {
            suffix = null;
        } else {
            String suffix2 = fileName.substring(dotPosition);
            suffix = suffix2;
        }
        final String fileTypeStr = suffix;
        FileSelectDialog dlg = new FileSelectDialog(this, new FileSelectDialog.IFileSelection() {
            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                if (openFile) {
                    ApkInfoActivity.this.saveParams(filePath, extraStr, listener);
                    OpenFiles.openFile(ApkInfoActivity.this, filePath, 1001);
                } else {
                    ApkInfoActivity.this.resListAdapter.replaceFile(extraStr, filePath);
                    if (listener != null) {
                        listener.somethingChanged();
                    }
                }
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public boolean isInterestedFile(String filename, String extraStr) {
                if (fileTypeStr != null) {
                    return filename.endsWith(fileTypeStr);
                }
                return true;
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        }, suffix, replacedPath, null, false, false, true, null);
        dlg.show();
    }

    protected void replaceFolder(int position) {
        String dlgTitle = getString(R.string.select_folder_replace);
        List<FileRecord> records = new ArrayList<>();
        String dirPath = this.resListAdapter.getData(records);
        FileRecord rec = records.get(position);
        FileSelectDialog.IFileSelection callback = new FileSelectDialog.IFileSelection() {
            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                String workingDir = SDCard.getEditorDirectory() + "/tmp";
                if (workingDir.startsWith(filePath)) {
                    Toast.makeText((Context) ApkInfoActivity.this, R.string.select_folder_err2, 1).show();
                } else if (!filePath.startsWith(workingDir)) {
                    ApkInfoActivity.this.resListAdapter.replaceFolder(extraStr, filePath);
                } else {
                    Toast.makeText((Context) ApkInfoActivity.this, R.string.select_folder_err1, 1).show();
                }
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public String getConfirmMessage(String filePath, String extraStr) {
                String replaced = extraStr.substring(ApkInfoActivity.this.decodeRootPath.length() + 1);
                String message = ApkInfoActivity.this.getString(R.string.folder_replace_tip);
                return String.format(message, replaced, filePath);
            }
        };
        FileSelectDialog dlg = new FileSelectDialog(this, callback, null, dirPath + "/" + rec.fileName, dlgTitle, true, true, false, null);
        dlg.show();
    }

    protected void addFile(int position) {
        String dirPath = this.resListAdapter.getData(null);
        FileSelectDialog dlg = new FileSelectDialog(this, new FileSelectDialog.IFileSelection() {
            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                String name = filePath.substring(filePath.lastIndexOf("/") + 1);
                ApkInfoActivity.this.resListAdapter.addFile(extraStr + "/" + name, filePath);
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Override // com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        }, null, dirPath, getString(R.string.add_a_file));
        dlg.show();
    }

    protected void createFolder(int position) {
        boolean showImportFolder = this.isFullDecoding;
        new AddFolderDialog(this, this, showImportFolder).show();
    }

    @Override // com.gmail.heagoo.apkeditor.IManifestChangeCallback
    public String tryToDeleteSection(LineRecord lineRec) {
        return null;
    }

    private File manifestDraftFile() {
        return new File(getFilesDir(), "manifest-draft-" + this.earlyEditToken);
    }

    private void loadInlineManifest() {
        if (this.manifestEditor == null || this.decodeRootPath == null || this.manifestDirty) return;
        try {
            String text = ManifestDocumentFile.read(new File(this.decodeRootPath, "AndroidManifest.xml"));
            this.manifestBaseline = text;
            if (this.restoreManifestDraft) {
                Object draft = IOUtils.readObjectFromFile(manifestDraftFile().getAbsolutePath());
                if (draft instanceof String[] && ((String[]) draft).length == 2) {
                    String[] values = (String[]) draft;
                    text = values[0];
                    this.manifestBaseline = values[1];
                    this.manifestDirty = true;
                }
                this.restoreManifestDraft = false;
            }
            this.manifestLoading = true;
            // Only whitespace between attributes is changed; the on-disk file is
            // left untouched until the user actually edits or saves it.
            if (!this.manifestDirty) text = ManifestTagFormatter.format(text);
            this.manifestEditor.setText(text);
            this.manifestEditor.setEnabled(true);
        } catch (Exception e) {
            this.manifestEditor.setEnabled(false);
            Toast.makeText(this, getString(R.string.lab_manifest_load_failed) + "\n" + e, Toast.LENGTH_LONG).show();
        } finally {
            this.manifestLoading = false;
        }
    }

    /** Flush before operations that read the file; never overwrite external changes. */
    private boolean saveInlineManifest(boolean showError) {
        if (this.manifestEditor == null || !this.manifestDirty) return true;
        this.manifestEditor.removeCallbacks(this.manifestAutosave);
        try {
            File file = new File(this.decodeRootPath, "AndroidManifest.xml");
            String text = this.manifestEditor.getText().toString();
            ManifestDocumentFile.save(file, this.manifestBaseline, text);
            this.manifestBaseline = text;
            this.manifestDirty = false;
            this.manifestModified = true;
            manifestDraftFile().delete();
            return true;
        } catch (Exception e) {
            if (showError) new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.lab_manifest_save_failed) + "\n" + (e instanceof ManifestDocumentFile.Conflict ? getString(R.string.lab_manifest_external_change) : e.getMessage()))
                    .setPositiveButton(android.R.string.ok, null).show();
            else Toast.makeText(this, R.string.lab_manifest_save_failed, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override // com.gmail.heagoo.apkeditor.IManifestChangeCallback
    public boolean manifestChanged(String newContent) {
        try {
            AtomicProjectFile.writeText(new File(this.decodeRootPath, "AndroidManifest.xml"), newContent);
            this.manifestModified = true;
            return true;
        } catch (IOException e) {
            new AlertDialog.Builder(this).setMessage(getString(R.string.lab_manifest_save_failed) + "\n" + e)
                    .setPositiveButton(android.R.string.ok, null).show();
            return false;
        }
    }

    protected void fakeShow() {
        Toast.makeText((Context) this, (CharSequence) "Manifest editing is disabled (seems not a genuine version)", 1).show();
    }

    public void dexDecodingStarted() {
        this.dexConversionInProgress = true;
        this.dex2smaliImage.setVisibility(4);
        this.decodeResultLayout.setVisibility(4);
        this.decodeProgressRow.setVisibility(0);
        this.decodeProgressBar.setVisibility(0);
        this.decodeProgressText.setText(R.string.lab_dex_progress_preparing);
        this.decodeProgressText.setVisibility(0);
    }

    public void dexDecodingProgress(int current, int total) {
        if (this.decodeProgressText != null) {
            this.decodeProgressText.setText(current + "/" + total);
        }
    }

    public void dexClassProgress(int dex, int dexTotal, int done, int total) {
        if (this.decodeProgressText != null) {
            this.decodeProgressText.setText(getString(R.string.lab_dex_class_progress, new Object[]{Integer.valueOf(dex), Integer.valueOf(dexTotal), Integer.valueOf(done), Integer.valueOf(total)}));
        }
    }

    public void dexDecodingFinished(boolean ret, String strError, String strWarning) {
        this.dexConversionInProgress = false;
        this.dexDecoded = ret;
        if (this.dexDecodedCallback != null) {
            this.dexDecodedCallback.callbackFunc();
            this.dexDecodedCallback = null;
        }
        boolean showResult = true;
        if (ret) {
            if (strWarning != null) {
                this.decodeResultTitle.setText(R.string.succeed_with_warning);
                String content = getString(R.string.warning) + ": " + strWarning;
                this.decodeResultDetail.setText(content);
                this.decodeResultDetail.setVisibility(0);
            } else {
                showResult = false;
                this.decodeResultTitle.setText(R.string.succeed);
                this.decodeResultDetail.setVisibility(8);
            }
        } else {
            this.decodeResultTitle.setText(R.string.failed);
            this.decodeResultDetail.setVisibility(0);
            if (strError != null) {
                this.decodeResultDetail.setText(strError);
            } else {
                this.decodeResultDetail.setText(R.string.unknown_error);
            }
        }
        this.dex2smaliImage.setVisibility(4);
        this.decodeProgressBar.setVisibility(4);
        this.decodeProgressText.setVisibility(8);
        this.decodeProgressRow.setVisibility(8);
        if (showResult) {
            this.decodeResultLayout.setVisibility(0);
        } else {
            this.dexDecodeLayout.setVisibility(8);
            Toast.makeText((Context) this, R.string.dex_decode_succeed, 1).show();
        }
        String curFolder = this.resListAdapter.getData(null);
        if (curFolder.endsWith("/decoded")) {
            this.resListAdapter.openDirectory(curFolder);
        }
    }

    @Override // android.view.View.OnLongClickListener
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.imageview_dex2smali) {
            v.performClick();
            return true;
        }
        return false;
    }

    public String getDecodeRootPath() {
        return this.decodeRootPath;
    }

    public ResListAdapter getResListAdapter() {
        return this.resListAdapter;
    }

    public boolean isDexDecoded() {
        return this.dexDecoded;
    }

    public String getApkPath() {
        return this.apkPath;
    }

    public ApkInfoParser.AppInfo getApkInfo() {
        return this.apkInfo;
    }

    public void onStart() {
        super.onStart();
    }

    public void onStop() {
        super.onStop();
    }

    public void saveParams(String filePath, String extraStr, SomethingChangedListener listener) {
        this.savedParam_filePath = filePath;
        this.savedParam_extraStr = extraStr;
        this.savedParam_listener = listener;
    }

    class MyServiceConnection implements ServiceConnection {
        private boolean bSign;

        public MyServiceConnection(boolean bSign) {
            this.bSign = bSign;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApkComposeService.ComposeServiceBinder binder = (ApkComposeService.ComposeServiceBinder) service;
            if (binder.isRunning()) {
                ApkInfoActivity.this.showCannotStartBuildDialog();
            } else {
                ApkInfoActivity.this.launchWithoutCheck(this.bSign);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
