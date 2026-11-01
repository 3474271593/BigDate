package org.example.process;

import java.util.Locale;

/** Runtime phase for the unchanged Goods streaming topology. */
public enum GoodsRuntimeMode {
    WARMUP,
    REALTIME;

    public static final String SYSTEM_PROPERTY = "goods.runtime.mode";

    public static GoodsRuntimeMode fromSystemProperty() {
        return parse(System.getProperty(SYSTEM_PROPERTY));
    }

    public static GoodsRuntimeMode parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required JVM property -D"
                            + SYSTEM_PROPERTY
                            + "=WARMUP|REALTIME"
            );
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid " + SYSTEM_PROPERTY + "=" + value
                            + "; expected WARMUP or REALTIME",
                    e
            );
        }
    }

    public String validateRestorePath(String value) {
        String path = value == null ? null : value.trim();
        boolean missing = path == null || path.isEmpty();

        if (this == WARMUP) {
            if (!missing) {
                throw new IllegalArgumentException(
                        "WARMUP must start without -Dflink.restore.path"
                );
            }
            return null;
        }

        if (missing) {
            throw new IllegalArgumentException(
                    "REALTIME must restore the WARMUP checkpoint; "
                            + "set -Dflink.restore.path=<checkpoint-uri>"
            );
        }
        return path;
    }
}
