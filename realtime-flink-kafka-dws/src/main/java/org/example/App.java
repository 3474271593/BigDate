package org.example;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import org.example.bean.TradeDelta;
import org.example.bean.TradeSourceRecord;
import org.example.bean.TradeSummary;
import org.example.process.DwsKafkaSinkFactory;
import org.example.process.FlinkCheckpointConfig;
import org.example.process.TradeAggregateFunction;
import org.example.process.TradeBootstrapConfig;
import org.example.process.TradeLateDataConfig;
import org.example.process.TradeSourceRecordDeserializer;
import org.example.process.TradeSummarySerializer;
import org.example.process.TradeWarmupProcessFunction;
import org.example.process.TradeWindowProcessFunction;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * 实时数仓DWS交易主题主程序。
 *
 * 当前完整链路：
 *
 * Kafka：dwd_order_info
 *          ↓
 * JSON反序列化
 *          ↓
 * DwdOrderInfo
 *          ↓
 * 基础过滤
 *          ↓
 * keyBy(orderId)
 *          ↓
 * TradeChangeProcessFunction
 *          ↓
 * ValueState维护订单历史状态
 *          ↓
 * TradeDelta
 *      ↙             ↘
 * ORDER_CREATE      ORDER_PAY
 * createTime        payTime
 *      ↘             ↙
 *          eventTs
 *             ↓
 *      Event Time + Watermark
 *             ↓
 *        keyBy("TRADE")
 *             ↓
 *   1分钟Tumbling Event Time Window
 *        ↙               ↘
 *   正常窗口数据          迟到数据
 *        ↓                 ↓
 * TradeSummary        Side Output
 *        ↓                 ↓
 * JSON                DWS-LATE-DATA
 *        ↓
 * Kafka
 *        ↓
 * dws_trade_summary
 *
 * 当前阶段已完成：
 *
 * 1. CDC订单状态识别；
 * 2. ValueState订单历史状态维护；
 * 3. Event Time；
 * 4. Watermark；
 * 5. 1分钟滚动窗口；
 * 6. Late Data Side Output；
 * 7. Checkpoint恢复；
 * 8. Stateful Operator兼容恢复；
 * 9. TradeSummary JSON序列化；
 * 10. TradeSummary写入Kafka DWS Topic。
 *
 * 当前开发初始化策略：
 *
 * 不恢复Checkpoint时，
 * dwd_order_info从Kafka earliest开始完整重建Trade State。
 *
 * 如果以后显式从Checkpoint恢复，
 * Checkpoint中的Kafka Source State优先参与恢复。
 */
public class App {

    /**
     * Kafka Broker地址。
     */
    private static final String KAFKA_BOOTSTRAP_SERVERS =
            "master:9092,slave1:9092,slave2:9092";

    /**
     * DWD订单事实Topic。
     */
    private static final String DWD_ORDER_TOPIC =
            "dwd_order_info";

    /**
     * DWS平台级每分钟交易汇总Topic。
     */
    private static final String DWS_TRADE_SUMMARY_TOPIC =
            "dws_trade_summary";

    /**
     * DWS Consumer Group。
     */
    private static final String DWS_CONSUMER_GROUP =
            "realtime-dws-trade-v2";

    /**
     * 当前开发阶段Watermark允许的最大乱序时间。
     *
     * 5秒目前属于开发阶段参数。
     *
     * 后续需要根据实际业务数据的Event Time
     * 乱序程度进行调整。
     */
    private static final Duration MAX_OUT_OF_ORDERNESS =
            Duration.ofSeconds(5);

