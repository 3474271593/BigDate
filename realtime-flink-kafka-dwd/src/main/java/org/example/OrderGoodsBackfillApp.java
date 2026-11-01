package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.util.Collector;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderGoods;
import org.example.process.DwdKafkaSerializer;
import org.example.process.GoodsDimRedisEnrichmentFunction;
import org.example.process.OrderGoodsBackfillFilter;
import org.example.process.OrderGoodsDwdProcessFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
//历史数据回灌程序
/**
 * order_goods历史数据安全重建程序。
 *
 * 数据链路：
 *
 * ods_shop_db
 *      ↓
 * 读取历史order_goods bootstrap
 *      ↓
 * OrderGoodsDwdProcessFunction
 *      │
 *      ├── 核心事实字段非法
 *      │       ↓
 *      │   DirtyData
 *      │       ↓
 *      │   dwd_dirty_data_rebuild_test
 *      │
 *      └── 正常交易事实
 *              ↓
 *      GoodsDimRedisEnrichmentFunction
 *              ↓
 *      goodsName为空时查询Redis商品DIM
 *              ↓
 *      dwd_order_goods_rebuild_test
 *
 * 注意：
 *
 * 1. 只处理table=order_goods
 * 2. 只处理type=bootstrap
 * 3. 不处理orders
 * 4. 不处理INSERT/UPDATE/DELETE
 * 5. 使用独立Consumer Group
 * 6. 三个Kafka分区明确从offset=0开始
 * 7. 当前为安全重建任务，不写正式DWD Topic
 */
public class OrderGoodsBackfillApp {

    /**
     * Kafka Broker地址。
     */
    private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.13.232:9092";

    /**
     * ODS原始数据Topic。
     */
    private static final String ODS_TOPIC = "ods_shop_db";

    /**
     * 历史重建测试Topic。
     *
     * 注意：
     * 当前不能直接写dwd_order_goods，
     * 否则会与之前已经存在的数据产生重复。
     */
    private static final String DWD_ORDER_GOODS_TOPIC = "dwd_order_goods_rebuild_test";

    /**
     * 历史重建Dirty测试Topic。
     *
     * 不写正式dwd_dirty_data，
     * 避免历史回灌再次污染正式Dirty数据。
     */
    private static final String DWD_DIRTY_TOPIC = "dwd_dirty_data_rebuild_test";

    /**
     * 历史回灌专用Consumer Group。
     *
     * 与实时App.java使用的Consumer Group完全独立。
     */
    private static final String BACKFILL_GROUP_ID = "realtime-dwd-order-goods-backfill-v1";

