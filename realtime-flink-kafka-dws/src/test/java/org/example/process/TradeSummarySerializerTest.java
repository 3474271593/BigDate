package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.bean.TradeSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * TradeSummary JSON序列化测试。
 */
public class TradeSummarySerializerTest {

    @Test
    public void testSerializeTradeSummary() {

        // 1. 构造DWS交易窗口结果
        TradeSummary summary = new TradeSummary();

        summary.setWindowStart(
                "2019-09-05 18:20:00"
        );

        summary.setWindowEnd(
                "2019-09-05 18:21:00"
        );

        summary.setOrderCount(
                10L
        );

        summary.setPaidOrderCount(
                8L
        );

        summary.setPaidAmount(
                new BigDecimal("12345.67")
        );

        // 2. 序列化
        String json =
                TradeSummarySerializer.serialize(summary);

        // 3. JSON不能为空
        Assert.assertNotNull(json);

        // 4. 重新解析JSON
        JSONObject object =
                JSON.parseObject(json);

        // 5. 验证字段
        Assert.assertEquals(
                "2019-09-05 18:20:00",
                object.getString("windowStart")
        );

        Assert.assertEquals(
                "2019-09-05 18:21:00",
                object.getString("windowEnd")
        );

        Assert.assertEquals(
                Long.valueOf(10L),
                object.getLong("orderCount")
        );

        Assert.assertEquals(
                Long.valueOf(8L),
                object.getLong("paidOrderCount")
        );

        Assert.assertEquals(
                0,
                new BigDecimal("12345.67")
                        .compareTo(
                                object.getBigDecimal("paidAmount")
                        )
        );
    }

    @Test
    public void testSerializeNull() {

        Assert.assertNull(
                TradeSummarySerializer.serialize(null)
        );
    }
}