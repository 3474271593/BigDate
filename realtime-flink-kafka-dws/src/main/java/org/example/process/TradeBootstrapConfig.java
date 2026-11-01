package org.example.process;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Configuration for this Trade bootstrap run; every default can be overridden. */
public final class TradeBootstrapConfig {

    private TradeBootstrapConfig() {
    }

    public static Map<Integer, Long> historyEndOffsets() {
        Map<Integer, Long> offsets = new HashMap<>();
        offsets.put(0, longProperty("trade.bootstrap.p0.end", 20015L));
        offsets.put(1, longProperty("trade.bootstrap.p1.end", 0L));
        offsets.put(2, longProperty("trade.bootstrap.p2.end", 0L));
        return Collections.unmodifiableMap(offsets);
    }

    public static long expectedRecordCount() {
        return longProperty("trade.bootstrap.expected.records", 20015L);
    }

    public static int expectedUniqueOrderCount() {
        return (int) longProperty("trade.bootstrap.expected.unique-orders", 20015L);
    }

    public static long expectedBootstrapCount() {
        return longProperty("trade.bootstrap.expected.bootstrap", 20015L);
    }

    public static long expectedUpdateCount() {
        return longProperty("trade.bootstrap.expected.updates", 0L);
    }

    private static long longProperty(String name, long defaultValue) {
        String value = System.getProperty(name);
        return value == null || value.trim().isEmpty()
                ? defaultValue : Long.parseLong(value.trim());
    }
}
