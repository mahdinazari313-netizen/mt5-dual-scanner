package com.mt5dual.dual;

import com.mt5dual.core.Direction;
import com.mt5dual.core.Signal;
import com.mt5dual.core.Timeframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DualTrigger {
    private final String id;
    private final String symbol;
    private final Timeframe timeframe;
    private final Direction direction;
    private final List<Signal> combination;
    private final long createdAt;
    private volatile boolean silenced;
    private volatile long lastAlarmAt;

    public DualTrigger(String symbol,
                       Timeframe timeframe,
                       Direction direction,
                       List<Signal> combination,
                       long createdAt) {
        if (symbol == null || timeframe == null || direction == null || combination == null
                || combination.size() != 2) {
            throw new IllegalArgumentException("A DualTrigger requires exactly two signals");
        }
        this.id = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
        this.combination = Collections.unmodifiableList(new ArrayList<>(combination));
        this.createdAt = createdAt;
        this.silenced = false;
        // The initial notification is considered an alarm event. Repeat begins after the interval.
        this.lastAlarmAt = createdAt;
    }

    public String getId() { return id; }
    public String getSymbol() { return symbol; }
    public Timeframe getTimeframe() { return timeframe; }
    public Direction getDirection() { return direction; }
    public List<Signal> getCombination() { return combination; }
    public long getCreatedAt() { return createdAt; }
    public boolean isSilenced() { return silenced; }
    public long getLastAlarmAt() { return lastAlarmAt; }

    public void silence() {
        silenced = true;
    }

    public void markAlarmFired(long now) {
        lastAlarmAt = now;
    }
}
