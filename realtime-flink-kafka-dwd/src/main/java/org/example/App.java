package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Collector;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.process.DwdKafkaSerializer;
import org.example.process.FlinkCheckpointConfig;
import org.example.process.GoodsDimRedisEnrichmentFunction;
import org.example.process.OrderDwdProcessFunction;
import org.example.process.OrderGoodsDwdProcessFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 实时数仓DWD处理程序。
 *
 * 数据链路：
 *
 * Kafka ods_shop_db
 *      ↓
 * Flink
 *      ↓
 * ODS结构统一
 *      ↓
 * 按业务表分流
 *
 * orders：
 * OrderDwdProcessFunction
 *      ↓
 * dwd_order_info
 *
 * order_goods：
 * OrderGoodsDwdProcessFunction
 *      ↓
 * GoodsDimRedisEnrichmentFunction
 *      ↓
 * dwd_order_goods
 *
 * Dirty统一写入：
 * dwd_dirty_data
 */
public class App {

    private static final String KAFKA_BOOTSTRAP_SERVERS =
            "master:9092,slave1:9092,slave2:9092";

    private static final String ODS_TOPIC = "ods_shop_db";

    private static final String DWD_ORDER_TOPIC = "dwd_order_info";

    private static final String DWD_ORDER_GOODS_TOPIC = "dwd_order_goods";

    private static final String DWD_DIRTY_TOPIC = "dwd_dirty_data";

    /**
     * 正式DWD Consumer Group。
     *
     * 继续沿用原Group，
     * 避免改变实时任务Kafka Offset。
     */
    private static final String DWD_ORDER_GROUP_ID = "realtime-dwd-order";

