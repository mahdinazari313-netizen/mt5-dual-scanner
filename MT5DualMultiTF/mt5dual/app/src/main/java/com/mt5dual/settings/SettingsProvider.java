package com.mt5dual.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsProvider {
    public static final String PREF_NAME = "mt5dual_settings";
    public static final String KEY_DUAL_WINDOW_CANDLES = "dual_window_candles";
    public static final String KEY_REPEAT_INTERVAL_MINUTES = "repeat_interval_minutes";
    public static final String KEY_PRICE_DIFFERENCE_ENABLED = "price_difference_enabled";
    public static final String KEY_MONITORING_ENABLED = "monitoring_enabled";

    private static final int DEFAULT_DUAL_WINDOW_CANDLES = 30;
    private static final int DEFAULT_REPEAT_INTERVAL_MINUTES = 10;
    private static final boolean DEFAULT_PRICE_DIFFERENCE_ENABLED = true;
    private static final boolean DEFAULT_MONITORING_ENABLED = true;

    private final SharedPreferences preferences;

    public SettingsProvider(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getDualWindowCandles() {
        return Math.max(2, preferences.getInt(KEY_DUAL_WINDOW_CANDLES, DEFAULT_DUAL_WINDOW_CANDLES));
    }

    public void setDualWindowCandles(int value) {
        if (value < 2) throw new IllegalArgumentException("Dual Window Candles must be >= 2");
        preferences.edit().putInt(KEY_DUAL_WINDOW_CANDLES, value).apply();
    }

    public int getRepeatIntervalMinutes() {
        return Math.max(1, preferences.getInt(KEY_REPEAT_INTERVAL_MINUTES, DEFAULT_REPEAT_INTERVAL_MINUTES));
    }

    public void setRepeatIntervalMinutes(int value) {
        if (value < 1) throw new IllegalArgumentException("Repeat Interval must be >= 1");
        preferences.edit().putInt(KEY_REPEAT_INTERVAL_MINUTES, value).apply();
    }

    public boolean isPriceDifferenceEnabled() {
        return preferences.getBoolean(KEY_PRICE_DIFFERENCE_ENABLED, DEFAULT_PRICE_DIFFERENCE_ENABLED);
    }

    public void setPriceDifferenceEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PRICE_DIFFERENCE_ENABLED, enabled).apply();
    }

    public boolean isMonitoringEnabled() {
        return preferences.getBoolean(KEY_MONITORING_ENABLED, DEFAULT_MONITORING_ENABLED);
    }

    public void setMonitoringEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply();
    }
}
