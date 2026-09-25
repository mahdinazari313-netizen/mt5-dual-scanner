package com.mt5dual.dual;

public interface DualAlarmListener {
    void onNewTrigger(DualTrigger trigger);
    void onRepeatAlarm(DualTrigger trigger);
    void onTriggerExpired(DualTrigger trigger);
}