    public static void main(String[] args) throws Exception {

        // ============================================================
        // 1. 创建Flink执行环境
        // ============================================================

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /*
         * 历史重建阶段使用单并行度，
         * 方便观察、验证和排查数据。
         */
        env.setParallelism(1);

        // ============================================================
        // 2. Kafka Consumer配置
        // ============================================================

        Properties consumerProperties = new Properties();

        consumerProperties.setProperty(
                "bootstrap.servers",
                KAFKA_BOOTSTRAP_SERVERS
        );

        consumerProperties.setProperty(
                "group.id",
                BACKFILL_GROUP_ID
        );

        /*
         * 保留earliest配置。
         *
         * 实际启动位置由下面的
         * setStartFromSpecificOffsets()
         * 明确指定。
         */
        consumerProperties.setProperty(
                "auto.offset.reset",
                "earliest"
        );

        /*
         * 当前历史重建任务没有启用Checkpoint，
         * 暂时继续使用Kafka自动提交Offset。
         */
        consumerProperties.setProperty(
                "enable.auto.commit",
                "true"
        );

        consumerProperties.setProperty(
                "auto.commit.interval.ms",
                "5000"
        );

        // ============================================================
        // 3. 创建Kafka Consumer
        // ============================================================

        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(
                ODS_TOPIC,
                new SimpleStringSchema(),
                consumerProperties
        );

        // ============================================================
        // 4. 明确指定三个Partition从offset=0开始
        // ============================================================

        /*
         * 这是之前已经实际验证成功的历史回灌启动方案。
         *
         * 不改成：
         *
         * setStartFromEarliest()
         *
         * 避免再次触发之前遇到的Partition Offset初始化问题。
         */
        Map<KafkaTopicPartition, Long> specificOffsets = new HashMap<>();

        specificOffsets.put(
                new KafkaTopicPartition(ODS_TOPIC, 0),
                0L
        );

        specificOffsets.put(
                new KafkaTopicPartition(ODS_TOPIC, 1),
                0L
        );

        specificOffsets.put(
                new KafkaTopicPartition(ODS_TOPIC, 2),
                0L
        );

        kafkaConsumer.setStartFromSpecificOffsets(specificOffsets);

        // ============================================================
        // 5. 读取ods_shop_db
        // ============================================================

        DataStream<String> odsStringStream = env
                .addSource(kafkaConsumer)
                .name("Kafka-ODS-Backfill-Source");

        // ============================================================
        // 6. String -> JSONObject
        // ============================================================

        SingleOutputStreamOperator<JSONObject> odsJsonStream = odsStringStream
                .map(JSON::parseObject)
                .name("Parse-Backfill-ODS-JSON");

        // ============================================================
        // 7. 统一bootstrap / Canal CDC数据结构
        // ============================================================

        /*
         * 复用App.java中已经测试通过的normalizeOdsEvent。
         *
         * bootstrap：
         * data = JSONObject
         *
         * Canal CDC：
         * data = JSONArray
         *
         * 统一后下游始终按照单条业务记录处理。
         */
        SingleOutputStreamOperator<JSONObject> normalizedStream = odsJsonStream
                .flatMap(new FlatMapFunction<JSONObject, JSONObject>() {

                    @Override
                    public void flatMap(JSONObject odsJson, Collector<JSONObject> out) {

                        List<JSONObject> events = App.normalizeOdsEvent(odsJson);

                        for (JSONObject event : events) {
                            out.collect(event);
                        }
                    }
                })
                .name("Normalize-Backfill-ODS");

        // ============================================================
        // 8. 只保留order_goods bootstrap历史数据
        // ============================================================

        /*
         * 只处理：
         *
         * table = order_goods
         * type = bootstrap
         *
         * 不处理：
         *
         * orders
         * INSERT
         * UPDATE
         * DELETE
         */
        SingleOutputStreamOperator<JSONObject> orderGoodsBootstrapStream = normalizedStream
                .filter(OrderGoodsBackfillFilter::shouldBackfill)
                .name("Filter-Order-Goods-Bootstrap");

        // ============================================================
        // 9. order_goods进入DWD基础清洗
        // ============================================================

        /*
         * 当前硬Dirty规则：
         *
         * ogId <= 0
         * orderId <= 0
         * goodsId <= 0
         * goodsNum <= 0
         * goodsPrice非法
         *
         * goodsName为空不再判Dirty。
         */
        SingleOutputStreamOperator<DwdOrderGoods> dwdOrderGoodsStream = orderGoodsBootstrapStream
                .process(new OrderGoodsDwdProcessFunction())
                .name("Backfill-DWD-Order-Goods");

        // ============================================================
        // 10. 获取硬错误Dirty Side Output
        // ============================================================

        DataStream<DirtyData> dirtyStream = dwdOrderGoodsStream
                .getSideOutput(OrderGoodsDwdProcessFunction.DIRTY_TAG);

        // ============================================================
        // 11. Redis商品DIM补全
        // ============================================================

        /*
         * goodsName有值：
         *
         * 保留订单商品快照，
         * 不访问Redis，不覆盖原值。
         *
         * goodsName为空：
         *
         * 根据goodsId查询：
         *
         * HGET dim:goods:<goodsId> goodsName
         *
         * 查询成功：
         * 补全goodsName。
         *
         * DIM不存在：
         * 保留交易事实，
         * qualityWarning = GOODS_NAME_DIM_MISSING。
         *
         * Redis查询失败：
         * 保留交易事实，
         * qualityWarning = GOODS_NAME_DIM_LOOKUP_FAILED。
         */
        SingleOutputStreamOperator<DwdOrderGoods> enrichedOrderGoodsStream = dwdOrderGoodsStream
                .map(new GoodsDimRedisEnrichmentFunction())
                .name("Redis-DIM-Enrich-Backfill-Order-Goods");

        // ============================================================
        // 12. 补全后的DwdOrderGoods -> JSON
        // ============================================================

        SingleOutputStreamOperator<String> orderGoodsJsonStream = enrichedOrderGoodsStream
                .map(DwdKafkaSerializer::serializeOrderGoods)
                .name("Serialize-Backfill-Order-Goods");

        // ============================================================
        // 13. DirtyData -> JSON
        // ============================================================

        SingleOutputStreamOperator<String> dirtyJsonStream = dirtyStream
                .map(DwdKafkaSerializer::serializeDirtyData)
                .name("Serialize-Backfill-Dirty");

        // ============================================================
        // 14. Kafka Producer配置
        // ============================================================

        Properties producerProperties = new Properties();

        producerProperties.setProperty(
                "bootstrap.servers",
                KAFKA_BOOTSTRAP_SERVERS
        );

        // ============================================================
        // 15. 创建DWD历史重建Producer
        // ============================================================

        FlinkKafkaProducer<String> orderGoodsProducer = new FlinkKafkaProducer<>(
                DWD_ORDER_GOODS_TOPIC,
                new SimpleStringSchema(),
                producerProperties
        );

        // ============================================================
        // 16. 创建Dirty历史重建Producer
        // ============================================================

        FlinkKafkaProducer<String> dirtyProducer = new FlinkKafkaProducer<>(
                DWD_DIRTY_TOPIC,
                new SimpleStringSchema(),
                producerProperties
        );

        // ============================================================
        // 17. 正常数据写入临时DWD Topic
        // ============================================================

        orderGoodsJsonStream
                .addSink(orderGoodsProducer)
                .name("Kafka-Backfill-Order-Goods-Rebuild-Sink");

        // ============================================================
        // 18. Dirty写入临时Dirty Topic
        // ============================================================

        dirtyJsonStream
                .addSink(dirtyProducer)
                .name("Kafka-Backfill-Dirty-Rebuild-Sink");

        // ============================================================
        // 19. 启动历史重建任务
        // ============================================================

        env.execute("Order-Goods-Historical-Rebuild-Test");
    }
}