// 文件路径：src/test/java/org/example/process/GoodsSummarySerializerTest.java
package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.bean.GoodsSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * GoodsSummarySerializer 单元测试。
 *
 * 验证：
 * 1. GoodsSummary可以正常序列化为JSON
 * 2. JSON中的核心字段和值正确
 * 3. null输入返回null
 */
public class GoodsSummarySerializerTest {

    /**
     * 验证完整GoodsSummary序列化。
     */
    @Test
    public void testSerializeGoodsSummary() {

        GoodsSummary summary =
                new GoodsSummary();

        summary.setWindowStart(
                "2019-06-22 17:28:00"
        );

        summary.setWindowEnd(
                "2019-06-22 17:29:00"
        );

        summary.setGoodsId(
                100125L
        );

        summary.setGoodsName(
                "三只松鼠 无核白葡萄干"
        );

        summary.setOrderCount(
                2L
        );

        summary.setOrderGoodsNum(
                5L
        );

        summary.setOrderAmount(
                new BigDecimal("58.00")
        );

        summary.setPaidOrderCount(
                1L
        );

        summary.setPaidGoodsNum(
                3L
        );

        summary.setPaidAmount(
                new BigDecimal("34.80")
        );

        String json =
                GoodsSummarySerializer.serialize(
                        summary
                );

        Assert.assertNotNull(
                json
        );

        Assert.assertFalse(
                json.trim().isEmpty()
        );

        JSONObject object =
                JSON.parseObject(
                        json
                );

        Assert.assertEquals(
                "2019-06-22 17:28:00",
                object.getString(
                        "windowStart"
                )
        );

        Assert.assertEquals(
                "2019-06-22 17:29:00",
                object.getString(
                        "windowEnd"
                )
        );

        Assert.assertEquals(
                Long.valueOf(100125L),
                object.getLong(
                        "goodsId"
                )
        );

        Assert.assertEquals(
                "三只松鼠 无核白葡萄干",
                object.getString(
                        "goodsName"
                )
        );

        Assert.assertEquals(
                Long.valueOf(2L),
                object.getLong(
                        "orderCount"
                )
        );

        Assert.assertEquals(
                Long.valueOf(5L),
                object.getLong(
                        "orderGoodsNum"
                )
        );

        assertDecimalEquals(
                "58.00",
                object.getBigDecimal(
                        "orderAmount"
                )
        );

        Assert.assertEquals(
                Long.valueOf(1L),
                object.getLong(
                        "paidOrderCount"
                )
        );

        Assert.assertEquals(
                Long.valueOf(3L),
                object.getLong(
                        "paidGoodsNum"
                )
        );

        assertDecimalEquals(
                "34.80",
                object.getBigDecimal(
                        "paidAmount"
                )
        );
    }

    /**
     * null输入不应该抛异常。
     */
    @Test
    public void testSerializeNull() {

        String json =
                GoodsSummarySerializer.serialize(
                        null
                );

        Assert.assertNull(
                json
        );
    }

    /**
     * BigDecimal只比较数值，
     * 不受scale影响。
     *
     * 例如：
     *
     * 58.00
     * 58.0
     *
     * 数值上应该视为相等。
     */
    private void assertDecimalEquals(
            String expected,
            BigDecimal actual) {

        Assert.assertNotNull(
                actual
        );

        Assert.assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}