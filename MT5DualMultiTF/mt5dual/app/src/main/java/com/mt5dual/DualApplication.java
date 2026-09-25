package com.mt5dual;

import android.app.Application;

import com.mt5dual.alarm.AlarmNotifier;
import com.mt5dual.core.SignalStateManager;
import com.mt5dual.dual.DualEngineManager;
import com.mt5dual.settings.SettingsProvider;

import java.util.concurrent.atomic.AtomicBoolean;

public class DualApplication extends Application {
    private static DualApplication instance;
    private final AtomicBoolean alarmNotifierRegistered = new AtomicBoolean(false);

    private SignalStateManager signalStateManager;
    private SettingsProvider settingsProvider;
    private DualEngineManager dualEngineManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        settingsProvider = new SettingsProvider(this);
        signalStateManager = new SignalStateManager();
        dualEngineManager = new DualEngineManager(signalStateManager, settingsProvider);
        ensureAlarmNotifierRegistered();
    }

    public static DualApplication getInstance() {
        if (instance == null) throw new IllegalStateException("DualApplication is not initialized");
        return instance;
    }

    public SignalStateManager getSignalStateManager() { return signalStateManager; }
    public SettingsProvider getSettingsProvider() { return settingsProvider; }
    public DualEngineManager getDualEngineManager() { return dualEngineManager; }

    public void ensureAlarmNotifierRegistered() {
        if (alarmNotifierRegistered.compareAndSet(false, true)) {
            dualEngineManager.addGlobalAlarmListener(new AlarmNotifier(this));
        }
    }
}