    public static void main(String[] args) throws Exception {

        // ============================================================
        // 1. 创建Flink执行环境
        // ============================================================

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        /*
         * 当前开发阶段使用并行度1。
         *
         * 方便：
         *
         * 1. 观察输出；
         * 2. 验证Keyed State；
         * 3. 验证Checkpoint；
         * 4. 验证Event Time；
         * 5. 验证Watermark；
         * 6. 验证Window；
         * 7. 验证Kafka Sink。
         */
        env.setParallelism(1);

        // ============================================================
        // 2. 开启Checkpoint
        // ============================================================

        /*
         * 当前已经完成实际验证：
         *
         * Kafka Source State
         *        +
         * ValueState
         *        +
         * Window State / Timer
         *
         * 可以通过Checkpoint进行恢复。
         *
         * 注意：
         *
         * IDEA手动Stop以后重新Run属于新的Job。
         *
         * 如果需要从指定历史Checkpoint恢复，
         * 应使用Flink CLI：
         *
         * CliFrontend run
         *      -s <checkpoint>
         *
         * 进行显式恢复。
         */
        FlinkCheckpointConfig.configure(env);

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
                DWS_CONSUMER_GROUP
        );

        /*
         * Kafka Offset主要由Flink Checkpoint状态
         * 参与故障恢复。
         *
         * 不依赖Kafka Consumer自动提交。
         */
        kafkaProperties.setProperty(
                "enable.auto.commit",
                "false"
        );

        /*
         * 当不存在可恢复Checkpoint状态时，
         * Offset Reset的兜底策略。
         */
        kafkaProperties.setProperty(
                "auto.offset.reset",
                "earliest"
        );

        // ============================================================
        // 4. 创建Kafka Source
        // ============================================================

        FlinkKafkaConsumer<TradeSourceRecord> kafkaConsumer =
                new FlinkKafkaConsumer<>(
                        DWD_ORDER_TOPIC,
                        new TradeSourceRecordDeserializer(),
                        kafkaProperties
                );

        // ============================================================
        // 5. 首次初始化Offset：earliest
        // ============================================================

        /*
         * 当前开发阶段采用全量初始化策略：
         *
         * 当Job不是从Checkpoint恢复时，
         * 从dwd_order_info最早可用Offset开始消费。
         *
         * 这样可以完整重建：
         *
         * Kafka Source进度
         *        +
         * Trade ValueState
         *        +
         * Event Time Window State
         *
         * 如果以后通过：
         *
         * -s <checkpoint>
         *
         * 显式恢复Job，
         *
         * Checkpoint中的Kafka Source State
         * 会参与恢复，不需要重新读取全部历史数据。
         */
        kafkaConsumer.setStartFromEarliest();

        // ============================================================
        // 6. 消费DWD Kafka数据
        // ============================================================

        /*
         * 重要：
         *
         * 下面的setUidHash来自历史Checkpoint中的Operator ID。
         *
         * 虽然本次选择全量重建，
         * 仍然保留这些稳定Operator ID，
         * 避免无意义地改变已经验证过的Job拓扑标识。
         *
         * 不要随意修改或删除。
         */
        DataStream<TradeSourceRecord> kafkaStream =
                env.addSource(kafkaConsumer)
                        .name(
                                "dwd-order-info-kafka-source"
                        )
                        .setUidHash(
                                "cbc357ccb763df2852fee8c4fc7d55f2"
                        );

        // ============================================================
        // 7. 过滤无效订单，同时保留Kafka partition/offset
        // ============================================================

        SingleOutputStreamOperator<TradeSourceRecord> orderStream =
                kafkaStream
                        .filter(
                                record ->
                                        record != null
                                                && record.getOrder() != null
                                                && record.getOrder().getOrderId() != null
                                                && record.getOrder().getOrderId() > 0
                        )
                        .name(
                                "filter-dwd-order-with-kafka-position"
                        );

        // ============================================================
        // 8. CDC订单状态 -> TradeDelta
        // ============================================================

        /*
         * 按orderId维护ValueState。
         *
         * ------------------------------------------------------------
         *
         * 第一次看到订单：
         *
         * ORDER_CREATE
         * eventTime = createTime
         *
         * ------------------------------------------------------------
         *
         * 第一次看到的订单已经支付：
         *
         * ORDER_CREATE
         * eventTime = createTime
         *
         * +
         *
         * ORDER_PAY
         * eventTime = payTime
         *
         * ------------------------------------------------------------
         *
         * 未支付 -> 已支付：
         *
         * ORDER_PAY
         * eventTime = payTime
         *
         * ------------------------------------------------------------
         *
         * 普通UPDATE：
         *
         * 不产生TradeDelta。
         */
        Map<Integer, Long> realtimeStartOffsets =
                TradeBootstrapConfig.historyEndOffsets();

