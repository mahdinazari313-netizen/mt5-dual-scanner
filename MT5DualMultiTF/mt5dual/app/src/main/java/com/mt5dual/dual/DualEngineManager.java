package com.mt5dual.dual;

import com.mt5dual.core.Signal;
import com.mt5dual.core.SignalStateManager;
import com.mt5dual.core.Timeframe;
import com.mt5dual.settings.SettingsProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Central multi-key Dual engine with a dedicated lock per Symbol+TF+Direction. */
public class DualEngineManager implements SignalStateManager.Listener {
    private final SignalStateManager signalStateManager;
    private final SettingsProvider settings;
    private final Map<SymbolTFKey, DualReference> references = new ConcurrentHashMap<>();
    private final Map<SymbolTFKey, DualTrigger> activeTriggers = new ConcurrentHashMap<>();
    private final Map<SymbolTFKey, Object> keyLocks = new ConcurrentHashMap<>();
    private final List<DualAlarmListener> globalAlarmListeners = new CopyOnWriteArrayList<>();

    public DualEngineManager(SignalStateManager signalStateManager, SettingsProvider settings) {
        this.signalStateManager = signalStateManager;
        this.settings = settings;
        this.signalStateManager.addListener(this);
    }

    @Override
    public void onSignalStateChanged(Signal incoming) {
        if (incoming == null) return;
        SymbolTFKey key = keyFor(incoming);
        Object lock = keyLocks.computeIfAbsent(key, k -> new Object());

        synchronized (lock) {
            DualReference ref = references.get(key);
            if (ref == null) {
                references.put(key, new DualReference(incoming));
                return;
            }

            Signal refSignal = ref.getSignal();
            if (settings.isPriceDifferenceEnabled() && refSignal.hasSamePrice(incoming)) {
                return;
            }

            long elapsed = com.mt5dual.core.WeekdayTimeUtil.weekdayMillisBetween(
                    refSignal.getReceivedAt(), incoming.getReceivedAt());
            long windowMillis = windowFor(incoming.getTimeframe());

            // Reference advances on every accepted event, even on a Dual FAIL.
            references.put(key, new DualReference(incoming));

            if (elapsed >= windowMillis) {
                // Existing trigger deliberately remains untouched on FAIL.
                return;
            }

            List<Signal> combination = new ArrayList<>(2);
            combination.add(refSignal);
            combination.add(incoming);

            DualTrigger newTrigger = new DualTrigger(
                    incoming.getSymbol(), incoming.getTimeframe(), incoming.getDirection(),
                    combination, incoming.getReceivedAt());

            DualTrigger oldTrigger = activeTriggers.put(key, newTrigger);
            if (oldTrigger != null) {
                fireTriggerExpired(oldTrigger);
            }
            fireNewTrigger(newTrigger);
        }
    }

    public void runPeriodicSafetyCheck() {
        runPeriodicSafetyCheck(System.currentTimeMillis());
    }

    public void runPeriodicSafetyCheck(long now) {
        for (Map.Entry<SymbolTFKey, DualTrigger> entry : activeTriggers.entrySet()) {
            SymbolTFKey key = entry.getKey();
            Object lock = keyLocks.computeIfAbsent(key, k -> new Object());

            synchronized (lock) {
                DualTrigger trigger = activeTriggers.get(key);
                if (trigger == null) continue;

                Signal reference = trigger.getCombination().get(0);
                long elapsed = com.mt5dual.core.WeekdayTimeUtil.weekdayMillisBetween(
                        reference.getReceivedAt(), now);
                long windowMillis = windowFor(trigger.getTimeframe());

                if (elapsed >= windowMillis) {
                    if (activeTriggers.remove(key, trigger)) {
                        fireTriggerExpired(trigger);
                    }
                    continue;
                }

                if (!trigger.isSilenced()) {
                    long repeatMillis = settings.getRepeatIntervalMinutes() * 60_000L;
                    if (now - trigger.getLastAlarmAt() >= repeatMillis) {
                        trigger.markAlarmFired(now);
                        fireRepeatAlarm(trigger);
                    }
                }
            }
        }
    }

    public void addGlobalAlarmListener(DualAlarmListener listener) {
        if (listener != null && !globalAlarmListeners.contains(listener)) {
            globalAlarmListeners.add(listener);
        }
    }

    public void removeGlobalAlarmListener(DualAlarmListener listener) {
        globalAlarmListeners.remove(listener);
    }

    public void silence(String symbol, Timeframe tf, com.mt5dual.core.Direction direction) {
        if (symbol == null || tf == null || direction == null) return;
        SymbolTFKey key = new SymbolTFKey(symbol, tf, direction);
        Object lock = keyLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            DualTrigger trigger = activeTriggers.get(key);
            if (trigger != null) trigger.silence();
        }
    }

    public List<DualTrigger> getAllActiveTriggers() {
        List<DualTrigger> result = new ArrayList<>(activeTriggers.values());
        result.sort(Comparator.comparingLong(DualTrigger::getCreatedAt).reversed());
        return result;
    }

    public List<Signal> getAllActiveSignals() {
        return signalStateManager.getAllSignals();
    }

    public DualTrigger getActiveTrigger(String symbol, Timeframe tf, com.mt5dual.core.Direction direction) {
        return activeTriggers.get(new SymbolTFKey(symbol, tf, direction));
    }

    private SymbolTFKey keyFor(Signal signal) {
        return new SymbolTFKey(signal.getSymbol(), signal.getTimeframe(), signal.getDirection());
    }

    private long windowFor(Timeframe timeframe) {
        return timeframe.getBaseMinutes() * settings.getDualWindowCandles() * 60_000L;
    }

    private void fireNewTrigger(DualTrigger trigger) {
        for (DualAlarmListener listener : globalAlarmListeners) listener.onNewTrigger(trigger);
    }

    private void fireRepeatAlarm(DualTrigger trigger) {
        for (DualAlarmListener listener : globalAlarmListeners) listener.onRepeatAlarm(trigger);
    }

    private void fireTriggerExpired(DualTrigger trigger) {
        for (DualAlarmListener listener : globalAlarmListeners) listener.onTriggerExpired(trigger);
    }
}
