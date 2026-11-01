// 文件路径：src/test/java/org/example/process/ShopAggregateFunctionTest.java
package org.example.process;

import org.example.bean.ShopAccumulator;
import org.example.bean.ShopDelta;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * ShopAggregateFunction 单元测试。
 *
 * 重点验证：
 *
 * 1. 下单订单数
 * 2. 下单 UV
 * 3. 下单金额
 * 4. 支付订单数
 * 5. 支付 UV
 * 6. 支付金额
 * 7. null userId 不进入 UV
 */
public class ShopAggregateFunctionTest {

    /**
     * 验证：
     *
     * 4 笔订单
     *
     * userId:
     * 10
     * 10
     * 20
     * 30
     *
     * 应得到：
     *
     * orderCount = 4
     * orderUserCount = 3
     */
    @Test
    public void testOrderCountAndExactUv() {

        ShopAggregateFunction function =
                new ShopAggregateFunction();

        ShopAccumulator accumulator =
                function.createAccumulator();

        accumulator = function.add(
                orderDelta(10L, "10.00"),
                accumulator
        );

        accumulator = function.add(
                orderDelta(10L, "20.00"),
                accumulator
        );

        accumulator = function.add(
                orderDelta(20L, "30.00"),
                accumulator
        );

        accumulator = function.add(
                orderDelta(30L, "40.00"),
                accumulator
        );

        Assert.assertEquals(
                4L,
                accumulator.getOrderCount()
        );

        Assert.assertEquals(
                3,
                accumulator.getOrderUserIds().size()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                accumulator.getOrderAmount()
                        )
        );
    }

    /**
     * 验证：
     *
     * 3 笔支付
     *
     * userId:
     * 10
     * 10
     * 30
     *
     * 应得到：
     *
     * paidOrderCount = 3
     * paidUserCount = 2
     */
    @Test
    public void testPaidCountAndExactUv() {

        ShopAggregateFunction function =
                new ShopAggregateFunction();

        ShopAccumulator accumulator =
                function.createAccumulator();

        accumulator = function.add(
                payDelta(10L, "10.00"),
                accumulator
        );

        accumulator = function.add(
                payDelta(10L, "20.00"),
                accumulator
        );

        accumulator = function.add(
                payDelta(30L, "30.00"),
                accumulator
        );

        Assert.assertEquals(
                3L,
                accumulator.getPaidOrderCount()
        );

        Assert.assertEquals(
                2,
                accumulator.getPaidUserIds().size()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("60.00")
                        .compareTo(
                                accumulator.getPaidAmount()
                        )
        );
    }

    /**
     * null userId：
     *
     * 订单事实仍然统计，
     * 但不能把 null 当成一个用户。
     */
    @Test
    public void testNullUserDoesNotIncreaseUv() {

        ShopAggregateFunction function =
                new ShopAggregateFunction();

        ShopAccumulator accumulator =
                function.createAccumulator();

        accumulator = function.add(
                orderDelta(null, "15.00"),
                accumulator
        );

        Assert.assertEquals(
                1L,
                accumulator.getOrderCount()
        );

        Assert.assertEquals(
                0,
                accumulator.getOrderUserIds().size()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("15.00")
                        .compareTo(
                                accumulator.getOrderAmount()
                        )
        );
    }

    /**
     * 同一个用户既下单又支付：
     *
     * order UV 与 paid UV 独立统计。
     */
    @Test
    public void testOrderAndPaidUvAreIndependent() {

        ShopAggregateFunction function =
                new ShopAggregateFunction();

        ShopAccumulator accumulator =
                function.createAccumulator();

        accumulator = function.add(
                orderDelta(100L, "88.00"),
                accumulator
        );

        accumulator = function.add(
                payDelta(100L, "88.00"),
                accumulator
        );

        Assert.assertEquals(
                1L,
                accumulator.getOrderCount()
        );

        Assert.assertEquals(
                1,
                accumulator.getOrderUserIds().size()
        );

        Assert.assertEquals(
                1L,
                accumulator.getPaidOrderCount()
        );

        Assert.assertEquals(
                1,
                accumulator.getPaidUserIds().size()
        );
    }

    /**
     * 验证 merge 后 UV 仍然精确去重。
     *
     * left:
     * 10, 20
     *
     * right:
     * 20, 30
     *
     * merge:
     * 10,20,30
     *
     * UV = 3
     */
    @Test
    public void testMergeExactUv() {

        ShopAggregateFunction function =
                new ShopAggregateFunction();

        ShopAccumulator left =
                function.createAccumulator();

        left = function.add(
                orderDelta(10L, "10.00"),
                left
        );

        left = function.add(
                orderDelta(20L, "20.00"),
                left
        );

        ShopAccumulator right =
                function.createAccumulator();

        right = function.add(
                orderDelta(20L, "30.00"),
                right
        );

        right = function.add(
                orderDelta(30L, "40.00"),
                right
        );

        ShopAccumulator merged =
                function.merge(left, right);

        Assert.assertEquals(
                4L,
                merged.getOrderCount()
        );

        Assert.assertEquals(
                3,
                merged.getOrderUserIds().size()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                merged.getOrderAmount()
                        )
        );
    }

    private ShopDelta orderDelta(
            Long userId,
            String amount) {

        ShopDelta delta =
                new ShopDelta();

        delta.setOrderId(1L);
        delta.setShopId(100001L);
        delta.setUserId(userId);

        delta.setOrderCountDelta(1L);
        delta.setOrderAmountDelta(
                new BigDecimal(amount)
        );

        delta.setPaidOrderCountDelta(0L);
        delta.setPaidAmountDelta(
                BigDecimal.ZERO
        );

        delta.setEventType(
                ShopDeltaCalculator.EVENT_SHOP_ORDER
        );

        return delta;
    }

    private ShopDelta payDelta(
            Long userId,
            String amount) {

        ShopDelta delta =
                new ShopDelta();

        delta.setOrderId(1L);
        delta.setShopId(100001L);
        delta.setUserId(userId);

        delta.setOrderCountDelta(0L);
        delta.setOrderAmountDelta(
                BigDecimal.ZERO
        );

        delta.setPaidOrderCountDelta(1L);
        delta.setPaidAmountDelta(
                new BigDecimal(amount)
        );

        delta.setEventType(
                ShopDeltaCalculator.EVENT_SHOP_PAY
        );

        return delta;
    }
}