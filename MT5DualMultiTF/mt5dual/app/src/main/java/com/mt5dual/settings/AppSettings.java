package com.mt5dual.settings;

import android.content.Context;

public class AppSettings {
    private final SettingsProvider provider;

    public AppSettings(Context context) {
        provider = new SettingsProvider(context);
    }

    public int getDualWindowCandles() { return provider.getDualWindowCandles(); }
    public void setDualWindowCandles(int value) { provider.setDualWindowCandles(value); }

    public int getRepeatIntervalMinutes() { return provider.getRepeatIntervalMinutes(); }
    public void setRepeatIntervalMinutes(int value) { provider.setRepeatIntervalMinutes(value); }

    public boolean isPriceDifferenceEnabled() { return provider.isPriceDifferenceEnabled(); }
    public void setPriceDifferenceEnabled(boolean enabled) { provider.setPriceDifferenceEnabled(enabled); }

    public boolean isMonitoringEnabled() { return provider.isMonitoringEnabled(); }
    public void setMonitoringEnabled(boolean enabled) { provider.setMonitoringEnabled(enabled); }
}
