package org.example;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeSummary;
import org.example.process.DwdOrderInfoDeserializer;
import org.example.process.TradeBootstrapCalculator;
import org.example.process.TradeBootstrapConfig;
import org.example.process.TradeBootstrapResult;
import org.example.process.TradeSummarySerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * One-shot, bounded Trade history initializer. It does not use Flink watermarks.
 */
public final class TradeBootstrapApp {

    private static final String BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,192.168.13.133:9092,192.168.13.131:9092";
    private static final String INPUT_TOPIC = "dwd_order_info";
    private static final String OUTPUT_TOPIC = "dws_trade_summary";

    private TradeBootstrapApp() {
    }

    public static void main(String[] args) {
        TradeBootstrapResult result = null;
        try {
            List<DwdOrderInfo> records = readHistory();
            result = TradeBootstrapCalculator.calculate(records);
            validate(result);
            writeSummaries(result.getSummaries());
            printStatistics(result);
            System.out.println("TRADE BOOTSTRAP SUCCESS");
        } catch (Exception e) {
            if (result != null) {
                printStatistics(result);
            }
            System.err.println("TRADE BOOTSTRAP FAILED");
            e.printStackTrace();
            throw new IllegalStateException("Trade bootstrap failed", e);
        }
    }

    private static List<DwdOrderInfo> readHistory() {
        Map<Integer, Long> endOffsets = TradeBootstrapConfig.historyEndOffsets();
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.setProperty("group.id", "realtime-dws-trade-bootstrap-read-only");
        properties.setProperty("enable.auto.commit", "false");
        properties.setProperty("auto.offset.reset", "none");
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        List<DwdOrderInfo> orders = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            List<PartitionInfo> metadata = consumer.partitionsFor(INPUT_TOPIC);
            if (metadata.size() != endOffsets.size()) {
                throw new IllegalStateException(
                        "Expected " + endOffsets.size() + " partitions but found " + metadata.size()
                );
            }

            List<TopicPartition> allPartitions = new ArrayList<>();
            List<TopicPartition> activePartitions = new ArrayList<>();
            for (Integer partition : endOffsets.keySet()) {
                TopicPartition topicPartition = new TopicPartition(INPUT_TOPIC, partition);
                allPartitions.add(topicPartition);
                if (endOffsets.get(partition) > 0L) {
                    activePartitions.add(topicPartition);
                }
            }

            Map<TopicPartition, Long> beginnings = consumer.beginningOffsets(allPartitions);
            Map<TopicPartition, Long> currentEnds = consumer.endOffsets(allPartitions);
            for (TopicPartition partition : allPartitions) {
                long configuredEnd = endOffsets.get(partition.partition());
                if (beginnings.get(partition) != 0L) {
                    throw new IllegalStateException("History no longer begins at offset 0 for " + partition);
                }
                if (currentEnds.get(partition) < configuredEnd) {
                    throw new IllegalStateException(
                            "Incomplete history for " + partition + ": end="
                                    + currentEnds.get(partition) + ", required=" + configuredEnd
                    );
                }
            }

            if (activePartitions.isEmpty()) {
                return orders;
            }

            consumer.assign(activePartitions);
            Map<TopicPartition, Long> nextOffsets = new HashMap<>();
            for (TopicPartition partition : activePartitions) {
                consumer.seek(partition, 0L);
                nextOffsets.put(partition, 0L);
            }

            long lastProgressAt = System.currentTimeMillis();
            while (!complete(nextOffsets, endOffsets)) {
                ConsumerRecords<String, String> polled = consumer.poll(Duration.ofSeconds(1));
                boolean progressed = false;
                for (ConsumerRecord<String, String> record : polled) {
                    TopicPartition partition =
                            new TopicPartition(record.topic(), record.partition());
                    long configuredEnd = endOffsets.get(record.partition());
                    if (record.offset() >= configuredEnd) {
                        continue;
                    }
                    long expectedOffset = nextOffsets.get(partition);
                    if (record.offset() != expectedOffset) {
                        throw new IllegalStateException(
                                "Non-contiguous history at " + partition
                                        + ": expected=" + expectedOffset
                                        + ", actual=" + record.offset()
                        );
                    }
                    DwdOrderInfo order = DwdOrderInfoDeserializer.deserialize(record.value());
                    if (order == null || order.getOrderId() == null) {
                        throw new IllegalStateException(
                                "Invalid DWD order at " + partition + " offset " + record.offset()
                        );
                    }
                    orders.add(order);
                    nextOffsets.put(partition, record.offset() + 1L);
                    progressed = true;
                }
                if (progressed) {
                    lastProgressAt = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastProgressAt > 60_000L) {
                    throw new IllegalStateException("Timed out before reaching all bootstrap offsets");
                }
            }
        }
        return orders;
    }

    private static boolean complete(Map<TopicPartition, Long> nextOffsets,
                                    Map<Integer, Long> endOffsets) {
        for (Map.Entry<TopicPartition, Long> entry : nextOffsets.entrySet()) {
            if (entry.getValue() < endOffsets.get(entry.getKey().partition())) {
                return false;
            }
        }
        return true;
    }

    private static void validate(TradeBootstrapResult result) {
        require("read records", TradeBootstrapConfig.expectedRecordCount(), result.getReadCount());
        require("bootstrap records", TradeBootstrapConfig.expectedBootstrapCount(), result.getBootstrapCount());
        require("UPDATE records", TradeBootstrapConfig.expectedUpdateCount(), result.getUpdateCount());
        require("unique orders", TradeBootstrapConfig.expectedUniqueOrderCount(), result.getUniqueOrderCount());
        require("orderCount", result.getUniqueOrderCount(), result.getOrderCount());
        if (result.getSummaries().isEmpty()) {
            throw new IllegalStateException("No TradeSummary was generated");
        }
    }

    private static void require(String name, long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException(
                    name + " mismatch: expected=" + expected + ", actual=" + actual
            );
        }
    }

    private static void writeSummaries(List<TradeSummary> summaries) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.setProperty("acks", "all");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            for (TradeSummary summary : summaries) {
                producer.send(new ProducerRecord<>(
                        OUTPUT_TOPIC,
                        summary.getWindowStart(),
                        TradeSummarySerializer.serialize(summary)
                )).get();
            }
            producer.flush();
        }
    }

    private static void printStatistics(TradeBootstrapResult result) {
        System.out.println("读取总数=" + result.getReadCount());
        System.out.println("bootstrap=" + result.getBootstrapCount());
        System.out.println("UPDATE=" + result.getUpdateCount());
        System.out.println("最终唯一orderId=" + result.getUniqueOrderCount());
        System.out.println("历史最早createTime=" + result.getEarliestCreateTime());
        System.out.println("历史最晚createTime=" + result.getLatestCreateTime());
        System.out.println("TradeSummary分钟数=" + result.getSummaries().size());
        System.out.println("orderCount=" + result.getOrderCount());
        System.out.println("paidOrderCount=" + result.getPaidOrderCount());
        System.out.println("paidAmount=" + result.getPaidAmount().toPlainString());
    }
}
