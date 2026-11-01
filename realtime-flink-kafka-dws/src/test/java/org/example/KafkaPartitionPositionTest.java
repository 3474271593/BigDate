// 文件路径：src/test/java/org/example/KafkaPartitionPositionTest.java
package org.example;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Kafka分区Position诊断测试。
 *
 * 目的：
 * 1. 完全绕开Flink；
 * 2. 直接使用DWS项目当前的Kafka Java Client；
 * 3. 检查Windows Java程序是否可以正常访问dwd_order_info的三个Partition；
 * 4. 检查空Partition是否能够正常获得position=0。
 *
 * 注意：
 * 本测试不会提交Consumer Group Offset，
 * 也不会修改Kafka中的任何业务数据。
 */
public class KafkaPartitionPositionTest {

    private static final String BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,192.168.13.133:9092,192.168.13.131:9092";

    private static final String TOPIC = "dwd_order_info";

    @Test
    public void testPartitionPositions() {

        Properties properties = new Properties();

        properties.setProperty(
                "bootstrap.servers",
                BOOTSTRAP_SERVERS
        );

        properties.setProperty(
                "key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer"
        );

        properties.setProperty(
                "value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer"
        );

        properties.setProperty(
                "group.id",
                "dws-kafka-position-test"
        );

        properties.setProperty(
                "enable.auto.commit",
                "false"
        );

        properties.setProperty(
                "auto.offset.reset",
                "earliest"
        );

        properties.setProperty(
                "request.timeout.ms",
                "10000"
        );

        properties.setProperty(
                "default.api.timeout.ms",
                "10000"
        );

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            System.out.println("========================================");
            System.out.println("Kafka Partition Position Test");
            System.out.println("========================================");

            // 1. 获取Topic元数据
            List<PartitionInfo> partitionInfos =
                    consumer.partitionsFor(TOPIC);

            System.out.println("Topic Partition数量：" + partitionInfos.size());

            for (PartitionInfo partitionInfo : partitionInfos) {
                System.out.println(
                        "Partition " + partitionInfo.partition()
                                + " Leader = "
                                + partitionInfo.leader()
                );
            }

            System.out.println("----------------------------------------");

            // 2. 明确指定三个Partition
            TopicPartition partition0 =
                    new TopicPartition(TOPIC, 0);

            TopicPartition partition1 =
                    new TopicPartition(TOPIC, 1);

            TopicPartition partition2 =
                    new TopicPartition(TOPIC, 2);

            List<TopicPartition> partitions = Arrays.asList(
                    partition0,
                    partition1,
                    partition2
            );

            // 3. 手动分配Partition，不经过Consumer Group Rebalance
            consumer.assign(partitions);

            // 4. 明确要求三个Partition从最早Offset开始
            consumer.seekToBeginning(partitions);

            // 5. 分别获取三个Partition当前Position
            for (TopicPartition partition : partitions) {

                System.out.println(
                        "正在获取 Partition "
                                + partition.partition()
                                + " 的 position..."
                );

                try {

                    long position =
                            consumer.position(partition);

                    System.out.println(
                            "Partition "
                                    + partition.partition()
                                    + " position = "
                                    + position
                    );

                } catch (Exception e) {

                    System.out.println(
                            "Partition "
                                    + partition.partition()
                                    + " position 获取失败"
                    );

                    e.printStackTrace();
                }
            }

            System.out.println("----------------------------------------");

            // 6. 获取三个Partition的Beginning Offset
            try {

                System.out.println(
                        "BeginningOffsets = "
                                + consumer.beginningOffsets(partitions)
                );

            } catch (Exception e) {

                System.out.println(
                        "BeginningOffsets 获取失败"
                );

                e.printStackTrace();
            }

            // 7. 获取三个Partition的End Offset
            try {

                System.out.println(
                        "EndOffsets = "
                                + consumer.endOffsets(partitions)
                );

            } catch (Exception e) {

                System.out.println(
                        "EndOffsets 获取失败"
                );

                e.printStackTrace();
            }

            System.out.println("========================================");
            System.out.println("测试结束");
            System.out.println("========================================");
        }
    }
}