    public static void main(String[] args) throws Exception {

        // ============================================================
        // 1. 创建Flink执行环境
        // ============================================================

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /*
         * 当前IDEA本地运行阶段使用单并行度。
         */
        env.setParallelism(1);

        /*
         * 开启Flink Checkpoint。
         *
         * 当前配置：
         * 30秒执行一次Checkpoint；
         * EXACTLY_ONCE模式；
         * Checkpoint超时60秒；
         * 最大并发Checkpoint数量为1；
         * 允许1次Checkpoint失败。
         *
         * Kafka Source消费进度会跟随Checkpoint进行一致性管理。
         */
        FlinkCheckpointConfig.configure(env);

        // ============================================================
        // 2. Kafka Consumer配置
        // ============================================================

        Properties consumerProperties = new Properties();

        consumerProperties.setProperty("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS);
        consumerProperties.setProperty("group.id", DWD_ORDER_GROUP_ID);
        consumerProperties.setProperty("auto.offset.reset", "latest");

        /*
         * 开启Checkpoint后关闭Kafka Consumer自动提交Offset。
         *
         * 不再依赖Kafka Consumer每隔几秒自动提交消费位置，
         * 而是由Flink Checkpoint统一管理Kafka Source消费进度。
         */
        consumerProperties.setProperty("enable.auto.commit", "false");

        // ============================================================
        // 3. 创建Kafka ODS Consumer
        // ============================================================

        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(
                ODS_TOPIC,
                new SimpleStringSchema(),
                consumerProperties
        );

        /*
         * 正式增量消费模式。
         *
         * Group有Offset：
         * 从已有Offset继续。
         *
         * Group没有Offset：
         * 根据auto.offset.reset=latest开始。
         *
         * 注意：
         * 不修改正式Group，
         * 不重置现有Kafka Offset，
         * 不使用setStartFromEarliest()。
         */
        kafkaConsumer.setStartFromGroupOffsets();

        // ============================================================
        // 4. Kafka ODS Source
        // ============================================================

        DataStream<String> odsStringStream = env
                .addSource(kafkaConsumer)
                .name("Kafka-ODS-Source");

        // ============================================================
        // 5. String -> JSONObject
        // ============================================================

        SingleOutputStreamOperator<JSONObject> odsJsonStream = odsStringStream
                .map(JSON::parseObject)
                .name("Parse-ODS-JSON");

        // ============================================================
        // 6. 统一bootstrap与Canal CDC数据结构
        // ============================================================

        SingleOutputStreamOperator<JSONObject> normalizedOdsStream = odsJsonStream
                .flatMap(new FlatMapFunction<JSONObject, JSONObject>() {

                    @Override
                    public void flatMap(JSONObject odsJson, Collector<JSONObject> out) {

                        List<JSONObject> normalizedEvents = normalizeOdsEvent(odsJson);

                        for (JSONObject normalizedEvent : normalizedEvents) {
                            out.collect(normalizedEvent);
                        }
                    }
                })
                .name("Normalize-ODS-Event");

        // ============================================================
        // 7. 过滤orders表
        // ============================================================

        SingleOutputStreamOperator<JSONObject> ordersStream = normalizedOdsStream
                .filter(json -> json != null && "orders".equals(json.getString("table")))
                .name("Filter-Orders");

        // ============================================================
        // 8. orders -> DWD
        // ============================================================

        SingleOutputStreamOperator<DwdOrderInfo> dwdOrderStream = ordersStream
                .process(new OrderDwdProcessFunction())
                .name("DWD-Order-Process");

        // ============================================================
        // 9. 获取orders脏数据
        // ============================================================

        DataStream<DirtyData> orderDirtyStream = dwdOrderStream
                .getSideOutput(OrderDwdProcessFunction.DIRTY_TAG);

        // ============================================================
        // 10. 过滤order_goods表
        // ============================================================

        SingleOutputStreamOperator<JSONObject> orderGoodsStream = normalizedOdsStream
                .filter(json -> json != null && "order_goods".equals(json.getString("table")))
                .name("Filter-Order-Goods");

        // ============================================================
        // 11. order_goods -> DWD基础清洗
        // ============================================================

        SingleOutputStreamOperator<DwdOrderGoods> dwdOrderGoodsStream = orderGoodsStream
                .process(new OrderGoodsDwdProcessFunction())
                .name("DWD-Order-Goods-Process");

        // ============================================================
        // 12. 获取order_goods脏数据
        // ============================================================

        /*
         * 注意：
         *
         * Side Output必须从原始ProcessFunction输出流获取。
         *
         * Redis DIM补全之后的流不再负责Dirty。
         */
        DataStream<DirtyData> orderGoodsDirtyStream = dwdOrderGoodsStream
                .getSideOutput(OrderGoodsDwdProcessFunction.DIRTY_TAG);

        // ============================================================
        // 13. Redis商品DIM补全
        // ============================================================

        /*
         * 只有goodsName为空时才查询Redis。
         *
         * Redis：
         *
         * Key：
         * dim:goods:<goodsId>
         *
         * Field：
         * goodsName
         *
         * 原始goodsName存在：
         * 不查询Redis，不覆盖订单快照。
         *
         * DIM不存在：
         * 保留事实 + GOODS_NAME_DIM_MISSING。
         *
         * Redis查询失败：
         * 保留事实 + GOODS_NAME_DIM_LOOKUP_FAILED。
         */
        SingleOutputStreamOperator<DwdOrderGoods> enrichedOrderGoodsStream = dwdOrderGoodsStream
                .map(new GoodsDimRedisEnrichmentFunction())
                .name("Redis-DIM-Enrich-Order-Goods");

        // ============================================================
        // 14. 合并所有DWD脏数据
        // ============================================================

        DataStream<DirtyData> dirtyStream = orderDirtyStream
                .union(orderGoodsDirtyStream);

        // ============================================================
        // 15. DwdOrderInfo -> JSON
        // ============================================================

        SingleOutputStreamOperator<String> dwdOrderJsonStream = dwdOrderStream
                .map(DwdKafkaSerializer::serializeOrder)
                .name("Serialize-DWD-Order");

        // ============================================================
        // 16. 补全后的DwdOrderGoods -> JSON
        // ============================================================

        /*
         * 注意这里必须使用enrichedOrderGoodsStream，
         * 不能继续使用dwdOrderGoodsStream。
         */
        SingleOutputStreamOperator<String> dwdOrderGoodsJsonStream = enrichedOrderGoodsStream
                .map(DwdKafkaSerializer::serializeOrderGoods)
                .name("Serialize-DWD-Order-Goods");

        // ============================================================
        // 17. DirtyData -> JSON
        // ============================================================

        SingleOutputStreamOperator<String> dirtyJsonStream = dirtyStream
                .map(DwdKafkaSerializer::serializeDirtyData)
                .name("Serialize-DWD-Dirty");

        // ============================================================
        // 18. Kafka Producer配置
        // ============================================================

        Properties producerProperties = new Properties();

        producerProperties.setProperty("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS);

        // ============================================================
        // 19. 创建正常订单Producer
        // ============================================================

        FlinkKafkaProducer<String> orderProducer = new FlinkKafkaProducer<>(
                DWD_ORDER_TOPIC,
                new SimpleStringSchema(),
                producerProperties
        );

        // ============================================================
        // 20. 创建订单商品明细Producer
        // ============================================================

        FlinkKafkaProducer<String> orderGoodsProducer = new FlinkKafkaProducer<>(
                DWD_ORDER_GOODS_TOPIC,
                new SimpleStringSchema(),
                producerProperties
        );

        // ============================================================
        // 21. 创建Dirty Producer
        // ============================================================

        FlinkKafkaProducer<String> dirtyProducer = new FlinkKafkaProducer<>(
                DWD_DIRTY_TOPIC,
                new SimpleStringSchema(),
                producerProperties
        );

        // ============================================================
        // 22. 正常订单写入dwd_order_info
        // ============================================================

        dwdOrderJsonStream
                .addSink(orderProducer)
                .name("Kafka-DWD-Order-Sink");

        // ============================================================
        // 23. 补全后的订单商品明细写入dwd_order_goods
        // ============================================================

        dwdOrderGoodsJsonStream
                .addSink(orderGoodsProducer)
                .name("Kafka-DWD-Order-Goods-Sink");

        // ============================================================
        // 24. Dirty写入dwd_dirty_data
        // ============================================================

        dirtyJsonStream
                .addSink(dirtyProducer)
                .name("Kafka-DWD-Dirty-Sink");

        // ============================================================
        // 25. 启动Flink Job
        // ============================================================

        env.execute("Realtime-DWD-Order-And-OrderGoods");
    }

    /**
     * 将不同来源的ODS消息统一为：
     *
     * {
     *     "table": "...",
     *     "type": "...",
     *     "data": {...},
     *     "old": {...}
     * }
     *
     * 历史bootstrap：
     * data本身就是JSONObject。
     *
     * Canal CDC：
     * data和old都是JSONArray，
     * 按下标拆成独立事件。
     */
    static List<JSONObject> normalizeOdsEvent(JSONObject odsJson) {

        List<JSONObject> result = new ArrayList<>();

        if (odsJson == null) {
            return result;
        }

        Object dataValue = odsJson.get("data");

        // ============================================================
        // 历史bootstrap：data = JSONObject
        // ============================================================

        if (dataValue instanceof JSONObject) {
            result.add(odsJson);
            return result;
        }

        // ============================================================
        // Canal CDC：data = JSONArray
        // ============================================================

        if (!(dataValue instanceof JSONArray)) {
            return result;
        }

        JSONArray dataArray = (JSONArray) dataValue;
        JSONArray oldArray = odsJson.getJSONArray("old");

        for (int i = 0; i < dataArray.size(); i++) {

            JSONObject data = dataArray.getJSONObject(i);

            if (data == null) {
                continue;
            }

            JSONObject normalizedEvent = new JSONObject();

            /*
             * 保留Canal原始元数据。
             */
            normalizedEvent.putAll(odsJson);

            /*
             * data：
             *
             * [{...}]
             *
             * 转换为：
             *
             * {...}
             */
            normalizedEvent.put("data", data);

            /*
             * UPDATE时old与data按照下标对应。
             */
            if (oldArray != null && i < oldArray.size()) {

                JSONObject oldData = oldArray.getJSONObject(i);

                if (oldData != null) {
                    normalizedEvent.put("old", oldData);
                } else {
                    normalizedEvent.remove("old");
                }

            } else {
                normalizedEvent.remove("old");
            }

            result.add(normalizedEvent);
        }

        return result;
    }
}
