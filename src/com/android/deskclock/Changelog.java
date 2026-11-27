package com.android.deskclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** "What's new" dialog, shown on demand and once after each update. */
public final class Changelog {

    private Changelog() { }

    private static long versionCode(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).getLongVersionCode();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String versionName(Context c) {
        try {
            final String v = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    public static void show(Context c) {
        final float d = c.getResources().getDisplayMetrics().density;
        final TextView text = new TextView(c);
        text.setText(Html.fromHtml(c.getString(R.string.cl_changelog), Html.FROM_HTML_MODE_COMPACT));
        text.setTextSize(15);
        text.setLineSpacing(0, 1.2f);
        text.setTextColor(ThemeUtils.resolveColor(c, R.attr.colorOnSurfaceVariant));
        text.setPadding((int) (24 * d), (int) (8 * d), (int) (24 * d), 0);
        final ScrollView scroll = new ScrollView(c);
        scroll.addView(text);
        final String v = versionName(c);
        new MaterialAlertDialogBuilder(c)
                .setTitle(c.getString(R.string.cl_whats_new) + (v.isEmpty() ? "" : " · " + v))
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** Shows the changelog once after the app is updated (not on a fresh install). */
    public static void maybeShowAfterUpdate(Activity a) {
        final SharedPreferences p = a.getSharedPreferences("changelog", Context.MODE_PRIVATE);
        final long current = versionCode(a);
        final long seen = p.getLong("seen", -1);
        p.edit().putLong("seen", current).apply();
        if (seen != -1 && seen != current) show(a);
    }
}
