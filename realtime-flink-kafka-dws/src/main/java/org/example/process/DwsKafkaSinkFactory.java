package org.example.process;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

/**
 * DWS Kafka Sink工厂。
 *
 * 当前链路：
 *
 * TradeSummary
 *      ↓
 * JSON String
 *      ↓
 * FlinkKafkaProducer
 *      ↓
 * Kafka DWS Topic
 *
 * 后续商品、店铺、用户、售后等DWS主题
 * 也可以复用该Kafka Producer创建逻辑。
 */
public final class DwsKafkaSinkFactory {

    private DwsKafkaSinkFactory() {
    }

    /**
     * 创建String类型Kafka Producer。
     *
     * @param bootstrapServers Kafka Broker地址
     * @param topic            目标Kafka Topic
     * @return FlinkKafkaProducer<String>
     */
    public static FlinkKafkaProducer<String> createStringProducer(
            String bootstrapServers,
            String topic) {

        // ============================================================
        // 1. Kafka Producer配置
        // ============================================================

        Properties properties =
                new Properties();

        properties.setProperty(
                "bootstrap.servers",
                bootstrapServers
        );

        // ============================================================
        // 2. 创建Kafka Producer
        // ============================================================

        /*
         * 当前阶段使用默认Kafka Producer语义。
         *
         * 现阶段目标：
         *
         * TradeSummary
         *      ↓
         * JSON
         *      ↓
         * Kafka
         *      ↓
         * dws_trade_summary
         *
         * 先完成DWS -> Kafka业务链路验证。
         *
         * 后续再单独验证：
         *
         * Checkpoint
         *      +
         * Kafka事务
         *      +
         * 故障恢复
         *
         * 后再决定是否升级为端到端Exactly-Once。
         */
        return new FlinkKafkaProducer<String>(
                topic,
                new SimpleStringSchema(),
                properties
        );
    }
}