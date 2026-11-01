package org.example.process;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shop 历史 Bootstrap 配置。
 *
 * 当前历史边界：
 *
 * dwd_order_info-0: [0, 20028)
 * dwd_order_info-1: [0, 0)
 * dwd_order_info-2: [0, 0)
 *
 * 注意：
 * endOffset 使用 Kafka 的右开区间语义。
 * 例如 partition 0 的 endOffset=20028，
 * 表示历史数据包含 offset 0~20027，
 * offset >= 20028 属于实时阶段。
 */
public final class ShopBootstrapConfig {

    private static final long DEFAULT_PARTITION_0_END_OFFSET = 20015L;
    private static final long DEFAULT_PARTITION_1_END_OFFSET = 0L;
    private static final long DEFAULT_PARTITION_2_END_OFFSET = 0L;

    private ShopBootstrapConfig() {
    }

    /**
     * 获取 Shop Bootstrap 的历史结束 Offset。
     *
     * 可以通过 JVM System Property 覆盖：
     *
     * -Dshop.bootstrap.partition.0.end.offset=20028
     * -Dshop.bootstrap.partition.1.end.offset=0
     * -Dshop.bootstrap.partition.2.end.offset=0
     */
    public static Map<Integer, Long> getEndOffsets() {
        Map<Integer, Long> offsets = new HashMap<>();

        offsets.put(
                0,
                getLongProperty(
                        "shop.bootstrap.partition.0.end.offset",
                        DEFAULT_PARTITION_0_END_OFFSET
                )
        );

        offsets.put(
                1,
                getLongProperty(
                        "shop.bootstrap.partition.1.end.offset",
                        DEFAULT_PARTITION_1_END_OFFSET
                )
        );

        offsets.put(
                2,
                getLongProperty(
                        "shop.bootstrap.partition.2.end.offset",
                        DEFAULT_PARTITION_2_END_OFFSET
                )
        );

        return Collections.unmodifiableMap(offsets);
    }

    private static long getLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid long system property: "
                            + key
                            + "="
                            + value,
                    e
            );
        }
    }
}
