package com.gmail.heagoo.apkeditor;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.apksig.ApkVerifier;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.common.EditorTheme;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class ApkInspectionActivity extends Activity implements FileSelectDialog.IFileSelection {
    private boolean busy;
    private Button compare;
    private Button decoded;
    private String first;
    private TextView firstLabel;
    private TextView report;
    private String second;
    private TextView secondLabel;
    private Button verify;

    interface Work {
        String execute() throws Exception;
    }

    @Override
    protected void onCreate(Bundle state) {
        EditorTheme.apply(this);
        super.onCreate(state);
        if (state != null) {
            this.first = state.getString("first");
            this.second = state.getString("second");
        }
        setTitle("Проверка APK");
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        int pad = (int) (getResources().getDisplayMetrics().density * 16.0f);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);
        setContentView(scrollView);
        TextView intro = new TextView(this);
        intro.setText("Сравнение файлов APK и проверка цифровой подписи. Исходные файлы не изменяются.");
        root.addView(intro);
        Button chooseFirst = new Button(this);
        chooseFirst.setText("Выбрать первый APK");
        root.addView(chooseFirst);
        this.firstLabel = new TextView(this);
        this.firstLabel.setTextIsSelectable(true);
        root.addView(this.firstLabel);
        Button chooseSecond = new Button(this);
        chooseSecond.setText("Выбрать второй APK");
        root.addView(chooseSecond);
        this.secondLabel = new TextView(this);
        this.secondLabel.setTextIsSelectable(true);
        root.addView(this.secondLabel);
        Button advanced = new Button(this);
        advanced.setText("Сравнение APK, файлов и папок");
        root.addView(advanced);
        advanced.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ApkInspectionActivity.this.m97x2fe1d9ac(view);
            }
        });
        this.compare = new Button(this);
        this.compare.setText("Сравнить два APK");
        root.addView(this.compare);
        this.decoded = new Button(this);
        this.decoded.setText("Сравнить декодированные XML и smali");
        root.addView(this.decoded);
        this.verify = new Button(this);
        this.verify.setText("Проверить подпись первого APK");
        root.addView(this.verify);
        this.report = new TextView(this);
        this.report.setTextIsSelectable(true);
        this.report.setPadding(0, pad, 0, pad);
        root.addView(this.report);
        updateLabels();
        chooseFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ApkInspectionActivity.this.m98x218b7fcb(view);
            }
        });
        chooseSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ApkInspectionActivity.this.m99x133525ea(view);
            }
        });
        this.compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ApkInspectionActivity.this.m100x4decc09(view);
            }
        });
        this.decoded.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ApkInspectionActivity.this.m102xe8321847(view);
            }
        });
        this.verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ApkInspectionActivity.this.m104xcb856485(view);
            }
        });
    }

    void m97x2fe1d9ac(View v) {
        startActivity(new Intent(this, (Class<?>) ComparisonActivity.class));
    }

    void m98x218b7fcb(View v) {
        select("first");
    }

    void m99x133525ea(View v) {
        select("second");
    }

    void m100x4decc09(View v) {
        if (this.first == null || this.second == null) {
            this.report.setText("Выберите два APK.");
        } else {
            openComparison(0);
        }
    }

    void m101xf6887228(DialogInterface d, int w) {
        openComparison(3);
    }

    void m102xe8321847(View v) {
        if (this.first == null || this.second == null) {
            this.report.setText("Выберите два APK.");
        } else {
            new AlertDialog.Builder(this).setMessage("Декодирование обоих APK может занять много времени и памяти. Нужен свободный объём для временных файлов.").setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Продолжить", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInspectionActivity.this.m101xf6887228(dialogInterface, i);
                }
            }).show();
        }
    }

    String m103xd9dbbe66(String a) throws Exception {
        return signature(new File(a));
    }

    void m104xcb856485(View v) {
        if (this.first == null) {
            this.report.setText("Выберите первый APK.");
        } else {
            final String a = this.first;
            run(new Work() {
                @Override
                public final String execute() throws Exception {
                    return ApkInspectionActivity.this.m103xd9dbbe66(a);
                }
            });
        }
    }

    private void openComparison(int mode) {
        Intent intent = new Intent(this, (Class<?>) ComparisonActivity.class);
        intent.putExtra(ComparisonActivity.EXTRA_FIRST, this.first);
        intent.putExtra(ComparisonActivity.EXTRA_SECOND, this.second);
        intent.putExtra(ComparisonActivity.EXTRA_MODE, mode);
        intent.putExtra(ComparisonActivity.EXTRA_RUN, true);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString("first", this.first);
        out.putString("second", this.second);
        super.onSaveInstanceState(out);
    }

    private void updateLabels() {
        this.firstLabel.setText(this.first == null ? "Файл не выбран" : this.first);
        this.secondLabel.setText(this.second != null ? this.second : "Файл не выбран");
    }

    private void select(String slot) {
        if (this.busy) {
            return;
        }
        new FileSelectDialog(this, this, ".apk", slot, "Выберите APK").show();
    }

    @Override
    public void fileSelectedInDialog(String path, String slot, boolean open) {
        if ("first".equals(slot)) {
            this.first = path;
        } else if ("second".equals(slot)) {
            this.second = path;
        }
        updateLabels();
    }

    @Override
    public boolean isInterestedFile(String filename, String extra) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".apk");
    }

    @Override
    public String getConfirmMessage(String filePath, String extra) {
        return filePath;
    }

    private void run(final Work work) {
        if (this.busy) {
            return;
        }
        this.busy = true;
        this.compare.setEnabled(false);
        this.verify.setEnabled(false);
        this.decoded.setEnabled(false);
        this.report.setText("Проверка выполняется…");
        new Thread(new Runnable() {
            @Override
            public final void run() {
                ApkInspectionActivity.this.m106lambda$run$9$comgmailheagooapkeditorApkInspectionActivity(work);
            }
        }, "apk-inspection").start();
    }

    void m105lambda$run$8$comgmailheagooapkeditorApkInspectionActivity(String r) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        this.busy = false;
        this.compare.setEnabled(true);
        this.verify.setEnabled(true);
        this.decoded.setEnabled(true);
        this.report.setText(r);
    }

    void m106lambda$run$9$comgmailheagooapkeditorApkInspectionActivity(Work work) {
        String result;
        try {
            result = work.execute();
        } catch (Exception e) {
            result = "Ошибка проверки: " + e;
        }
        final String r = result;
        runOnUiThread(new Runnable() {
            @Override
            public final void run() {
                ApkInspectionActivity.this.m105lambda$run$8$comgmailheagooapkeditorApkInspectionActivity(r);
            }
        });
    }

    private static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & ParserRule.MATCH_TYPE_CONTEXT)));
        }
        return s.toString();
    }

    private static String hash(byte[] bytes) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private String signature(File f) throws Exception {
        File idsig = new File(f.getAbsolutePath() + ".idsig");
        ApkVerifier.Builder builder = new ApkVerifier.Builder(f).setMaxCheckedPlatformVersion(37);
        if (idsig.isFile()) {
            builder.setV4SignatureFile(idsig);
        }
        ApkVerifier.Result r = builder.build().verify();
        StringBuilder s = new StringBuilder("APK: ").append(f).append("\nДиапазон проверки: от minSdk APK до Android API 37.\n\n");
        s.append("Результат проверки подписи: ").append(r.isVerified() ? "ПРОЙДЕНА" : "НЕ ПРОЙДЕНА").append('\n');
        s.append("Проверены схемы: v1=").append(r.isVerifiedUsingV1Scheme()).append(", v2=").append(r.isVerifiedUsingV2Scheme()).append(", v3=").append(r.isVerifiedUsingV3Scheme()).append(".\n");
        s.append(idsig.isFile() ? "V4 (соседний .idsig): " + r.isVerifiedUsingV4Scheme() + Document.LINE_SEPARATOR_UNIX : "V4: не проверялась — соседний файл .apk.idsig не найден.\n");
        s.append("Отсутствие проверенной схемы не означает, что блок отсутствует.\n");
        Set<String> apkSigners = new TreeSet<>();
        for (X509Certificate c : r.getSignerCertificates()) {
            String h = hash(c.getEncoded());
            apkSigners.add(h);
            s.append("\nСЕРТИФИКАТ № ").append(apkSigners.size()).append(Document.LINE_SEPARATOR_UNIX).append(CertificateDetails.describe(c));
        }
        if (!r.getAllErrors().isEmpty()) {
            s.append("\nОшибки: ").append(r.getAllErrors()).append('\n');
        }
        if (!r.getWarnings().isEmpty()) {
            s.append("\nПредупреждения: ").append(r.getWarnings()).append('\n');
        }
        PackageManager pm = getPackageManager();
        PackageInfo archive = pm.getPackageArchiveInfo(f.getAbsolutePath(), 0);
        if (archive == null || archive.packageName == null) {
            s.append("\nAndroid не смог прочитать сведения о пакете; совместимость обновления не определена.");
            return s.toString();
        }
        s.append("\nПакет: ").append(archive.packageName).append("\nВерсия: ").append(archive.versionName).append(" (код ").append(Build.VERSION.SDK_INT >= 28 ? archive.getLongVersionCode() : archive.versionCode).append(")\n");
        try {
            int flags = Build.VERSION.SDK_INT >= 28 ? 134217728 : 64;
            PackageInfo installed = pm.getPackageInfo(archive.packageName, flags);
            Set<String> installedSigners = new TreeSet<>();
            Signature[] sigs = Build.VERSION.SDK_INT >= 28 ? installed.signingInfo == null ? null : installed.signingInfo.getApkContentsSigners() : installed.signatures;
            if (sigs != null) {
                int length = sigs.length;
                int i = 0;
                while (i < length) {
                    Signature sig = sigs[i];
                    installedSigners.add(hash(sig.toByteArray()));
                    i++;
                    flags = flags;
                }
            }
            long av = Build.VERSION.SDK_INT >= 28 ? archive.getLongVersionCode() : archive.versionCode;
            long iv = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
            s.append("Установленная версия: ").append(installed.versionName).append(" (код ").append(iv).append(")\n");
            if (!r.isVerified() || apkSigners.isEmpty() || installedSigners.isEmpty()) {
                s.append("Совместимость сертификатов определить нельзя.\n");
            } else if (apkSigners.equals(installedSigners)) {
                s.append("Текущие сертификаты совпадают. ").append(av < iv ? "Код версии ниже установленного — обычное обновление не подойдёт.\n" : "Код версии не ниже установленного.\n");
            } else {
                s.append("Текущие сертификаты различаются. Возможную ротацию ключей эта проверка не определяет.\n");
            }
            s.append("Это предварительная оценка, а не гарантия установки: влияют lineage, split APK, политика устройства и другие условия.\n");
        } catch (PackageManager.NameNotFoundException e) {
            s.append("Пакет не найден среди видимых установленных приложений; сравнение обновления не выполнено.\n");
        }
        return s.toString();
    }
}
