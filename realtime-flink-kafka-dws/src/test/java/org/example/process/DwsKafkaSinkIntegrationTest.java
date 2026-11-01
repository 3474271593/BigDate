// 文件路径：src/main/java/org/example/test/DwsKafkaSinkIntegrationTest.java
package org.example.process;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.example.bean.TradeSummary;
import org.example.process.DwsKafkaSinkFactory;
import org.example.process.TradeSummarySerializer;

import java.math.BigDecimal;

/**
 * DWS Kafka Sink独立集成验证程序。
 *
 * 目的：
 * 只验证当前新增链路：
 *
 * TradeSummary
 *      ↓
 * JSON
 *      ↓
 * FlinkKafkaProducer
 *      ↓
 * dws_trade_summary
 *
 * 不重新测试：
 * MySQL / Canal / ODS / DWD / Watermark / Window。
 *
 * 验证完成后，该类仅作为开发阶段测试工具保留。
 */
public class DwsKafkaSinkIntegrationTest {

    private static final String KAFKA_BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,192.168.13.133:9092,192.168.13.131:9092";

    private static final String DWS_TRADE_SUMMARY_TOPIC =
            "dws_trade_summary";

    public static void main(String[] args) throws Exception {

        // ============================================================
        // 1. 创建执行环境
        // ============================================================

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        // ============================================================
        // 2. 构造一条模拟TradeSummary
        // ============================================================

        TradeSummary summary =
                new TradeSummary();

        summary.setWindowStart(
                "2026-09-17 10:20:00"
        );

        summary.setWindowEnd(
                "2026-09-17 10:21:00"
        );

        summary.setOrderCount(
                10L
        );

        summary.setPaidOrderCount(
                8L
        );

        summary.setPaidAmount(
                new BigDecimal("8888.88")
        );

        // ============================================================
        // 3. TradeSummary -> JSON
        // ============================================================

        String json =
                TradeSummarySerializer.serialize(
                        summary
                );

        System.out.println(
                "准备发送到Kafka："
        );

        System.out.println(
                json
        );

        // ============================================================
        // 4. JSON -> Kafka
        // ============================================================

        env.fromElements(
                        json
                )
                .addSink(
                        DwsKafkaSinkFactory.createStringProducer(
                                KAFKA_BOOTSTRAP_SERVERS,
                                DWS_TRADE_SUMMARY_TOPIC
                        )
                )
                .name(
                        "dws-trade-summary-integration-test-sink"
                );

        // ============================================================
        // 5. 启动测试Job
        // ============================================================

        env.execute(
                "DWS Kafka Sink Integration Test"
        );
    }
}