        SingleOutputStreamOperator<TradeDelta> tradeDeltaStream =
                orderStream
                        .keyBy(
                                record -> record.getOrder().getOrderId()
                        )
                        .process(
                                new TradeWarmupProcessFunction(
                                        realtimeStartOffsets
                                )
                        )
                        .name(
                                "trade-change-process"
                        )
                        .setUidHash(
                                "c27dcf7b54ef6bfd6cff02ca8870b681"
                        );

        // ============================================================
        // 9. 分配Event Time和Watermark
        // ============================================================

        /*
         * TradeDeltaCalculator已经保证：
         *
         * ORDER_CREATE
         *      ↓
         * eventTs = createTime
         *
         * ORDER_PAY
         *      ↓
         * eventTs = payTime
         *
         * 因此从这里开始，
         * 下游统一使用TradeDelta.eventTs。
         */
        WatermarkStrategy<TradeDelta> watermarkStrategy =
                WatermarkStrategy
                        .<TradeDelta>forBoundedOutOfOrderness(
                                MAX_OUT_OF_ORDERNESS
                        )
                        .withTimestampAssigner(
                                new SerializableTimestampAssigner<TradeDelta>() {

                                    @Override
                                    public long extractTimestamp(
                                            TradeDelta element,
                                            long recordTimestamp) {

                                        /*
                                         * eventTs单位为毫秒。
                                         */
                                        return element.getEventTs();
                                    }
                                }
                        );

        SingleOutputStreamOperator<TradeDelta> tradeEventTimeStream =
                tradeDeltaStream
                        .assignTimestampsAndWatermarks(
                                watermarkStrategy
                        )
                        .name(
                                "trade-event-time-watermark"
                        );

        // ============================================================
        // 10. 平台级交易主题
        // ============================================================

        /*
         * 当前DWS第一张汇总结果：
         *
         * 平台级每分钟交易汇总。
         *
         * 所以当前所有TradeDelta：
         *
         * keyBy("TRADE")
         *
         * 后续：
         *
         * 店铺主题：
         * keyBy(shopId)
         *
         * 商品主题：
         * 结合dwd_order_goods进一步设计。
         */

        // ============================================================
        // 11. 1分钟Tumbling Event Time Window
        // ============================================================

        /*
         * 每个窗口长度为1分钟。
         *
         * 例如：
         *
         * 17:28:00 ~ 17:29:00
         * 17:29:00 ~ 17:30:00
         *
         * ORDER_CREATE使用createTime。
         *
         * ORDER_PAY使用payTime。
         *
         * 如果TradeDelta到达时，
         * 对应窗口已经被Watermark关闭，
         * 则进入Late Data Side Output。
         *
         * ------------------------------------------------------------
         *
         * 重要：
         *
         * one-minute-trade-summary的setUidHash
         * 对应历史Checkpoint Window Operator ID。
         *
         * 不要随意修改。
         */
        SingleOutputStreamOperator<TradeSummary> tradeSummaryStream =
                tradeEventTimeStream
                        .keyBy(
                                delta -> "TRADE"
                        )
                        .window(
                                TumblingEventTimeWindows.of(
                                        Time.minutes(1)
                                )
                        )
                        .sideOutputLateData(
                                TradeLateDataConfig.getLateDataOutputTag()
                        )
                        .aggregate(
                                new TradeAggregateFunction(),
                                new TradeWindowProcessFunction()
                        )
                        .name(
                                "one-minute-trade-summary"
                        )
                        .setUidHash(
                                "3e124ac022a3afbc05857c5ecbc241be"
                        );

        // ============================================================
        // 12. 打印DWS交易窗口汇总
        // ============================================================

        /*
         * 开发阶段继续保留控制台输出。
         *
         * 用于与Kafka dws_trade_summary中的结果
         * 进行对比验证。
         */
        tradeSummaryStream.print(
                "DWS-TRADE-SUMMARY"
        );

