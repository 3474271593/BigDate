package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderInfo;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Kafka DWD 数据序列化测试。
 *
 * 当前测试目标：
 *
 * 1. DwdOrderInfo 能否正确转成 JSON
 * 2. DirtyData 能否正确转成 JSON
 *
 * 下一步 Kafka Sink 会直接把这里生成的 JSON
 * 写入对应 Topic。
 */
public class DwdKafkaSerializerTest {

    /**
     * 测试 15：
     *
     * DwdOrderInfo
     *      ↓
     * JSON String
     *
     * 并验证几个核心字段没有丢失。
     */
    @Test
    public void shouldSerializeDwdOrderInfoToJson() {

        // ============================================================
        // 1. 构造一条 DWD 订单
        // ============================================================

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(60049001L);
        order.setOrderNo("6004900157434");
        order.setShopId(100055L);
        order.setUserId(9L);
        order.setOrderStatus(0);

        order.setGoodsMoney(
                new BigDecimal("496227.61")
        );

        order.setDeliverMoney(
                new BigDecimal("10.00")
        );

        order.setTotalMoney(
                new BigDecimal("496237.61")
        );

        order.setIsPay(0);
        order.setCreateTime("2019-09-05 20:14:48");
        order.setOpType("bootstrap");
        order.setProcessTs(1000L);


        // ============================================================
        // 2. 调用待实现的 Kafka 序列化工具
        // ============================================================

        String json =
                DwdKafkaSerializer.serializeOrder(
                        order
                );


        // ============================================================
        // 3. JSON 不能为空
        // ============================================================

        Assert.assertNotNull(
                json
        );


        // ============================================================
        // 4. 再解析回来检查核心字段
        // ============================================================

        JSONObject jsonObject =
                JSON.parseObject(
                        json
                );


        Assert.assertEquals(
                Long.valueOf(60049001L),
                jsonObject.getLong("orderId")
        );


        Assert.assertEquals(
                "6004900157434",
                jsonObject.getString("orderNo")
        );


        Assert.assertEquals(
                Long.valueOf(100055L),
                jsonObject.getLong("shopId")
        );


        Assert.assertEquals(
                "bootstrap",
                jsonObject.getString("opType")
        );


        /*
         * 金额使用 BigDecimal，
         * 序列化以后必须仍然能够正确读取。
         */
        Assert.assertEquals(
                0,
                new BigDecimal("496227.61")
                        .compareTo(
                                jsonObject.getBigDecimal(
                                        "goodsMoney"
                                )
                        )
        );
    }


    /**
     * 测试 16：
     *
     * DirtyData
     *      ↓
     * JSON String
     *
     * 要保证异常原因和原始数据都不会丢。
     */
    @Test
    public void shouldSerializeDirtyDataToJson() {

        // ============================================================
        // 1. 构造一条脏数据
        // ============================================================

        DirtyData dirtyData =
                new DirtyData();

        dirtyData.setTableName(
                "orders"
        );

        dirtyData.setOpType(
                "bootstrap"
        );

        dirtyData.setErrorType(
                "IllegalArgumentException"
        );

        dirtyData.setErrorMessage(
                "orderId非法，orderId=0"
        );

        dirtyData.setRawData(
                "{\"table\":\"orders\",\"type\":\"bootstrap\","
                        + "\"data\":{\"orderId\":\"0\"}}"
        );

        dirtyData.setProcessTs(
                2000L
        );


        // ============================================================
        // 2. 调用待实现的序列化工具
        // ============================================================

        String json =
                DwdKafkaSerializer.serializeDirtyData(
                        dirtyData
                );


        // ============================================================
        // 3. JSON不能为空
        // ============================================================

        Assert.assertNotNull(
                json
        );


        // ============================================================
        // 4. 检查序列化结果
        // ============================================================

        JSONObject jsonObject =
                JSON.parseObject(
                        json
                );


        Assert.assertEquals(
                "orders",
                jsonObject.getString(
                        "tableName"
                )
        );


        Assert.assertEquals(
                "bootstrap",
                jsonObject.getString(
                        "opType"
                )
        );


        Assert.assertEquals(
                "IllegalArgumentException",
                jsonObject.getString(
                        "errorType"
                )
        );


        Assert.assertEquals(
                "orderId非法，orderId=0",
                jsonObject.getString(
                        "errorMessage"
                )
        );


        /*
         * rawData 必须完整保留。
         *
         * 后期发生脏数据时，
         * 我们还需要依靠原始消息定位问题。
         */
        Assert.assertTrue(
                jsonObject
                        .getString("rawData")
                        .contains(
                                "\"orderId\":\"0\""
                        )
        );


        Assert.assertEquals(
                Long.valueOf(2000L),
                jsonObject.getLong(
                        "processTs"
                )
        );
    }
}