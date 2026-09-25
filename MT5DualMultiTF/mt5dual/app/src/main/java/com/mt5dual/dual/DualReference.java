package com.mt5dual.dual;

import com.mt5dual.core.Signal;

/** Immutable reference wrapper. */
public final class DualReference {
    private final Signal signal;

    public DualReference(Signal signal) {
        if (signal == null) {
            throw new IllegalArgumentException("signal cannot be null");
        }
        this.signal = signal;
    }

    public Signal getSignal() {
        return signal;
    }
}
