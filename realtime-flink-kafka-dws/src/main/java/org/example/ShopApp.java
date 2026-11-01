// 文件路径：src/main/java/org/example/ShopApp.java
package org.example;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import org.example.bean.ShopDelta;
import org.example.bean.ShopSourceRecord;
import org.example.bean.ShopSummary;

import org.example.process.DwsKafkaSinkFactory;
import org.example.process.FlinkCheckpointConfig;
import org.example.process.ShopAggregateFunction;
import org.example.process.ShopBootstrapConfig;
import org.example.process.ShopSourceRecordDeserializer;
import org.example.process.ShopSummarySerializer;
import org.example.process.ShopWarmupProcessFunction;
import org.example.process.ShopWindowProcessFunction;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * 实时数仓 DWS 店铺主题主程序。
 *
 * 完整链路：
 *
 * dwd_order_info
 *      ↓
 * ShopSourceRecordDeserializer
 *      ↓
 * ShopSourceRecord
 *      ↓
 * keyBy(orderId)
 *      ↓
 * ShopWarmupProcessFunction
 *      ↓
 * ShopDelta
 *      ↓
 * Event Time + Watermark
 *      ↓
 * keyBy(shopId)
 *      ↓
 * 1分钟 Tumbling Event Time Window
 *      ↓
 * ShopAggregateFunction
 *      +
 * ShopWindowProcessFunction
 *      ↓
 * ShopSummary
 *      ↓
 * JSON
 *      ↓
 * Kafka
 *      ↓
 * dws_shop_summary
 *
 *
 * Shop V1 指标：
 *
 * orderCount
 * orderUserCount
 * orderAmount
 * paidOrderCount
 * paidUserCount
 * paidAmount
 *
 *
 * Event Time：
 *
 * SHOP_ORDER
 *      -> createTime
 *
 * SHOP_PAY
 *      -> payTime
 *
 *
 * 历史初始化策略：
 *
 * ShopBootstrapApp 已经完成固定历史范围的基线初始化。
 *
 * 当前历史边界：
 *
 * partition 0 : [0, 20028)
 * partition 1 : [0, 0)
 * partition 2 : [0, 0)
 *
 * ShopApp 第一次启动时仍从 earliest 读取，
 * 但历史范围只用于恢复 previous-order-state，
 * 不重新产生 ShopDelta。
 *
 * 到达 Bootstrap 边界后，
 * 才正式开始实时计算。
 *
 * 后续正常重启应从 Shop 自己的 Checkpoint 恢复。
 */
public class ShopApp {

    // ============================================================
    // Kafka
    // ============================================================

    private static final String KAFKA_BOOTSTRAP_SERVERS =
            "master:9092,slave1:9092,slave2:9092";

    /**
     * DWD订单事实Topic。
     */
    private static final String DWD_ORDER_INFO_TOPIC =
            "dwd_order_info";

    /**
     * Shop DWS输出Topic。
     */
    private static final String DWS_SHOP_SUMMARY_TOPIC =
            "dws_shop_summary";

    /**
     * Shop独立Consumer Group。
     */
    private static final String SHOP_CONSUMER_GROUP =
            "realtime-dws-shop-v1";

    /**
     * Shop独立Checkpoint目录。
     *
     * 不能与Trade/Goods共用。
     */
    private static final String SHOP_CHECKPOINT_PATH =
            "hdfs:///flink/checkpoints/dws-shop";

    /**
     * 当前开发阶段Watermark最大乱序时间。
     */
    private static final Duration MAX_OUT_OF_ORDERNESS =
            Duration.ofSeconds(5);

