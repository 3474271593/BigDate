// 文件路径：src/main/java/org/example/process/UserBootstrapConfig.java
package org.example.process;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User 历史 Bootstrap 配置。
 *
 * 当前历史边界：
 *
 * dwd_order_info-0: [0, 20030)
 * dwd_order_info-1: [0, 0)
 * dwd_order_info-2: [0, 0)
 *
 * 注意：
 * endOffset 使用 Kafka 的右开区间语义。
 *
 * partition 0 的 endOffset = 20030，
 * 表示历史数据包含 offset 0 ~ 20029。
 *
 * offset >= 20030 属于 User 实时阶段。
 *
 * 该边界是 User V1 的固定历史/实时分界，
 * 后续 Kafka end offset 增长时不能跟着修改。
 */
public final class UserBootstrapConfig {

    private static final long DEFAULT_PARTITION_0_END_OFFSET = 20015L;
    private static final long DEFAULT_PARTITION_1_END_OFFSET = 0L;
    private static final long DEFAULT_PARTITION_2_END_OFFSET = 0L;

    private UserBootstrapConfig() {
    }

    /**
     * 获取 User Bootstrap 历史结束 Offset。
     *
     * 可以通过 JVM System Property 覆盖：
     *
     * -Duser.bootstrap.partition.0.end.offset=20030
     * -Duser.bootstrap.partition.1.end.offset=0
     * -Duser.bootstrap.partition.2.end.offset=0
     */
    public static Map<Integer, Long> getEndOffsets() {

        Map<Integer, Long> offsets =
                new HashMap<>();

        offsets.put(
                0,
                getLongProperty(
                        "user.bootstrap.partition.0.end.offset",
                        DEFAULT_PARTITION_0_END_OFFSET
                )
        );

        offsets.put(
                1,
                getLongProperty(
                        "user.bootstrap.partition.1.end.offset",
                        DEFAULT_PARTITION_1_END_OFFSET
                )
        );

        offsets.put(
                2,
                getLongProperty(
                        "user.bootstrap.partition.2.end.offset",
                        DEFAULT_PARTITION_2_END_OFFSET
                )
        );

        return Collections.unmodifiableMap(
                offsets
        );
    }

    /**
     * 读取 long 类型 JVM System Property。
     *
     * 如果没有配置，则使用默认值。
     */
    private static long getLongProperty(
            String key,
            long defaultValue) {

        String value =
                System.getProperty(key);

        if (value == null
                || value.trim().isEmpty()) {

            return defaultValue;
        }

        try {

            return Long.parseLong(
                    value.trim()
            );

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
