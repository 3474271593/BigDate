package org.example;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KafkaOffsetDiagnostic {

    private static final String BOOTSTRAP_SERVERS = "192.168.13.232:9092";
    private static final String TOPIC = "ods_shop_db";

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.setProperty("group.id", "kafka-offset-diagnostic");
        properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("enable.auto.commit", "false");
        properties.setProperty("request.timeout.ms", "10000");
        properties.setProperty("default.api.timeout.ms", "30000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            System.out.println("========== 1. 查询 Topic Partition ==========");

            List<PartitionInfo> partitionInfos = consumer.partitionsFor(TOPIC);
            List<TopicPartition> partitions = new ArrayList<>();

            for (PartitionInfo info : partitionInfos) {
                TopicPartition partition = new TopicPartition(TOPIC, info.partition());
                partitions.add(partition);

                System.out.println(
                        "partition=" + info.partition()
                                + ", leader=" + info.leader()
                                + ", replicas=" + info.replicas().length
                                + ", isr=" + info.inSyncReplicas().length
                );
            }

            System.out.println();
            System.out.println("========== 2. 查询 Beginning Offset ==========");

            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);

            for (TopicPartition partition : partitions) {
                System.out.println(
                        partition
                                + " beginningOffset="
                                + beginningOffsets.get(partition)
                );
            }

            System.out.println();
            System.out.println("========== 3. 查询 End Offset ==========");

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            for (TopicPartition partition : partitions) {
                System.out.println(
                        partition
                                + " endOffset="
                                + endOffsets.get(partition)
                );
            }

            System.out.println();
            System.out.println("========== 4. 单独测试 Partition 2 ==========");

            TopicPartition partition2 = new TopicPartition(TOPIC, 2);

            consumer.assign(Collections.singletonList(partition2));

            System.out.println("assign partition 2 成功");

            consumer.seekToBeginning(Collections.singletonList(partition2));

            System.out.println("seekToBeginning partition 2 成功");

            long position = consumer.position(partition2);

            System.out.println("partition 2 position=" + position);

            System.out.println();
            System.out.println("========== 5. Poll 第一批数据 ==========");

            int count = consumer.poll(Duration.ofSeconds(10)).count();

            System.out.println("poll records count=" + count);

            System.out.println();
            System.out.println("========== Kafka Offset Diagnostic SUCCESS ==========");
        } catch (Exception e) {
            System.err.println();
            System.err.println("========== Kafka Offset Diagnostic FAILED ==========");
            e.printStackTrace();
        }
    }
}