package com.android.deskclock;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** One-line current weather for the Clock tab (Open-Meteo, approximate location). */
public class WeatherView extends AppCompatTextView {

    private static final long MAX_AGE = 30 * 60 * 1000L;
    private final Handler mMain = new Handler(Looper.getMainLooper());

    public WeatherView(Context c) { this(c, null); }

    public WeatherView(Context c, AttributeSet a) {
        super(c, a);
        setOnClickListener(v -> onTap());
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    private boolean hasPermission() {
        return getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh(false);
    }

    private void onTap() {
        if (!hasPermission() && getContext() instanceof Activity) {
            ((Activity) getContext()).requestPermissions(
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 7302);
            // Check back a few times while the user answers the prompt.
            for (int i = 1; i <= 6; i++) mMain.postDelayed(() -> { if (hasPermission()) refresh(true); }, i * 2000L);
        } else {
            refresh(true);
        }
    }

    private void refresh(boolean force) {
        final String cached = prefs().getString("text", null);
        if (cached != null) setText(cached);
        if (!hasPermission()) {
            if (cached == null) setText(getContext().getString(com.android.deskclock.R.string.cl_tap_weather));
            return;
        }
        if (!force && cached != null
                && System.currentTimeMillis() - prefs().getLong("time", 0) < MAX_AGE) return;
        if (cached == null) setText(getContext().getString(com.android.deskclock.R.string.cl_loading_weather));
        final Location loc = lastLocation();
        if (loc != null) {
            fetch(loc);
        } else if (Build.VERSION.SDK_INT >= 30) {
            try {
                final LocationManager lm = getContext().getSystemService(LocationManager.class);
                lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null,
                        getContext().getMainExecutor(), l -> {
                            if (l != null) fetch(l); else if (cached == null) setText(getContext().getString(com.android.deskclock.R.string.cl_weather_unavailable));
                        });
            } catch (Exception e) {
                if (cached == null) setText(getContext().getString(com.android.deskclock.R.string.cl_weather_unavailable));
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private Location lastLocation() {
        try {
            final LocationManager lm = getContext().getSystemService(LocationManager.class);
            Location best = null;
            for (String p : lm.getProviders(true)) {
                final Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    private void fetch(Location loc) {
        new Thread(() -> {
            try {
                final String url = String.format(Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f"
                        + "&current=temperature_2m,weather_code,is_day"
                        + "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1",
                        loc.getLatitude(), loc.getLongitude());
                final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                final StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                final JSONObject j = new JSONObject(sb.toString());
                final JSONObject cur = j.getJSONObject("current");
                final JSONObject daily = j.getJSONObject("daily");
                final int code = cur.getInt("weather_code");
                final boolean day = cur.optInt("is_day", 1) == 1;
                final String text = String.format(Locale.getDefault(), "%s %d°C · %s · H %d° L %d°",
                        icon(code, day), Math.round(cur.getDouble("temperature_2m")), describe(code),
                        Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)),
                        Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)));
                prefs().edit().putString("text", text).putLong("time", System.currentTimeMillis()).apply();
                mMain.post(() -> setText(text));
            } catch (Exception e) {
                mMain.post(() -> { if (prefs().getString("text", null) == null) setText(getContext().getString(com.android.deskclock.R.string.cl_weather_unavailable)); });
            }
        }).start();
    }

    private static String icon(int c, boolean day) {
        if (c == 0) return day ? "☀️" : "🌙";
        if (c <= 2) return day ? "⛅" : "☁️";
        if (c == 3) return "☁️";
        if (c == 45 || c == 48) return "🌫️";
        if (c >= 51 && c <= 57) return "🌦️";
        if (c >= 61 && c <= 67) return "🌧️";
        if (c >= 71 && c <= 77) return "🌨️";
        if (c >= 80 && c <= 82) return "🌦️";
        if (c == 85 || c == 86) return "🌨️";
        if (c >= 95) return "⛈️";
        return "🌡️";
    }

    private String describe(int c) {
        if (c == 0) return getContext().getString(com.android.deskclock.R.string.cl_w_clear);
        if (c <= 2) return getContext().getString(com.android.deskclock.R.string.cl_w_partly_cloudy);
        if (c == 3) return getContext().getString(com.android.deskclock.R.string.cl_w_cloudy);
        if (c == 45 || c == 48) return getContext().getString(com.android.deskclock.R.string.cl_w_fog);
        if (c >= 51 && c <= 57) return getContext().getString(com.android.deskclock.R.string.cl_w_drizzle);
        if (c >= 61 && c <= 67) return getContext().getString(com.android.deskclock.R.string.cl_w_rain);
        if (c >= 71 && c <= 77) return getContext().getString(com.android.deskclock.R.string.cl_w_snow);
        if (c >= 80 && c <= 82) return getContext().getString(com.android.deskclock.R.string.cl_w_showers);
        if (c == 85 || c == 86) return getContext().getString(com.android.deskclock.R.string.cl_w_snow_showers);
        if (c >= 95) return getContext().getString(com.android.deskclock.R.string.cl_w_thunder);
        return getContext().getString(com.android.deskclock.R.string.cl_w_generic);
    }
}
