package com.mt5dual.dual;

import com.mt5dual.core.Direction;
import com.mt5dual.core.Timeframe;

import java.util.Objects;

public final class SymbolTFKey {
    private final String symbol;
    private final Timeframe timeframe;
    private final Direction direction;

    public SymbolTFKey(String symbol, Timeframe timeframe, Direction direction) {
        if (symbol == null || symbol.trim().isEmpty() || timeframe == null || direction == null) {
            throw new IllegalArgumentException("symbol, timeframe and direction are required");
        }
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
    }

    public String getSymbol() { return symbol; }
    public Timeframe getTimeframe() { return timeframe; }
    public Direction getDirection() { return direction; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolTFKey)) return false;
        SymbolTFKey that = (SymbolTFKey) o;
        return symbol.equals(that.symbol)
                && timeframe == that.timeframe
                && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timeframe, direction);
    }

    @Override
    public String toString() {
        return symbol + "|" + timeframe + "|" + direction;
    }
}
