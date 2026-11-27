package com.android.deskclock.alarms;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;

/** Optional calendar date for one-time alarms, stored per alarm id. */
public final class AlarmDates {

    private AlarmDates() { }

    private static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext()
                .getSharedPreferences("alarm_dates", Context.MODE_PRIVATE);
    }

    public static long get(Context c, long alarmId) {
        return prefs(c).getLong("a" + alarmId, 0);
    }

    /** Moves the instance to the chosen date if one is set and it's later than scheduled. */
    public static void apply(Context c, AlarmInstance inst) { applyStatus(c, inst); }

    public static String applyStatus(Context c, AlarmInstance inst) {
        try {
            if (inst == null || inst.mAlarmId == null) return "no instance";
            final long d = get(c, inst.mAlarmId);
            if (d == 0) return "no date saved";
            final Alarm alarm = Alarm.getAlarm(c.getContentResolver(), inst.mAlarmId);
            if (alarm == null || alarm.daysOfWeek.isRepeating()) return "alarm missing or repeating";

            final Calendar target = Calendar.getInstance();
            target.setTimeInMillis(d);
            target.set(Calendar.HOUR_OF_DAY, inst.mHour);
            target.set(Calendar.MINUTE, inst.mMinute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            if (target.after(inst.getAlarmTime()) && target.after(Calendar.getInstance())) {
                inst.mYear = target.get(Calendar.YEAR);
                inst.mMonth = target.get(Calendar.MONTH);
                inst.mDay = target.get(Calendar.DAY_OF_MONTH);
                AlarmInstance.updateInstance(c.getContentResolver(), inst);
                return "moved";
            }
            return "not moved: target " + target.getTime() + " vs " + inst.getAlarmTime().getTime();
        } catch (Exception e) {
            return "error: " + e;
        }
    }

    /** Shows a date picker for a one-time alarm. */
    public static void pick(Context c, Alarm alarm, AlarmInstance inst) {
        pick(c, alarm, inst, null);
    }

    public static void pick(Context c, Alarm alarm, AlarmInstance inst, Runnable after) {
        if (alarm == null) return;
        if (alarm.daysOfWeek.isRepeating()) {
            Toast.makeText(c, c.getString(com.android.deskclock.R.string.cl_dates_one_time),
                    Toast.LENGTH_LONG).show();
            return;
        }
        final Calendar start = Calendar.getInstance();
        final long cur = get(c, alarm.id);
        if (cur > 0) start.setTimeInMillis(cur);

        final androidx.fragment.app.FragmentActivity act = activityOf(c);
        if (act == null) return;
        final java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");
        final Calendar initUtc = Calendar.getInstance(utc);
        initUtc.clear();
        initUtc.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
        final Calendar now = Calendar.getInstance();
        final Calendar todayUtc = Calendar.getInstance(utc);
        todayUtc.clear();
        todayUtc.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        final com.google.android.material.datepicker.MaterialDatePicker.Builder<Long> builder =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTitleText(c.getString(com.android.deskclock.R.string.cl_pick_date))
                        .setSelection(initUtc.getTimeInMillis())
                        .setCalendarConstraints(
                                new com.google.android.material.datepicker.CalendarConstraints.Builder()
                                        .setStart(todayUtc.getTimeInMillis())
                                        .setValidator(com.google.android.material.datepicker
                                                .DateValidatorPointForward.from(todayUtc.getTimeInMillis()))
                                        .build());
        if (cur > 0) builder.setNegativeButtonText(c.getString(com.android.deskclock.R.string.cl_clear));
        final com.google.android.material.datepicker.MaterialDatePicker<Long> picker = builder.build();

        picker.addOnPositiveButtonClickListener(sel -> {
            final Calendar u = Calendar.getInstance(utc);
            u.setTimeInMillis(sel);
            final Calendar day = Calendar.getInstance();
            day.clear();
            day.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH));
            prefs(c).edit().putLong("a" + alarm.id, day.getTimeInMillis()).apply();
            if (inst != null) {
                applyStatus(c, inst);
                try {
                    AlarmStateManager.registerInstance(c, inst, true);
                } catch (Exception ignored) { }
            }
            Toast.makeText(c, c.getString(com.android.deskclock.R.string.cl_alarm_set_for)
                    + DateFormat.format("EEE d MMM", day)
                    + (inst == null ? c.getString(com.android.deskclock.R.string.cl_turn_on_to_activate) : ""),
                    Toast.LENGTH_LONG).show();
            if (after != null) after.run();
        });
        if (cur > 0) {
            picker.addOnNegativeButtonClickListener(v -> {
                prefs(c).edit().remove("a" + alarm.id).apply();
                Toast.makeText(c, c.getString(com.android.deskclock.R.string.cl_date_cleared), Toast.LENGTH_LONG).show();
                if (after != null) after.run();
            });
        }
        picker.show(act.getSupportFragmentManager(), "alarm_date");
    }
    /** "Tomorrow" or "Sat 28 Nov" for a dated one-time alarm; otherwise the fallback text. */
    public static String label(Context c, Alarm alarm, String fallback) {
        try {
            final long d = get(c, alarm.id);
            if (d == 0 || alarm.daysOfWeek.isRepeating()) return fallback;
            final Calendar sel = Calendar.getInstance();
            sel.setTimeInMillis(d);
            final Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            if (!sel.after(today)) return fallback;
            final Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            if (sel.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR)
                    && sel.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
                return c.getString(com.android.deskclock.R.string.alarm_tomorrow);
            }
            return DateFormat.format("EEE d MMM", sel).toString();
        } catch (Exception e) {
            return fallback;
        }
    }
    private static androidx.fragment.app.FragmentActivity activityOf(Context c) {
        while (c instanceof android.content.ContextWrapper) {
            if (c instanceof androidx.fragment.app.FragmentActivity) {
                return (androidx.fragment.app.FragmentActivity) c;
            }
            c = ((android.content.ContextWrapper) c).getBaseContext();
        }
        return null;
    }
}
