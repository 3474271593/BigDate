// 文件路径：src/test/java/org/example/process/GoodsAggregateFunctionTest.java
package org.example.process;

import org.example.bean.GoodsDelta;
import org.example.bean.GoodsSummary;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * GoodsAggregateFunction 单元测试。
 *
 * 验证：
 * 1. 普通下单聚合
 * 2. 支付聚合
 * 3. 下单 + 支付累计
 * 4. 正向 Correction
 * 5. 负向 Correction
 * 6. 已支付商品 Correction
 * 7. 空 goodsName 不覆盖已有有效名称
 */
public class GoodsAggregateFunctionTest {

    private GoodsAggregateFunction function;

    @Before
    public void setUp() {
        function = new GoodsAggregateFunction();
    }

    /**
     * 1. 普通下单。
     *
     * 2件商品 / 23.20元
     */
    @Test
    public void testOrderDelta() {

        GoodsSummary accumulator =
                function.createAccumulator();

        GoodsDelta delta =
                createDelta(
                        100125L,
                        "测试商品",
                        1L,
                        2L,
                        "23.20",
                        0L,
                        0L,
                        "0"
                );

        GoodsSummary result =
                function.add(
                        delta,
                        accumulator
                );

        Assert.assertEquals(
                Long.valueOf(100125L),
                result.getGoodsId()
        );

        Assert.assertEquals(
                "测试商品",
                result.getGoodsName()
        );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                2L,
                result.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "23.20",
                result.getOrderAmount()
        );

        Assert.assertEquals(
                0L,
                result.getPaidOrderCount()
        );

        Assert.assertEquals(
                0L,
                result.getPaidGoodsNum()
        );