    public static void main(String[] args)
            throws Exception {

        // ============================================================
        // 1. Flink执行环境
        // ============================================================

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment
                        .getExecutionEnvironment();

        /*
         * 当前开发阶段并行度1。
         *
         * 方便验证：
         *
         * ValueState
         * Bootstrap边界
         * Event Time
         * Watermark
         * Window
         * Checkpoint
         * Kafka Sink
         */
        env.setParallelism(1);

        // ============================================================
        // 2. Shop独立Checkpoint
        // ============================================================

        FlinkCheckpointConfig.configure(
                env,
                SHOP_CHECKPOINT_PATH
        );

        // ============================================================
        // 3. Kafka Consumer配置
        // ============================================================

        Properties kafkaProperties =
                new Properties();

        kafkaProperties.setProperty(
                "bootstrap.servers",
                KAFKA_BOOTSTRAP_SERVERS
        );

        kafkaProperties.setProperty(
                "group.id",
                SHOP_CONSUMER_GROUP
        );

        /*
         * Kafka Source恢复进度主要由Flink Checkpoint管理。
         */
        kafkaProperties.setProperty(
                "enable.auto.commit",
                "false"
        );

        /*
         * 没有Checkpoint状态时，
         * 从最早可用Offset开始。
         */
        kafkaProperties.setProperty(
                "auto.offset.reset",
                "earliest"
        );

        // ============================================================
        // 4. Kafka Source
        // ============================================================

        FlinkKafkaConsumer<ShopSourceRecord> kafkaConsumer =
                new FlinkKafkaConsumer<>(
                        DWD_ORDER_INFO_TOPIC,
                        new ShopSourceRecordDeserializer(),
                        kafkaProperties
                );

        /*
         * 第一次启动必须从earliest开始。
         *
         * 原因：
         *
         * ShopWarmupProcessFunction需要重新读取Bootstrap历史范围，
         * 用于恢复previous-order-state。
         *
         * 历史范围不会产生ShopDelta。
         */
        kafkaConsumer.setStartFromEarliest();

        DataStream<ShopSourceRecord> kafkaStream =
                env.addSource(
                                kafkaConsumer
                        )
                        .name(
                                "shop-order-info-kafka-source"
                        )
                        /*
                         * Shop Source自己的稳定UID。
                         *
                         * Shop正式产生Checkpoint以后不要修改。
                         */
                        .uid(
                                "shop-order-info-kafka-source"
                        );

        // ============================================================
        // 5. 基础过滤
        // ============================================================

        SingleOutputStreamOperator<ShopSourceRecord> orderStream =
                kafkaStream
                        .filter(
                                record ->
                                        record != null
                                                && record.getOrder() != null
                                                && record.getOrder()
                                                .getOrderId() != null
                                                && record.getOrder()
                                                .getOrderId() > 0
                        )
                        .name(
                                "shop-filter-valid-order"
                        )
                        .uid(
                                "shop-filter-valid-order"
                        );

        // ============================================================
        // 6. Bootstrap历史预热 + 实时ShopDelta
        // ============================================================

        /*
         * 当前边界：
         *
         * P0 -> 20028
         * P1 -> 0
         * P2 -> 0
         */
        Map<Integer, Long> realtimeStartOffsets =
                ShopBootstrapConfig.getEndOffsets();

        SingleOutputStreamOperator<ShopDelta> shopDeltaStream =
                orderStream
                        .keyBy(
                                record ->
                                        record.getOrder()
                                                .getOrderId()
                        )
                        .process(
                                new ShopWarmupProcessFunction(
                                        realtimeStartOffsets
                                )
                        )
                        .name(
                                "shop-warmup-change-process"
                        )
                        /*
                         * Shop最关键的Stateful Operator。
                         *
                         * 内部包含：
                         *
                         * ValueState<DwdOrderInfo>
                         *
                         * 正式Checkpoint产生以后不要修改UID。
                         */
                        .uid(
                                "shop-warmup-change-process"
                        );

        // ============================================================
        // 7. Event Time + Watermark
        // ============================================================

        /*
         * ShopDeltaCalculator已经确定：
         *
         * SHOP_ORDER
         *      eventTs = createTime
         *
         * SHOP_PAY
         *      eventTs = payTime
         */
        WatermarkStrategy<ShopDelta> shopWatermarkStrategy =
                WatermarkStrategy
                        .<ShopDelta>forBoundedOutOfOrderness(
                                MAX_OUT_OF_ORDERNESS
                        )
                        .withTimestampAssigner(
                                new SerializableTimestampAssigner<ShopDelta>() {

                                    @Override
                                    public long extractTimestamp(
                                            ShopDelta element,
                                            long recordTimestamp) {

                                        return element.getEventTs();
                                    }
                                }
                        );

        SingleOutputStreamOperator<ShopDelta> shopEventTimeStream =
                shopDeltaStream
                        .assignTimestampsAndWatermarks(
                                shopWatermarkStrategy
                        )
                        .name(
                                "shop-event-time-watermark"
                        )
                        .uid(
                                "shop-event-time-watermark"
                        );

        // ============================================================
        // 8. shopId + 1分钟窗口
        // ============================================================

        /*
         * Shop DWS粒度：
         *
         * shopId + 1分钟
         *
         * 每个窗口计算：
         *
         * orderCount
         * orderUserCount
         * orderAmount
         *
         * paidOrderCount
         * paidUserCount
         * paidAmount
         */
        SingleOutputStreamOperator<ShopSummary> shopSummaryStream =
                shopEventTimeStream
                        .keyBy(
                                ShopDelta::getShopId
                        )
                        .window(
                                TumblingEventTimeWindows.of(
                                        Time.minutes(1)
                                )
                        )
                        .aggregate(
                                new ShopAggregateFunction(),
                                new ShopWindowProcessFunction()
                        )
                        .name(
                                "one-minute-shop-summary"
                        )
                        /*
                         * Shop第二个关键Stateful Operator。
                         *
                         * Window State / Timer均依赖Checkpoint。
                         */
                        .uid(
                                "one-minute-shop-summary"
                        );

        // ============================================================
        // 9. 控制台打印
        // ============================================================

        /*
         * 开发阶段保留，
         * 方便和Kafka dws_shop_summary结果进行对比。
         */
        shopSummaryStream.print(
                "DWS-SHOP-SUMMARY"
        );

        // ============================================================
        // 10. ShopSummary -> JSON
        // ============================================================

        SingleOutputStreamOperator<String> shopSummaryJsonStream =
                shopSummaryStream
                        .map(
                                ShopSummarySerializer::serialize
                        )
                        .returns(
                                String.class
                        )
                        .filter(
                                json ->
                                        json != null
                                                && !json.trim().isEmpty()
                        )
                        .name(
                                "serialize-shop-summary"
                        )
                        .uid(
                                "serialize-shop-summary"
                        );

        // ============================================================
        // 11. JSON -> Kafka dws_shop_summary
        // ============================================================

        shopSummaryJsonStream
                .addSink(
                        DwsKafkaSinkFactory
                                .createStringProducer(
                                        KAFKA_BOOTSTRAP_SERVERS,
                                        DWS_SHOP_SUMMARY_TOPIC
                                )
                )
                .name(
                        "dws-shop-summary-kafka-sink"
                )
                .uid(
                        "dws-shop-summary-kafka-sink"
                );

        // ============================================================
        // 12. 启动信息
        // ============================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "启动 Realtime DWS Shop Job"
        );

