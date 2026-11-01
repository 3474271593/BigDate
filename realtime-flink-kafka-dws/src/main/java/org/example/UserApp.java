package org.example;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import org.example.bean.UserDelta;
import org.example.bean.UserSourceRecord;
import org.example.bean.UserSummary;

import org.example.process.DwsKafkaSinkFactory;
import org.example.process.FlinkCheckpointConfig;
import org.example.process.UserAggregateFunction;
import org.example.process.UserBootstrapConfig;
import org.example.process.UserSourceRecordDeserializer;
import org.example.process.UserSummarySerializer;
import org.example.process.UserWarmupProcessFunction;
import org.example.process.UserWindowProcessFunction;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * 实时数仓 DWS 用户主题主程序。
 *
 * 完整链路：
 *
 * dwd_order_info
 *      ↓
 * UserSourceRecordDeserializer
 *      ↓
 * UserSourceRecord
 *      ↓
 * keyBy(orderId)
 *      ↓
 * UserWarmupProcessFunction
 *      ↓
 * UserDelta
 *      ↓
 * Event Time + Watermark
 *      ↓
 * keyBy(userId)
 *      ↓
 * 1分钟 Tumbling Event Time Window
 *      ↓
 * UserAggregateFunction
 *      +
 * UserWindowProcessFunction
 *      ↓
 * UserSummary
 *      ↓
 * JSON
 *      ↓
 * Kafka
 *      ↓
 * dws_user_summary
 *
 *
 * User V1 指标：
 *
 * orderCount
 * orderShopCount
 * orderAmount
 * paidOrderCount
 * paidShopCount
 * paidAmount
 *
 *
 * Event Time：
 *
 * USER_ORDER
 *      -> createTime
 *
 * USER_PAY
 *      -> payTime
 *
 *
 * 历史初始化策略：
 *
 * UserBootstrapApp 已经完成固定历史范围的基线初始化。
 *
 * 当前历史边界：
 *
 * partition 0 : [0, 20030)
 * partition 1 : [0, 0)
 * partition 2 : [0, 0)
 *
 * UserApp 第一次启动时仍从 earliest 读取，
 * 但历史范围只用于恢复 previous-order-state，
 * 不重新产生 UserDelta。
 *
 * 到达 Bootstrap 边界后，
 * 才正式开始实时计算。
 *
 * 后续正常重启应从 User 自己的 Checkpoint 恢复。
 */
public class UserApp {

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
     * User DWS输出Topic。
     */
    private static final String DWS_USER_SUMMARY_TOPIC =
            "dws_user_summary";

    /**
     * User独立Consumer Group。
     */
    private static final String USER_CONSUMER_GROUP =
            "realtime-dws-user-v1";

    /**
     * User独立Checkpoint目录。
     *
     * 不能与Trade/Goods/Shop共用。
     */
    private static final String USER_CHECKPOINT_PATH =
            "hdfs:///flink/checkpoints/dws-user";

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
        // 2. User独立Checkpoint
        // ============================================================

        FlinkCheckpointConfig.configure(
                env,
                USER_CHECKPOINT_PATH
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
                USER_CONSUMER_GROUP
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

        FlinkKafkaConsumer<UserSourceRecord> kafkaConsumer =
                new FlinkKafkaConsumer<>(
                        DWD_ORDER_INFO_TOPIC,
                        new UserSourceRecordDeserializer(),
                        kafkaProperties
                );

        /*
         * 第一次启动必须从earliest开始。
         *
         * UserWarmupProcessFunction需要重新读取Bootstrap历史范围，
         * 用于恢复previous-order-state。
         *
         * 历史范围不会产生UserDelta。
         */
        kafkaConsumer.setStartFromEarliest();

        DataStream<UserSourceRecord> kafkaStream =
                env.addSource(
                                kafkaConsumer
                        )
                        .name(
                                "user-order-info-kafka-source"
                        )
                        /*
                         * User Source稳定UID。
                         *
                         * 正式产生Checkpoint以后不要修改。
                         */
                        .uid(
                                "user-order-info-kafka-source"
                        );

        // ============================================================
        // 5. 基础过滤
        // ============================================================

        SingleOutputStreamOperator<UserSourceRecord> orderStream =
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
                                "user-filter-valid-order"
                        )
                        .uid(
                                "user-filter-valid-order"
                        );

        // ============================================================
        // 6. Bootstrap历史预热 + 实时UserDelta
        // ============================================================

        /*
         * User固定历史边界：
         *
         * P0 -> 20030
         * P1 -> 0
         * P2 -> 0
         */
        Map<Integer, Long> realtimeStartOffsets =
                UserBootstrapConfig.getEndOffsets();

        SingleOutputStreamOperator<UserDelta> userDeltaStream =
                orderStream
                        .keyBy(
                                record ->
                                        record.getOrder()
                                                .getOrderId()
                        )
                        .process(
                                new UserWarmupProcessFunction(
                                        realtimeStartOffsets
                                )
                        )
                        .name(
                                "user-warmup-change-process"
                        )
                        /*
                         * User最关键的Stateful Operator。
                         *
                         * 内部包含：
                         *
                         * ValueState<DwdOrderInfo>
                         *
                         * 正式Checkpoint产生以后不要修改UID。
                         */
                        .uid(
                                "user-warmup-change-process"
                        );

