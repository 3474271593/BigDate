package org.example.process;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/** Reads fixed Goods history ranges without committing consumer offsets. */
public final class GoodsBootstrapHistoryReader {
    private static final String BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,192.168.13.133:9092,192.168.13.131:9092";
    private static final String ORDER_TOPIC = "dwd_order_info";
    private static final String DETAIL_TOPIC = "dwd_order_goods";

    private GoodsBootstrapHistoryReader() {
    }

    public static GoodsBootstrapHistory readHistory() {
        List<DwdOrderInfo> orders = read(
                ORDER_TOPIC,
                "realtime-dws-goods-bootstrap-order-read-only",
                GoodsBootstrapConfig.orderEndOffsets(),
                DwdOrderInfoDeserializer::deserialize,
                value -> value != null && value.getOrderId() != null
        );
        List<DwdOrderGoods> details = read(
                DETAIL_TOPIC,
                "realtime-dws-goods-bootstrap-detail-read-only",
                GoodsBootstrapConfig.detailEndOffsets(),
                DwdOrderGoodsDeserializer::deserialize,
                value -> value != null
                        && value.getOrderId() != null
                        && value.getOgId() != null
                        && value.getGoodsId() != null
        );
        return new GoodsBootstrapHistory(orders, details);
    }

    private static <T> List<T> read(
            String topic,
            String groupId,
            Map<Integer, Long> endOffsets,
            Function<String, T> parser,
            java.util.function.Predicate<T> validator) {

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.setProperty("group.id", groupId);
        properties.setProperty("enable.auto.commit", "false");
        properties.setProperty("auto.offset.reset", "none");
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        List<T> values = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            List<PartitionInfo> metadata = consumer.partitionsFor(topic);
            if (metadata.size() != endOffsets.size()) {
                throw new IllegalStateException(
                        topic + " partition count mismatch: expected="
                                + endOffsets.size() + ", actual=" + metadata.size()
                );
            }

            List<TopicPartition> all = new ArrayList<>();
            List<TopicPartition> active = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : endOffsets.entrySet()) {
                TopicPartition partition = new TopicPartition(topic, entry.getKey());
                all.add(partition);
                if (entry.getValue() > 0L) {
                    active.add(partition);
                }
            }

            Map<TopicPartition, Long> beginnings = consumer.beginningOffsets(all);
            Map<TopicPartition, Long> currentEnds = consumer.endOffsets(all);
            for (TopicPartition partition : all) {
                long requiredEnd = endOffsets.get(partition.partition());
                Long beginning = beginnings.get(partition);
                Long currentEnd = currentEnds.get(partition);
                if (beginning == null || beginning != 0L) {
                    throw new IllegalStateException(
                            "History no longer begins at offset 0 for " + partition
                                    + ", beginning=" + beginning
                    );
                }
                if (currentEnd == null || currentEnd < requiredEnd) {
                    throw new IllegalStateException(
                            "Incomplete history for " + partition
                                    + ": currentEnd=" + currentEnd
                                    + ", requiredEnd=" + requiredEnd
                    );
                }
            }

            if (active.isEmpty()) {
                return values;
            }

            consumer.assign(active);
            Map<TopicPartition, Long> nextOffsets = new HashMap<>();
            for (TopicPartition partition : active) {
                consumer.seek(partition, 0L);
                nextOffsets.put(partition, 0L);
            }

            long lastProgressAt = System.currentTimeMillis();
            while (!complete(nextOffsets, endOffsets)) {
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofSeconds(1L));
                boolean progressed = false;
                for (ConsumerRecord<String, String> record : records) {
                    TopicPartition partition =
                            new TopicPartition(record.topic(), record.partition());
                    Long requiredEnd = endOffsets.get(record.partition());
                    if (requiredEnd == null || record.offset() >= requiredEnd) {
                        continue;
                    }
                    Long expected = nextOffsets.get(partition);
                    if (expected == null || record.offset() != expected) {
                        throw new IllegalStateException(
                                "Non-contiguous history at " + partition
                                        + ": expected=" + expected
                                        + ", actual=" + record.offset()
                        );
                    }
                    T value = parser.apply(record.value());
                    if (!validator.test(value)) {
                        throw new IllegalStateException(
                                "Invalid DWD record at " + partition
                                        + " offset " + record.offset()
                        );
                    }
                    values.add(value);
                    nextOffsets.put(partition, record.offset() + 1L);
                    progressed = true;
                }

                if (progressed) {
                    lastProgressAt = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastProgressAt > 60_000L) {
                    throw new IllegalStateException(
                            "Timed out before reaching configured end offsets for " + topic
                    );
                }
            }
        }
        return values;
    }

    private static boolean complete(
            Map<TopicPartition, Long> nextOffsets,
            Map<Integer, Long> endOffsets) {
        for (Map.Entry<TopicPartition, Long> entry : nextOffsets.entrySet()) {
            if (entry.getValue() < endOffsets.get(entry.getKey().partition())) {
                return false;
            }
        }
        return true;
    }

    public static final class GoodsBootstrapHistory {
        private final List<DwdOrderInfo> orders;
        private final List<DwdOrderGoods> details;

        private GoodsBootstrapHistory(
                List<DwdOrderInfo> orders,
                List<DwdOrderGoods> details) {
            this.orders = orders;
            this.details = details;
        }

        public List<DwdOrderInfo> getOrders() {
            return orders;
        }

        public List<DwdOrderGoods> getDetails() {
            return details;
        }
    }
}