        System.out.println(
                "Source Topic："
                        + DWD_ORDER_INFO_TOPIC
        );

        System.out.println(
                "Output Topic："
                        + DWS_SHOP_SUMMARY_TOPIC
        );

        System.out.println(
                "Consumer Group："
                        + SHOP_CONSUMER_GROUP
        );

        System.out.println(
                "首次初始化Offset：earliest"
        );

        System.out.println(
                "Bootstrap历史边界："
                        + realtimeStartOffsets
        );

        System.out.println(
                "历史范围：只恢复previous-order-state，不产生ShopDelta"
        );

        System.out.println(
                "实时范围：Bootstrap边界之后开始产生ShopDelta"
        );

        System.out.println(
                "State Key：orderId"
        );

        System.out.println(
                "DWS统计粒度：shopId + 1分钟"
        );

        System.out.println(
                "SHOP_ORDER时间：createTime"
        );

        System.out.println(
                "SHOP_PAY时间：payTime"
        );

        System.out.println(
                "Event Time：ShopDelta.eventTs"
        );

        System.out.println(
                "Watermark最大乱序时间：5秒（当前开发参数）"
        );

        System.out.println(
                "指标：orderCount、orderUserCount、orderAmount、"
                        + "paidOrderCount、paidUserCount、paidAmount"
        );

        System.out.println(
                "Checkpoint目录："
                        + SHOP_CHECKPOINT_PATH
        );

        System.out.println(
                "========================================"
        );

        // ============================================================
        // 13. 启动Shop Job
        // ============================================================

        env.execute(
                "Realtime DWS Shop Job"
        );
    }
}