        // ============================================================
        // 7. Event Time + Watermark
        // ============================================================

        /*
         * UserDeltaCalculator已经确定：
         *
         * USER_ORDER
         *      eventTs = createTime
         *
         * USER_PAY
         *      eventTs = payTime
         */
        WatermarkStrategy<UserDelta> userWatermarkStrategy =
                WatermarkStrategy
                        .<UserDelta>forBoundedOutOfOrderness(
                                MAX_OUT_OF_ORDERNESS
                        )
                        .withTimestampAssigner(
                                new SerializableTimestampAssigner<UserDelta>() {

                                    @Override
                                    public long extractTimestamp(
                                            UserDelta element,
                                            long recordTimestamp) {

                                        return element.getEventTs();
                                    }
                                }
                        );

        SingleOutputStreamOperator<UserDelta> userEventTimeStream =
                userDeltaStream
                        .assignTimestampsAndWatermarks(
                                userWatermarkStrategy
                        )
                        .name(
                                "user-event-time-watermark"
                        )
                        .uid(
                                "user-event-time-watermark"
                        );

        // ============================================================
        // 8. userId + 1分钟窗口
        // ============================================================

        /*
         * User DWS粒度：
         *
         * userId + 1分钟
         *
         * 每个窗口计算：
         *
         * orderCount
         * orderShopCount
         * orderAmount
         *
         * paidOrderCount
         * paidShopCount
         * paidAmount
         */
        SingleOutputStreamOperator<UserSummary> userSummaryStream =
                userEventTimeStream
                        .keyBy(
                                UserDelta::getUserId
                        )
                        .window(
                                TumblingEventTimeWindows.of(
                                        Time.minutes(1)
                                )
                        )
                        .aggregate(
                                new UserAggregateFunction(),
                                new UserWindowProcessFunction()
                        )
                        .name(
                                "one-minute-user-summary"
                        )
                        /*
                         * User第二个关键Stateful Operator。
                         *
                         * Window State / Timer均依赖Checkpoint。
                         */
                        .uid(
                                "one-minute-user-summary"
                        );

        // ============================================================
        // 9. 控制台打印
        // ============================================================

        /*
         * 开发阶段保留，
         * 方便和Kafka dws_user_summary结果进行对比。
         */
        userSummaryStream.print(
                "DWS-USER-SUMMARY"
        );

        // ============================================================
        // 10. UserSummary -> JSON
        // ============================================================

        SingleOutputStreamOperator<String> userSummaryJsonStream =
                userSummaryStream
                        .map(
                                UserSummarySerializer::serialize
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
                                "serialize-user-summary"
                        )
                        .uid(
                                "serialize-user-summary"
                        );

        // ============================================================
        // 11. JSON -> Kafka dws_user_summary
        // ============================================================

        userSummaryJsonStream
                .addSink(
                        DwsKafkaSinkFactory
                                .createStringProducer(
                                        KAFKA_BOOTSTRAP_SERVERS,
                                        DWS_USER_SUMMARY_TOPIC
                                )
                )
                .name(
                        "dws-user-summary-kafka-sink"
                )
                .uid(
                        "dws-user-summary-kafka-sink"
                );

        // ============================================================
        // 12. 启动信息
        // ============================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "启动 Realtime DWS User Job"
        );

        System.out.println(
                "Source Topic："
                        + DWD_ORDER_INFO_TOPIC
        );

        System.out.println(
                "Output Topic："
                        + DWS_USER_SUMMARY_TOPIC
        );

        System.out.println(
                "Consumer Group："
                        + USER_CONSUMER_GROUP
        );

        System.out.println(
                "首次初始化Offset：earliest"
        );

        System.out.println(
                "Bootstrap历史边界："
                        + realtimeStartOffsets
        );

        System.out.println(
                "历史范围：只恢复previous-order-state，不产生UserDelta"
        );

        System.out.println(
                "实时范围：Bootstrap边界之后开始产生UserDelta"
        );

        System.out.println(
                "State Key：orderId"
        );

        System.out.println(
                "DWS统计粒度：userId + 1分钟"
        );

        System.out.println(
                "USER_ORDER时间：createTime"
        );

        System.out.println(
                "USER_PAY时间：payTime"
        );

        System.out.println(
                "Event Time：UserDelta.eventTs"
        );

        System.out.println(
                "Watermark最大乱序时间：5秒（当前开发参数）"
        );

        System.out.println(
                "指标：orderCount、orderShopCount、orderAmount、"
                        + "paidOrderCount、paidShopCount、paidAmount"
        );

        System.out.println(
                "Checkpoint目录："
                        + USER_CHECKPOINT_PATH
        );

        System.out.println(
                "========================================"
        );

        // ============================================================
        // 13. 启动User Job
        // ============================================================

        env.execute(
                "Realtime DWS User Job"
        );
    }
}
