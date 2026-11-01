// 文件路径：src/main/java/org/example/GoodsApp.java
package org.example;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsDelta;
import org.example.bean.GoodsSummary;

import org.example.process.DwdOrderGoodsDeserializer;
import org.example.process.DwdOrderInfoDeserializer;
import org.example.process.DwsKafkaSinkFactory;
import org.example.process.FlinkCheckpointConfig;
import org.example.process.GoodsAggregateFunction;
import org.example.process.GoodsJoinProcessFunction;
import org.example.process.GoodsRuntimeMode;
import org.example.process.GoodsSummarySerializer;
import org.example.process.GoodsWindowProcessFunction;

import java.time.Duration;
import java.util.Properties;

/**
 * 实时数仓 DWS 商品主题主程序。
 *
 * 完整链路：
 *
 * dwd_order_info ───────────┐
 *                           │
 *                           ├── keyBy(orderId)
 *                           │
 * dwd_order_goods ──────────┘
 *              ↓
 *     GoodsJoinProcessFunction
 *              ↓
 *          GoodsDelta
 *              ↓
 *     Event Time + Watermark
 *              ↓
 *        keyBy(goodsId)
 *              ↓
 *  1分钟 Tumbling Event Time Window
 *              ↓
 *    GoodsAggregateFunction
 *              +
 *  GoodsWindowProcessFunction
 *              ↓
 *         GoodsSummary
 *              ↓
 *             JSON
 *              ↓
 *            Kafka
 *              ↓
 *      dws_goods_summary
 *
 *
 * 第一版指标：
 *
 * orderCount
 * orderGoodsNum
 * orderAmount
 * paidOrderCount
 * paidGoodsNum
 * paidAmount
 *
 *
 * Event Time规则：
 *
 * GOODS_ORDER
 *      -> order_info.createTime
 *
 * GOODS_PAY
 *      -> order_info.payTime
 *
 * GOODS_UPDATE_CORRECTION
 *      -> order_goods.processTs
 *
 *
 * 注意：
 *
 * Goods Job是独立Stateful Job。
 *
 * 第一次启动时：
 *
 * dwd_order_info
 * dwd_order_goods
 *
 * 两个Source都从earliest开始，
 * 用于完整建立商品主题Join State。
 *
 * 后续正常恢复应从Goods自己的Checkpoint恢复，
 * 不再重新读取全部历史数据。
 */
public class GoodsApp {

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
     * DWD订单商品事实Topic。
     */
    private static final String DWD_ORDER_GOODS_TOPIC =
            "dwd_order_goods";

    /**
     * DWS商品主题结果Topic。
     */
    private static final String DWS_GOODS_SUMMARY_TOPIC =
            "dws_goods_summary";

    /**
     * Goods订单Source独立Consumer Group。
     */
    private static final String GOODS_ORDER_CONSUMER_GROUP =
            "realtime-dws-goods-order-v1";

    /**
     * Goods商品明细Source独立Consumer Group。
     */
    private static final String GOODS_DETAIL_CONSUMER_GROUP =
            "realtime-dws-goods-detail-v1";

    /**
     * Goods独立Checkpoint目录。
     *
     * Trade：
     * hdfs:///flink/checkpoints/dws-trade
     *
     * Goods：
     * hdfs:///flink/checkpoints/dws-goods
     *
     * 两个Stateful Job必须使用不同的Checkpoint根目录。
     */
    private static final String GOODS_CHECKPOINT_PATH =
            "hdfs:///flink/checkpoints/dws-goods";

    /**
     * 当前开发阶段Watermark最大乱序时间。
     *
     * 与Trade第一版保持一致：
     *
     * 5秒。
     *
     * 后续根据实际生产数据乱序程度再调整。
     */
    private static final Duration MAX_OUT_OF_ORDERNESS =
            Duration.ofSeconds(5);

