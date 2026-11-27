package com.android.deskclock.alarms;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Records snoozes and wake-up times, and shows them as a small chart. */
public final class WakeStats {

    private WakeStats() { }

    private static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext()
                .getSharedPreferences("wake_stats", Context.MODE_PRIVATE);
    }

    public static void onSnooze(Context c, Long alarmId) {
        if (alarmId == null) return;
        final SharedPreferences p = prefs(c);
        p.edit().putInt("s" + alarmId, p.getInt("s" + alarmId, 0) + 1).apply();
    }

    public static void onDismiss(Context c, Long alarmId) {
        if (alarmId == null) return;
        final SharedPreferences p = prefs(c);
        final int snoozes = p.getInt("s" + alarmId, 0);
        final Calendar now = Calendar.getInstance();
        final String rec = String.format(Locale.US, "%04d%02d%02d,%d,%d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE), snoozes);
        final List<String> recs = load(c);
        recs.add(rec);
        while (recs.size() > 30) recs.remove(0);
        p.edit().putString("records", String.join(";", recs)).remove("s" + alarmId).apply();
    }

    private static List<String> load(Context c) {
        final String raw = prefs(c).getString("records", "");
        final List<String> out = new ArrayList<>();
        for (String r : raw.split(";")) if (!r.isEmpty()) out.add(r);
        return out;
    }

    private static String hhmm(int minutes) {
        return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }

    public static void show(Context c) {
        final List<String> recs = load(c);
        final com.google.android.material.dialog.MaterialAlertDialogBuilder b =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(c)
                        .setTitle("Wake-up stats")
                        .setPositiveButton(android.R.string.ok, null);
        if (recs.isEmpty()) {
            b.setMessage("No data yet. Stats are recorded when you dismiss an alarm "
                    + "from the ringing screen.").show();
            return;
        }
        final List<int[]> data = new ArrayList<>();
        int sumMin = 0, sumSnooze = 0;
        for (String r : recs) {
            final String[] f = r.split(",");
            final int m = Integer.parseInt(f[1]);
            final int s = Integer.parseInt(f[2]);
            data.add(new int[] {m, s});
            sumMin += m;
            sumSnooze += s;
        }
        final int n = data.size();
        final List<int[]> last = data.subList(Math.max(0, n - 14), n);

        final int primary = ThemeUtils.resolveColor(c, R.attr.colorPrimary);
        final int muted = ThemeUtils.resolveColor(c, R.attr.colorOnSurfaceVariant);
        final float d = c.getResources().getDisplayMetrics().density;

        final View chart = new View(c) {
            final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
            final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override
            protected void onDraw(Canvas cv) {
                bar.setColor(primary);
                text.setColor(muted);
                text.setTextSize(10 * d);
                text.setTextAlign(Paint.Align.CENTER);
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                for (int[] e : last) { min = Math.min(min, e[0]); max = Math.max(max, e[0]); }
                final float w = getWidth(), h = getHeight();
                final float top = 18 * d, bottom = h - 18 * d;
                final float slot = w / last.size();
                for (int i = 0; i < last.size(); i++) {
                    final int[] e = last.get(i);
                    final float frac = max == min ? 0.6f : 0.25f + 0.75f * (e[0] - min) / (float) (max - min);
                    final float x = slot * i + slot / 2;
                    final float y = bottom - (bottom - top) * frac;
                    cv.drawRoundRect(x - slot * 0.3f, y, x + slot * 0.3f, bottom, 6 * d, 6 * d, bar);
                    cv.drawText(hhmm(e[0]), x, y - 4 * d, text);
                    cv.drawText(e[1] > 0 ? e[1] + "z" : "", x, h - 4 * d, text);
                }
            }
        };
        chart.setMinimumHeight((int) (200 * d));

        final TextView summary = new TextView(c);
        summary.setTextColor(muted);
        summary.setTextSize(15);
        summary.setPadding(0, (int) (16 * d), 0, 0);
        summary.setText(String.format(Locale.getDefault(),
                "Average wake-up: %s\nAverage snoozes: %.1f\nDays tracked: %d",
                hhmm(sumMin / n), sumSnooze / (float) n, n));

        final LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (24 * d), (int) (16 * d), (int) (24 * d), 0);
        box.addView(chart, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (200 * d)));
        box.addView(summary);
        b.setView(box).show();
    }
}
