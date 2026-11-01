// 文件路径：src/main/java/org/example/process/UserBootstrapHistoryReader.java
package org.example.process;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.bean.DwdOrderInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User Bootstrap 历史数据只读工具。
 *
 * 从 dwd_order_info 的 offset 0 开始读取，
 * 一直读取到 UserBootstrapConfig 中配置的历史边界。
 *
 * 当前 User 正式历史边界：
 *
 * P0: [0, 20030)
 * P1: [0, 0)
 * P2: [0, 0)
 *
 * 本类：
 *
 * - 不提交 Kafka Offset；
 * - 不写任何 Kafka Topic；
 * - 不修改 Flink Checkpoint；
 * - 只负责读取历史 DWD OrderInfo。
 */
public final class UserBootstrapHistoryReader {

    private static final String BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,"
                    + "192.168.13.133:9092,"
                    + "192.168.13.131:9092";

    private static final String INPUT_TOPIC =
            "dwd_order_info";

    private UserBootstrapHistoryReader() {
    }

    /**
     * 读取 User Bootstrap 固定历史区间。
     *
     * @return 历史 DwdOrderInfo
     */
    public static List<DwdOrderInfo> readHistory() {

        Map<Integer, Long> endOffsets =
                UserBootstrapConfig.getEndOffsets();

        Properties properties =
                new Properties();

        properties.setProperty(
                "bootstrap.servers",
                BOOTSTRAP_SERVERS
        );

        /*
         * Bootstrap 使用独立只读 group。
         *
         * 实际读取通过 assign + seek 控制，
         * 不依赖 group 已提交 offset。
         */
        properties.setProperty(
                "group.id",
                "realtime-dws-user-bootstrap-read-only"
        );

        properties.setProperty(
                "enable.auto.commit",
                "false"
        );

        properties.setProperty(
                "auto.offset.reset",
                "none"
        );

        properties.setProperty(
                "key.deserializer",
                StringDeserializer.class.getName()
        );

        properties.setProperty(
                "value.deserializer",
                StringDeserializer.class.getName()
        );

        List<DwdOrderInfo> orders =
                new ArrayList<>();

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            /*
             * ============================================================
             * 1. 检查 Topic 分区数量
             * ============================================================
             */

            List<PartitionInfo> metadata =
                    consumer.partitionsFor(INPUT_TOPIC);

            if (metadata.size() != endOffsets.size()) {

                throw new IllegalStateException(
                        "Expected "
                                + endOffsets.size()
                                + " partitions but found "
                                + metadata.size()
                );
            }

            List<TopicPartition> allPartitions =
                    new ArrayList<>();

            List<TopicPartition> activePartitions =
                    new ArrayList<>();

            for (Integer partition :
                    endOffsets.keySet()) {

                TopicPartition topicPartition =
                        new TopicPartition(
                                INPUT_TOPIC,
                                partition
                        );

                allPartitions.add(
                        topicPartition
                );

                /*
                 * endOffset = 0 的分区没有历史数据需要读取。
                 */
                if (endOffsets.get(partition) > 0L) {

                    activePartitions.add(
                            topicPartition
                    );
                }
            }

            /*
             * ============================================================
             * 2. 校验历史起点和当前 End Offset
             * ============================================================
             *
             * Bootstrap 要求：
             *
             * beginning offset 必须仍然是 0，
             * 并且 Kafka 当前 end offset
             * 不能小于我们已经冻结的历史边界。
             */

            Map<TopicPartition, Long> beginnings =
                    consumer.beginningOffsets(
                            allPartitions
                    );

            Map<TopicPartition, Long> currentEnds =
                    consumer.endOffsets(
                            allPartitions
                    );

            for (TopicPartition partition :
                    allPartitions) {

                long configuredEnd =
                        endOffsets.get(
                                partition.partition()
                        );

                Long beginning =
                        beginnings.get(
                                partition
                        );

                Long currentEnd =
                        currentEnds.get(
                                partition
                        );

                if (beginning == null
                        || beginning != 0L) {

                    throw new IllegalStateException(
                            "History no longer begins "
                                    + "at offset 0 for "
                                    + partition
                                    + ", beginning="
                                    + beginning
                    );
                }

                if (currentEnd == null
                        || currentEnd < configuredEnd) {

                    throw new IllegalStateException(
                            "Incomplete history for "
                                    + partition
                                    + ": currentEnd="
                                    + currentEnd
                                    + ", requiredEnd="
                                    + configuredEnd
                    );
                }
            }

            /*
             * 没有需要读取的分区时直接返回。
             */
            if (activePartitions.isEmpty()) {

                return orders;
            }

            /*
             * ============================================================
             * 3. 手动 assign + seek(0)
             * ============================================================
             *
             * Bootstrap 不加入正常实时 Consumer Group。
             *
             * 每次执行都严格从 offset 0
             * 开始读取冻结的历史区间。
             */

            consumer.assign(
                    activePartitions
            );

            /*
             * 记录每个 active partition
             * 下一条应该读取到的 offset。
             */
            Map<TopicPartition, Long> nextOffsets =
                    new HashMap<>();

            for (TopicPartition partition :
                    activePartitions) {

                consumer.seek(
                        partition,
                        0L
                );

                nextOffsets.put(
                        partition,
                        0L
                );
            }

            long lastProgressAt =
                    System.currentTimeMillis();

            /*
             * ============================================================
             * 4. 一直读取到配置好的 User 历史边界
             * ============================================================
             */

            while (!complete(
                    nextOffsets,
                    endOffsets)) {

                ConsumerRecords<String, String> polled =
                        consumer.poll(
                                Duration.ofSeconds(1)
                        );

                boolean progressed =
                        false;

                for (ConsumerRecord<String, String> record :
                        polled) {

                    TopicPartition partition =
                            new TopicPartition(
                                    record.topic(),
                                    record.partition()
                            );

                    Long configuredEndObject =
                            endOffsets.get(
                                    record.partition()
                            );

                    if (configuredEndObject == null) {

                        throw new IllegalStateException(
                                "Unexpected partition: "
                                        + record.partition()
                        );
                    }

                    long configuredEnd =
                            configuredEndObject;

                    /*
                     * configuredEnd 是右开区间。
                     *
                     * User 当前：
                     *
                     * P0 = 20030
                     *
                     * 因此只允许读取：
                     *
                     * 0 ~ 20029
                     *
                     * offset 20030 开始属于 realtime。
                     */
                    if (record.offset()
                            >= configuredEnd) {

                        continue;
                    }

                    Long expectedOffsetObject =
                            nextOffsets.get(
                                    partition
                            );

                    if (expectedOffsetObject == null) {

                        throw new IllegalStateException(
                                "Unexpected active partition: "
                                        + partition
                        );
                    }

                    long expectedOffset =
                            expectedOffsetObject;

                    /*
                     * 必须保证历史消息连续。
                     *
                     * 如果期待 offset 100，
                     * 实际直接读到了 101，
                     * Bootstrap 立即失败，
                     * 防止在历史缺失的情况下生成错误基线。
                     */
                    if (record.offset()
                            != expectedOffset) {

                        throw new IllegalStateException(
                                "Non-contiguous history at "
                                        + partition
                                        + ": expected="
                                        + expectedOffset
                                        + ", actual="
                                        + record.offset()
                        );
                    }

                    DwdOrderInfo order =
                            DwdOrderInfoDeserializer
                                    .deserialize(
                                            record.value()
                                    );

                    if (order == null
                            || order.getOrderId() == null) {

                        throw new IllegalStateException(
                                "Invalid DWD order at "
                                        + partition
                                        + " offset "
                                        + record.offset()
                        );
                    }

                    orders.add(
                            order
                    );

                    /*
                     * 当前 offset 已成功读取，
                     * 下一条期望 offset = 当前 + 1。
                     */
                    nextOffsets.put(
                            partition,
                            record.offset() + 1L
                    );

                    progressed =
                            true;
                }

                /*
                 * 60 秒没有任何读取进展则失败。
                 *
                 * 避免 Kafka / 网络异常时
                 * Bootstrap 永久卡住。
                 */
                if (progressed) {

                    lastProgressAt =
                            System.currentTimeMillis();

                } else if (
                        System.currentTimeMillis()
                                - lastProgressAt
                                > 60_000L) {

                    throw new IllegalStateException(
                            "Timed out before reaching "
                                    + "all User bootstrap offsets"
                    );
                }
            }
        }

        return orders;
    }

    /**
     * 判断所有 active partition
     * 是否都已经读取到冻结的历史 endOffset。
     */
    private static boolean complete(
            Map<TopicPartition, Long> nextOffsets,
            Map<Integer, Long> endOffsets) {

        for (Map.Entry<TopicPartition, Long> entry :
                nextOffsets.entrySet()) {

            long requiredEnd =
                    endOffsets.get(
                            entry.getKey()
                                    .partition()
                    );

            if (entry.getValue()
                    < requiredEnd) {

                return false;
            }
        }

        return true;
    }
}