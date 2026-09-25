package com.mt5dual.core;

public interface SignalParser {
    RawParsedSignal tryParse(String notificationText);
}