        // ============================================================
        // 13. TradeSummary -> JSON
        // ============================================================

        /*
         * TradeSummary
         *      ↓
         * TradeSummarySerializer
         *      ↓
         * JSON String
         */
        SingleOutputStreamOperator<String> tradeSummaryJsonStream =
                tradeSummaryStream
                        .map(
                                TradeSummarySerializer::serialize
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
                                "serialize-trade-summary"
                        );

        // ============================================================
        // 14. JSON -> Kafka dws_trade_summary
        // ============================================================

        /*
         * 正式将DWS交易窗口结果写入Kafka。
         *
         * 当前链路：
         *
         * TradeSummary
         *      ↓
         * JSON
         *      ↓
         * FlinkKafkaProducer
         *      ↓
         * dws_trade_summary
         *
         * 当前阶段先完成业务链路验证。
         *
         * Kafka Sink端到端Exactly-Once
         * 后续单独验证。
         */
        tradeSummaryJsonStream
                .addSink(
                        DwsKafkaSinkFactory.createStringProducer(
                                KAFKA_BOOTSTRAP_SERVERS,
                                DWS_TRADE_SUMMARY_TOPIC
                        )
                )
                .name(
                        "dws-trade-summary-kafka-sink"
                );

        // ============================================================
        // 15. 获取并打印迟到数据Side Output
        // ============================================================

        /*
         * 当前不配置allowedLateness。
         *
         * 当TradeDelta所属窗口已经被Watermark关闭，
         * 数据进入Side Output。
         *
         * 当前开发阶段继续打印：
         *
         * DWS-LATE-DATA
         */
        DataStream<TradeDelta> lateDataStream =
                tradeSummaryStream.getSideOutput(
                        TradeLateDataConfig.getLateDataOutputTag()
                );

        lateDataStream.print(
                "DWS-LATE-DATA"
        );

        // ============================================================
        // 16. 输出启动信息
        // ============================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "启动Realtime DWS Trade Job"
        );

        System.out.println(
                "DWD Kafka Topic：" + DWD_ORDER_TOPIC
        );

        System.out.println(
                "DWS Kafka Topic：" + DWS_TRADE_SUMMARY_TOPIC
        );

        System.out.println(
                "Consumer Group：" + DWS_CONSUMER_GROUP
        );

        System.out.println(
                "首次初始化Offset：earliest"
        );

        System.out.println(
                "Checkpoint：已开启"
        );

        System.out.println(
                "Checkpoint间隔：30秒"
        );

        System.out.println(
                "CDC状态识别：ValueState<orderId>"
        );

        System.out.println(
                "业务事件模型：TradeDelta"
        );

        System.out.println(
                "ORDER_CREATE时间：createTime"
        );

        System.out.println(
                "ORDER_PAY时间：payTime"
        );

        System.out.println(
                "Event Time：TradeDelta.eventTs"
        );

        System.out.println(
                "Watermark最大乱序时间：5秒（当前开发参数）"
        );

        System.out.println(
                "窗口类型：Tumbling Event Time Window"
        );

        System.out.println(
                "窗口大小：1分钟"
        );

        System.out.println(
                "DWS统计粒度：平台级每分钟交易汇总"
        );

        System.out.println(
                "DWS输出模型：TradeSummary"
        );

        System.out.println(
                "DWS指标：orderCount、paidOrderCount、paidAmount"
        );

        System.out.println(
                "正常窗口控制台输出：DWS-TRADE-SUMMARY"
        );

        System.out.println(
                "迟到数据控制台输出：DWS-LATE-DATA"
        );

        System.out.println(
                "Kafka输出：TradeSummary -> JSON -> "
                        + DWS_TRADE_SUMMARY_TOPIC
        );

        System.out.println(
                "当前链路：DWD -> TradeDelta -> Event Time -> Window -> TradeSummary -> Kafka"
        );

        System.out.println(
                "========================================"
        );

        // ============================================================
        // 17. 启动Flink Job
        // ============================================================

        env.execute(
                "Realtime DWS Trade Job"
        );
    }
}
