package org.example.process;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fixed right-open history ranges used by the one-time Goods Bootstrap. */
public final class GoodsBootstrapConfig {
    private GoodsBootstrapConfig() {
    }

    public static Map<Integer, Long> orderEndOffsets() {
        Map<Integer, Long> offsets = new LinkedHashMap<>();
        offsets.put(0, value("goods.bootstrap.order.p0.end", 20015L));
        offsets.put(1, value("goods.bootstrap.order.p1.end", 0L));
        offsets.put(2, value("goods.bootstrap.order.p2.end", 0L));
        return Collections.unmodifiableMap(offsets);
    }

    public static Map<Integer, Long> detailEndOffsets() {
        Map<Integer, Long> offsets = new LinkedHashMap<>();
        offsets.put(0, value("goods.bootstrap.detail.p0.end", 30638L));
        offsets.put(1, value("goods.bootstrap.detail.p1.end", 0L));
        offsets.put(2, value("goods.bootstrap.detail.p2.end", 0L));
        return Collections.unmodifiableMap(offsets);
    }

    private static long value(String key, long defaultValue) {
        String configured = System.getProperty(key);
        if (configured == null || configured.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(configured.trim());
            if (parsed < 0L) {
                throw new IllegalArgumentException(key + " must be >= 0");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid long system property " + key + "=" + configured,
                    e
            );
        }
    }
}