    public static void main(String[] args)
            throws Exception {

        GoodsRuntimeMode runtimeMode =
                GoodsRuntimeMode.fromSystemProperty();

        String restorePath = runtimeMode.validateRestorePath(
                System.getProperty("flink.restore.path")
        );

        // ============================================================
        // 1. Flink执行环境
        // ============================================================

        Configuration executionConfiguration =
                new Configuration();

        if (restorePath != null) {
            executionConfiguration.setString(
                    SavepointConfigOptions.SAVEPOINT_PATH,
                    restorePath
            );
            executionConfiguration.setBoolean(
                    SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
                    false
            );
        }

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment
                        .getExecutionEnvironment(
                                executionConfiguration
                        );

        /*
         * 当前开发阶段并行度1。
         *
         * 方便观察：
         *
         * Join State
         * Event Time
         * Watermark
         * Window
         * Checkpoint
         * Kafka Sink
         */
        env.setParallelism(1);

        // ============================================================
        // 2. Checkpoint
        // ============================================================

        /*
         * GoodsJoinProcessFunction内部包含：
         *
         * ValueState<DwdOrderInfo>
         *
         * MapState<ogId, DwdOrderGoods>
         *
         * MapState<ogId, Boolean> orderEmittedState
         *
         * MapState<ogId, Boolean> payEmittedState
         *
         * 所以Goods必须开启Checkpoint。
         *
         * 这里不能使用：
         *
         * FlinkCheckpointConfig.configure(env);
         *
         * 因为单参数版本默认使用Trade的Checkpoint目录。
         *
         * Goods必须明确指定自己的Checkpoint目录。
         */
        FlinkCheckpointConfig.configure(
                env,
                GOODS_CHECKPOINT_PATH
        );

        // ============================================================
        // 3. Goods订单Source Kafka配置
        // ============================================================

        Properties orderKafkaProperties =
                createKafkaProperties(
                        GOODS_ORDER_CONSUMER_GROUP
                );

        FlinkKafkaConsumer<String> orderKafkaConsumer =
                new FlinkKafkaConsumer<>(
                        DWD_ORDER_INFO_TOPIC,
                        new SimpleStringSchema(),
                        orderKafkaProperties
                );

        /*
         * Goods第一次初始化必须完整读取order_info历史。
         *
         * 如果以后从Checkpoint恢复，
         * Checkpoint中的Source State优先参与恢复。
         */
        orderKafkaConsumer.setStartFromEarliest();

        // ============================================================
        // 4. Goods明细Source Kafka配置
        // ============================================================

        Properties goodsKafkaProperties =
                createKafkaProperties(
                        GOODS_DETAIL_CONSUMER_GROUP
                );

        FlinkKafkaConsumer<String> goodsKafkaConsumer =
                new FlinkKafkaConsumer<>(
                        DWD_ORDER_GOODS_TOPIC,
                        new SimpleStringSchema(),
                        goodsKafkaProperties
                );

        /*
         * 与order_info保持相同初始化策略：
         *
         * 第一次从earliest开始。
         */
        goodsKafkaConsumer.setStartFromEarliest();

        // ============================================================
        // 5. Kafka Source
        // ============================================================

        DataStream<String> orderKafkaStream =
                env.addSource(
                                orderKafkaConsumer
                        )
                        .name(
                                "goods-order-info-kafka-source"
                        )
                        /*
                         * Goods自己的稳定UID。
                         *
                         * 后续Goods Checkpoint产生以后，
                         * 不要随意修改。
                         */
                        .uid(
                                "goods-order-info-kafka-source"
                        );

        DataStream<String> goodsKafkaStream =
                env.addSource(
                                goodsKafkaConsumer
                        )
                        .name(
                                "goods-order-goods-kafka-source"
                        )
                        .uid(
                                "goods-order-goods-kafka-source"
                        );

        // ============================================================
        // 6. JSON -> DwdOrderInfo
        // ============================================================

        SingleOutputStreamOperator<DwdOrderInfo> orderStream =
                orderKafkaStream
                        .map(
                                DwdOrderInfoDeserializer::deserialize
                        )
                        .returns(
                                DwdOrderInfo.class
                        )
                        .filter(
                                order ->
                                        order != null
                                                && order.getOrderId() != null
                                                && order.getOrderId() > 0
                        )
                        .name(
                                "goods-deserialize-order-info"
                        )
                        .uid(
                                "goods-deserialize-order-info"
                        );

        // ============================================================
        // 7. JSON -> DwdOrderGoods
        // ============================================================

        SingleOutputStreamOperator<DwdOrderGoods> goodsStream =
                goodsKafkaStream
                        .map(
                                DwdOrderGoodsDeserializer::deserialize
                        )
                        .returns(
                                DwdOrderGoods.class
                        )
                        .filter(
                                goods ->
                                        goods != null
                                                && goods.getOrderId() != null
                                                && goods.getOrderId() > 0
                                                && goods.getOgId() != null
                                                && goods.getOgId() > 0
                                                && goods.getGoodsId() != null
                                                && goods.getGoodsId() > 0
                        )
                        .name(
                                "goods-deserialize-order-goods"
                        )
                        .uid(
                                "goods-deserialize-order-goods"
                        );

        // ============================================================
        // 8. 双流按照orderId连接
        // ============================================================

        /*
         * input1：
         *
         * DwdOrderInfo
         *
         * input2：
         *
         * DwdOrderGoods
         *
         * 两边必须使用相同的orderId作为Key。
         */
        ConnectedStreams<DwdOrderInfo, DwdOrderGoods>
                connectedStreams =
                orderStream
                        .keyBy(
                                DwdOrderInfo::getOrderId
                        )
                        .connect(
                                goodsStream.keyBy(
                                        DwdOrderGoods::getOrderId
                                )
                        );

        // ============================================================
        // 9. Order + OrderGoods -> GoodsDelta
        // ============================================================

        SingleOutputStreamOperator<GoodsDelta>
                goodsDeltaStream =
                connectedStreams
                        .process(
                                new GoodsJoinProcessFunction(
                                        runtimeMode
                                )
                        )
                        .name(
                                "goods-order-goods-join"
                        )
                        /*
                         * 这是Goods最重要的Stateful Operator之一。
                         *
                         * 后续不要随意修改UID。
                         */
                        .uid(
                                "goods-order-goods-join"
                        );

        // ============================================================
        // 10. Event Time + Watermark
        // ============================================================

        /*
         * GoodsDeltaCalculator已经确定：
         *
         * GOODS_ORDER
         * eventTs = createTime
         *
         * GOODS_PAY
         * eventTs = payTime
         *
         * GOODS_UPDATE_CORRECTION
         * eventTs = order_goods.processTs
         */
        WatermarkStrategy<GoodsDelta>
                goodsWatermarkStrategy =
                WatermarkStrategy
                        .<GoodsDelta>forBoundedOutOfOrderness(
                                MAX_OUT_OF_ORDERNESS
                        )
                        .withTimestampAssigner(
                                new SerializableTimestampAssigner<GoodsDelta>() {

                                    @Override
                                    public long extractTimestamp(
                                            GoodsDelta element,
                                            long recordTimestamp) {

                                        return element.getEventTs();
                                    }
                                }
                        );

        SingleOutputStreamOperator<GoodsDelta>
                goodsEventTimeStream =
                goodsDeltaStream
                        .assignTimestampsAndWatermarks(
                                goodsWatermarkStrategy
                        )
                        .name(
                                "goods-event-time-watermark"
                        )
                        .uid(
                                "goods-event-time-watermark"
                        );

        // ============================================================
        // 11. 商品维度1分钟窗口
        // ============================================================

        /*
         * 聚合粒度：
         *
         * goodsId + 1分钟
         *
         * 第一版指标：
         *
         * orderCount
         * orderGoodsNum
         * orderAmount
         *
         * paidOrderCount
         * paidGoodsNum
         * paidAmount
         */
        SingleOutputStreamOperator<GoodsSummary>
                goodsSummaryStream =
                goodsEventTimeStream
                        .keyBy(
                                GoodsDelta::getGoodsId
                        )
                        .window(
                                TumblingEventTimeWindows.of(
                                        Time.minutes(1)
                                )
                        )
                        .aggregate(
                                new GoodsAggregateFunction(),
                                new GoodsWindowProcessFunction()
                        )
                        .name(
                                "one-minute-goods-summary"
                        )
                        /*
                         * Goods第二个关键Stateful Operator。
                         */
                        .uid(
                                "one-minute-goods-summary"
                        );

        // ============================================================
        // 12. 控制台打印
        // ============================================================

        /*
         * 开发阶段保留。
         *
         * 后面可以与Kafka结果进行对照。
         */
        goodsSummaryStream.print(
                "DWS-GOODS-SUMMARY"
        );

        // ============================================================
        // 13. GoodsSummary -> JSON
        // ============================================================

        SingleOutputStreamOperator<String>
                goodsSummaryJsonStream =
                goodsSummaryStream
                        .map(
                                GoodsSummarySerializer::serialize
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
                                "serialize-goods-summary"
                        )
                        .uid(
                                "serialize-goods-summary"
                        );

        // ============================================================
        // 14. JSON -> Kafka
        // ============================================================

        goodsSummaryJsonStream
                .addSink(
                        DwsKafkaSinkFactory
                                .createStringProducer(
                                        KAFKA_BOOTSTRAP_SERVERS,
                                        DWS_GOODS_SUMMARY_TOPIC
                                )
                )
                .name(
                        "dws-goods-summary-kafka-sink"
                )
                .uid(
                        "dws-goods-summary-kafka-sink"
                );

        // ============================================================
        // 15. 启动信息
        // ============================================================

        System.out.println(
                "========================================"
        );

        System.out.println(
                "启动 Realtime DWS Goods Job"
        );

        System.out.println(
                "Runtime Mode：" + runtimeMode
        );

        if (runtimeMode == GoodsRuntimeMode.WARMUP) {
            System.out.println(
                    "WARMUP：只恢复Join State，不输出GoodsDelta"
            );
        } else {
            System.out.println(
                    "REALTIME：必须从WARMUP生成的Checkpoint恢复"
            );
            System.out.println(
                    "Restore Path：" + restorePath
            );
        }

        System.out.println(
                "Order Source Topic：" +
                        DWD_ORDER_INFO_TOPIC
        );

        System.out.println(
                "Goods Source Topic：" +
                        DWD_ORDER_GOODS_TOPIC
        );

        System.out.println(
                "Output Topic：" +
                        DWS_GOODS_SUMMARY_TOPIC
        );

        System.out.println(
                "Order Consumer Group：" +
                        GOODS_ORDER_CONSUMER_GROUP
        );

        System.out.println(
                "Goods Consumer Group：" +
                        GOODS_DETAIL_CONSUMER_GROUP
        );

        System.out.println(
                "首次启动Offset：两个Source均从earliest开始"
        );

        System.out.println(
                "Join Key：orderId"
        );

        System.out.println(
                "DWS统计粒度：goodsId + 1分钟"
        );

        System.out.println(
                "Event Time：GoodsDelta.eventTs"
        );

        System.out.println(
                "Watermark最大乱序时间：5秒（当前开发参数）"
        );

        System.out.println(
                "指标：orderCount、orderGoodsNum、orderAmount、"
                        + "paidOrderCount、paidGoodsNum、paidAmount"
        );

        System.out.println(
                "Checkpoint目录：" +
                        GOODS_CHECKPOINT_PATH
        );

        System.out.println(
                "========================================"
        );

        // ============================================================
        // 16. 启动Goods Job
        // ============================================================

        env.execute(
                "Realtime DWS Goods Job"
        );
    }

    /**
     * 创建Kafka Consumer公共配置。
     */
    private static Properties createKafkaProperties(
            String consumerGroup) {

        Properties properties =
                new Properties();

        properties.setProperty(
                "bootstrap.servers",
                KAFKA_BOOTSTRAP_SERVERS
        );

        properties.setProperty(
                "group.id",
                consumerGroup
        );

        /*
         * Flink通过Checkpoint管理Source恢复位置。
         */
        properties.setProperty(
                "enable.auto.commit",
                "false"
        );

        /*
         * 没有Checkpoint状态时，
         * 从Kafka最早可用Offset开始。
         */
        properties.setProperty(
                "auto.offset.reset",
                "earliest"
        );

        return properties;
    }
}
