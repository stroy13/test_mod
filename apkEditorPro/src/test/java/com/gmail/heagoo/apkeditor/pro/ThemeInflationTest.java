package com.gmail.heagoo.apkeditor.pro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.gmail.heagoo.common.EditorTheme;
import com.gmail.heagoo.common.ThemeColors;
import com.gmail.heagoo.common.ThemeRegistry;
import com.gmail.heagoo.common.ThemedActivity;
import com.gmail.heagoo.common.ThemedAppCompatActivity;
import com.gmail.heagoo.common.ThemedPreferenceActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Inflates real screens and dialogs under all twelve selectable palettes. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
public class ThemeInflationTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    private void select(int id) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("AppTheme", String.valueOf(id)).commit();
    }

    private static double luminance(int color) {
        double[] channels = {((color >> 16) & 255) / 255.0, ((color >> 8) & 255) / 255.0, (color & 255) / 255.0};
        for (int i = 0; i < channels.length; i++) {
            double c = channels[i];
            channels[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
    }

    private static void assertReadable(String palette, int foreground, int background) {
        double a = luminance(foreground);
        double b = luminance(background);
        double contrast = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
        assertTrue(palette + " contrast too low: " + contrast, contrast >= 4.5);
    }

    private void assertPalette(Activity activity, int id) {
        String palette = ThemeRegistry.styleName(id);
        assertEquals(palette, id, EditorTheme.selected(activity));
        int surface = ThemeColors.color(activity, com.gmail.heagoo.common.R.attr.labSurface);
        int text = ThemeColors.color(activity, com.gmail.heagoo.common.R.attr.labText);
        int muted = ThemeColors.color(activity, com.gmail.heagoo.common.R.attr.labMuted);
        int accent = ThemeColors.color(activity, com.gmail.heagoo.common.R.attr.labAccent);
        assertTrue(palette, surface != 0 && text != 0);
        assertReadable(palette, text, surface);
        assertReadable(palette, muted, surface);
        assertReadable(palette, accent, surface);
    }

    @Test
    public void allPalettesApplyToFrameworkAppCompatAndPreferenceActivities() {
        for (int id = 0; id < ThemeRegistry.count(); id++) {
            select(id);
            ActivityController<ProbeActivity> framework = Robolectric.buildActivity(ProbeActivity.class).create();
            assertPalette(framework.get(), id);
            framework.destroy();

            ActivityController<ProbeCompatActivity> compat = Robolectric.buildActivity(ProbeCompatActivity.class).create();
            assertPalette(compat.get(), id);
            compat.destroy();

            ActivityController<ProbePreferenceActivity> prefs = Robolectric.buildActivity(ProbePreferenceActivity.class).create();
            assertPalette(prefs.get(), id);
            prefs.destroy();
        }
    }

    @Test
    public void allPalettesInflatePreviouslyUnthemedScreensAndDialogs() {
        int[] layouts = {
                R.layout.activity_commonedit,
                R.layout.activity_apkcompose,
                R.layout.activity_projectlist,
                R.layout.dlg_decodemode,
                R.layout.dlg_manifestline,
                R.layout.dlg_processing,
                R.layout.pngeditor_scale_options,
                R.layout.sql_activity_tableview,
                R.layout.appdm_activity_dataoverview
        };
        for (int id = 0; id < ThemeRegistry.count(); id++) {
            select(id);
            ActivityController<ProbeActivity> controller = Robolectric.buildActivity(ProbeActivity.class).create();
            ProbeActivity activity = controller.get();
            LayoutInflater inflater = LayoutInflater.from(activity);
            for (int layout : layouts) {
                View view = inflater.inflate(layout, null);
                assertNotNull(ThemeRegistry.styleName(id) + " layout " + layout, view);
            }

            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(R.string.app_name)
                    .setView(inflater.inflate(R.layout.dlg_decodemode, null))
                    .create();
            dialog.show();
            Context dialogContext = dialog.getContext();
            int dialogText = ThemeColors.color(dialogContext, com.gmail.heagoo.common.R.attr.labText);
            int activityText = ThemeColors.color(activity, com.gmail.heagoo.common.R.attr.labText);
            assertEquals(ThemeRegistry.styleName(id), activityText, dialogText);
            TextView option = dialog.findViewById(R.id.decode_all_files);
            assertNotNull(option);
            assertEquals(ThemeRegistry.styleName(id), activityText, option.getCurrentTextColor());
            dialog.dismiss();
            controller.destroy();
        }
    }

    public static class ProbeActivity extends ThemedActivity {
        @Override
        protected void onCreate(Bundle state) {
            super.onCreate(state);
            setContentView(new TextView(this));
        }
    }

    public static class ProbeCompatActivity extends ThemedAppCompatActivity {
        @Override
        protected void onCreate(Bundle state) {
            super.onCreate(state);
            setContentView(new TextView(this));
        }
    }

    public static class ProbePreferenceActivity extends ThemedPreferenceActivity {
        @Override
        protected void onCreate(Bundle state) {
            super.onCreate(state);
        }
    }
}