        assertDecimalEquals(
                "0",
                result.getPaidAmount()
        );
    }

    /**
     * 2. 支付 Delta。
     */
    @Test
    public void testPayDelta() {

        GoodsSummary accumulator =
                function.createAccumulator();

        GoodsDelta delta =
                createDelta(
                        100125L,
                        "测试商品",
                        0L,
                        0L,
                        "0",
                        1L,
                        2L,
                        "23.20"
                );

        GoodsSummary result =
                function.add(
                        delta,
                        accumulator
                );

        Assert.assertEquals(
                0L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                0L,
                result.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "0",
                result.getOrderAmount()
        );

        Assert.assertEquals(
                1L,
                result.getPaidOrderCount()
        );

        Assert.assertEquals(
                2L,
                result.getPaidGoodsNum()
        );

        assertDecimalEquals(
                "23.20",
                result.getPaidAmount()
        );
    }

    /**
     * 3. 同一个Accumulator先下单、后支付。
     */
    @Test
    public void testOrderAndPayAccumulation() {

        GoodsSummary accumulator =
                function.createAccumulator();

        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        1L,
                        2L,
                        "23.20",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        0L,
                        0L,
                        "0",
                        1L,
                        2L,
                        "23.20"
                ),
                accumulator
        );

        GoodsSummary result =
                function.getResult(
                        accumulator
                );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                2L,
                result.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "23.20",
                result.getOrderAmount()
        );

        Assert.assertEquals(
                1L,
                result.getPaidOrderCount()
        );

        Assert.assertEquals(
                2L,
                result.getPaidGoodsNum()
        );

        assertDecimalEquals(
                "23.20",
                result.getPaidAmount()
        );
    }

    /**
     * 4. 正向 Correction。
     *
     * 初始：
     * 1件 / 11.60
     *
     * Correction：
     * +2件 / +23.20
     *
     * 最终：
     * 3件 / 34.80
     */
    @Test
    public void testPositiveCorrection() {

        GoodsSummary accumulator =
                function.createAccumulator();

        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        1L,
                        1L,
                        "11.60",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        0L,
                        2L,
                        "23.20",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        GoodsSummary result =
                function.getResult(
                        accumulator
                );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                3L,
                result.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "34.80",
                result.getOrderAmount()
        );
    }

    /**
     * 5. 负向 Correction。
     *
     * 初始：
     * 3件 / 34.80
     *
     * Correction：
     * -2件 / -23.20
     *
     * 最终：
     * 1件 / 11.60
     */
    @Test
    public void testNegativeCorrection() {

        GoodsSummary accumulator =
                function.createAccumulator();

        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        1L,
                        3L,
                        "34.80",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        0L,
                        -2L,
                        "-23.20",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        GoodsSummary result =
                function.getResult(
                        accumulator
                );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                1L,
                result.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "11.60",
                result.getOrderAmount()
        );
    }

    /**
     * 6. 已支付商品发生Correction。
     *
     * 初始：
     * order = 1件 / 11.60
     * paid  = 1件 / 11.60
     *
     * Correction：
     * order +3件 / +34.80
     * paid  +3件 / +34.80
     *
     * 最终：
     * order = 4件 / 46.40
     * paid  = 4件 / 46.40
     */
    @Test
    public void testPaidCorrection() {

        GoodsSummary accumulator =
                function.createAccumulator();

        /*
         * GOODS_ORDER
         */
        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        1L,
                        1L,
                        "11.60",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        /*
         * GOODS_PAY
         */
        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        0L,
                        0L,
                        "0",
                        1L,
                        1L,
                        "11.60"
                ),
                accumulator
        );

        /*
         * GOODS_UPDATE_CORRECTION
         */
        function.add(
                createDelta(
                        100125L,
                        "测试商品",
                        0L,
                        3L,
                        "34.80",
                        0L,
                        3L,
                        "34.80"
                ),
                accumulator
        );

        GoodsSummary result =
                function.getResult(
                        accumulator
                );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                4L,
                result.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "46.40",
                result.getOrderAmount()
        );

        Assert.assertEquals(
                1L,
                result.getPaidOrderCount()
        );

        Assert.assertEquals(
                4L,
                result.getPaidGoodsNum()
        );

        assertDecimalEquals(
                "46.40",
                result.getPaidAmount()
        );
    }

    /**
     * 7. 空goodsName不能覆盖已有有效名称。
     */
    @Test
    public void testEmptyGoodsNameDoesNotOverwrite() {

        GoodsSummary accumulator =
                function.createAccumulator();

        function.add(
                createDelta(
                        100125L,
                        "三只松鼠 无核白葡萄干",
                        1L,
                        1L,
                        "11.60",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        function.add(
                createDelta(
                        100125L,
                        "",
                        0L,
                        1L,
                        "11.60",
                        0L,
                        0L,
                        "0"
                ),
                accumulator
        );

        GoodsSummary result =
                function.getResult(
                        accumulator
                );

        Assert.assertEquals(
                "三只松鼠 无核白葡萄干",
                result.getGoodsName()
        );
    }

    /**
     * 创建测试GoodsDelta。
     */
    private GoodsDelta createDelta(
            Long goodsId,
            String goodsName,
            long orderCountDelta,
            long orderGoodsNumDelta,
            String orderAmountDelta,
            long paidOrderCountDelta,
            long paidGoodsNumDelta,
            String paidAmountDelta) {

        GoodsDelta delta =
                new GoodsDelta();

        delta.setGoodsId(
                goodsId
        );

        delta.setGoodsName(
                goodsName
        );

        delta.setOrderCountDelta(
                orderCountDelta
        );

        delta.setOrderGoodsNumDelta(
                orderGoodsNumDelta
        );

        delta.setOrderAmountDelta(
                new BigDecimal(
                        orderAmountDelta
                )
        );

        delta.setPaidOrderCountDelta(
                paidOrderCountDelta
        );

        delta.setPaidGoodsNumDelta(
                paidGoodsNumDelta
        );

        delta.setPaidAmountDelta(
                new BigDecimal(
                        paidAmountDelta
                )
        );

        return delta;
    }

    /**
     * BigDecimal数值比较。
     *
     * 使用compareTo避免：
     *
     * 23.20
     * 23.2
     *
     * 因scale不同导致equals失败。
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