package com.android.deskclock.alarms;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import androidx.core.app.NotificationCompat;

import com.android.deskclock.DeskClock;
import com.android.deskclock.R;

import java.util.Calendar;
import java.util.Locale;

/** Nightly "time for bed" reminder. */
public final class Bedtime {

    private static final String CHANNEL = "bedtime_popup";
    private static final int NOTIF_ID = 7301;

    private Bedtime() { }

    private static SharedPreferences[] prefs(Context c) {
        return new SharedPreferences[] {
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(
                        c.createDeviceProtectedStorageContext()),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(c)};
    }

    private static boolean enabled(Context c) {
        for (SharedPreferences p : prefs(c)) {
            try { if (p.getBoolean("bedtime_enabled", false)) return true; } catch (Exception ignored) { }
        }
        return false;
    }

    private static String time(Context c) {
        for (SharedPreferences p : prefs(c)) {
            try {
                final String t = p.getString("bedtime_time", null);
                if (t != null) return t;
            } catch (Exception ignored) { }
        }
        return "22:30";
    }

    private static PendingIntent pending(Context c) {
        return PendingIntent.getBroadcast(c, 7301, new Intent(c, Receiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Schedules (or cancels) the next reminder. Safe to call any time. */
    public static void schedule(Context c) { scheduleStatus(c); }

    public static String scheduleStatus(Context c) {
        try {
            final AlarmManager am = c.getSystemService(AlarmManager.class);
            if (am == null) return "no alarm manager";
            am.cancel(pending(c));
            if (!enabled(c)) return "reminder is off";
            final String[] hm = time(c).split(":");
            final Calendar next = Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
            next.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);
            if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending(c));
            return "Bedtime reminder set for " + DateFormat.format("EEE HH:mm", next);
        } catch (Exception e) {
            return "error: " + e;
        }
    }

    static void notifyNow(Context c) {
        final NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.deleteNotificationChannel("bedtime");
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Bedtime",
                NotificationManager.IMPORTANCE_HIGH));

        String text = "Time to wind down.";
        final AlarmManager am = c.getSystemService(AlarmManager.class);
        final AlarmManager.AlarmClockInfo next = am == null ? null : am.getNextAlarmClock();
        if (next != null) {
            final long mins = (next.getTriggerTime() - System.currentTimeMillis()) / 60000;
            if (mins > 0 && mins < 24 * 60) {
                final Calendar t = Calendar.getInstance();
                t.setTimeInMillis(next.getTriggerTime());
                final String when = DateFormat.format(
                        DateFormat.is24HourFormat(c) ? "EEE HH:mm" : "EEE h:mm a", t).toString();
                text = String.format(Locale.getDefault(),
                        "Next alarm %s, so you'd get %d h %d min of sleep.", when, mins / 60, mins % 60);
            }
        }
        final PendingIntent open = PendingIntent.getActivity(c, 0,
                new Intent(c, DeskClock.class), PendingIntent.FLAG_IMMUTABLE);
        nm.notify(NOTIF_ID, new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setContentTitle("Time for bed")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build());
    }

    /** Fires at bedtime: shows the reminder and schedules tomorrow's. */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            if (enabled(c)) notifyNow(c);
            schedule(c);
        }
    }
}
