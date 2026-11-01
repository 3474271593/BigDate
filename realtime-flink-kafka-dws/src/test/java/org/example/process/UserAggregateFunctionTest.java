// 文件路径：src/test/java/org/example/process/UserAggregateFunctionTest.java
package org.example.process;

import org.example.bean.UserAccumulator;
import org.example.bean.UserDelta;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * UserAggregateFunction 单元测试。
 *
 * 重点验证：
 *
 * 1. 同一用户同一家店多次下单：
 *    orderCount 正常累加
 *    orderShopCount 只计算一次
 *
 * 2. 同一用户不同店铺下单：
 *    orderShopCount 正常增加
 *
 * 3. 支付店铺单独去重：
 *    paidShopCount 与 orderShopCount 互不干扰
 *
 * 4. 金额正确累计。
 */
public class UserAggregateFunctionTest {

    /**
     * 同一家店连续下3单。
     *
     * 预期：
     *
     * orderCount = 3
     * orderShopCount = 1
     * orderAmount = 600
     */
    @Test
    public void testSameShopMultipleOrders() {

        UserAggregateFunction function =
                new UserAggregateFunction();

        UserAccumulator accumulator =
                function.createAccumulator();

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100057L,
                                "100.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100057L,
                                "200.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100057L,
                                "300.00"
                        ),
                        accumulator
                );

        Assert.assertEquals(
                Long.valueOf(11L),
                accumulator.getUserId()
        );

        Assert.assertEquals(
                3L,
                accumulator.getOrderCount()
        );

        Assert.assertEquals(
                1L,
                accumulator.getOrderShopCount()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("600.00")
                        .compareTo(
                                accumulator.getOrderAmount()
                        )
        );
    }

    /**
     * 同一用户在两个不同店铺下单。
     *
     * 预期：
     *
     * orderCount = 3
     * orderShopCount = 2
     */
    @Test
    public void testDifferentOrderShops() {

        UserAggregateFunction function =
                new UserAggregateFunction();

        UserAccumulator accumulator =
                function.createAccumulator();

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100057L,
                                "100.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100057L,
                                "200.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100061L,
                                "300.00"
                        ),
                        accumulator
                );

        Assert.assertEquals(
                3L,
                accumulator.getOrderCount()
        );

        Assert.assertEquals(
                2L,
                accumulator.getOrderShopCount()
        );

        Assert.assertTrue(
                accumulator.getOrderShopIds()
                        .contains(100057L)
        );

        Assert.assertTrue(
                accumulator.getOrderShopIds()
                        .contains(100061L)
        );
    }

    /**
     * 支付店铺必须独立于下单店铺统计。
     *
     * 用户：
     *
     * 在100057下单
     * 在100061下单
     *
     * 但只有100057产生支付。
     *
     * 预期：
     *
     * orderShopCount = 2
     * paidShopCount = 1
     */
    @Test
    public void testPaidShopIndependentDeduplication() {

        UserAggregateFunction function =
                new UserAggregateFunction();

        UserAccumulator accumulator =
                function.createAccumulator();

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100057L,
                                "100.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createOrderDelta(
                                11L,
                                100061L,
                                "200.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createPayDelta(
                                11L,
                                100057L,
                                "100.00"
                        ),
                        accumulator
                );

        Assert.assertEquals(
                2L,
                accumulator.getOrderCount()
        );

        Assert.assertEquals(
                2L,
                accumulator.getOrderShopCount()
        );

        Assert.assertEquals(
                1L,
                accumulator.getPaidOrderCount()
        );

        Assert.assertEquals(
                1L,
                accumulator.getPaidShopCount()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(
                                accumulator.getOrderAmount()
                        )
        );

        Assert.assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                accumulator.getPaidAmount()
                        )
        );
    }

    /**
     * 同一家店产生多次支付贡献。
     *
     * 订单数可以增加，
     * 但支付店铺数只能算1。
     */
    @Test
    public void testSameShopMultiplePayments() {

        UserAggregateFunction function =
                new UserAggregateFunction();

        UserAccumulator accumulator =
                function.createAccumulator();

        accumulator =
                function.add(
                        createPayDelta(
                                11L,
                                100057L,
                                "100.00"
                        ),
                        accumulator
                );

        accumulator =
                function.add(
                        createPayDelta(
                                11L,
                                100057L,
                                "200.00"
                        ),
                        accumulator
                );

        Assert.assertEquals(
                2L,
                accumulator.getPaidOrderCount()
        );

        Assert.assertEquals(
                1L,
                accumulator.getPaidShopCount()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(
                                accumulator.getPaidAmount()
                        )
        );
    }

    /**
     * 构造 USER_ORDER。
     */
    private UserDelta createOrderDelta(
            Long userId,
            Long shopId,
            String amount) {

        UserDelta delta =
                new UserDelta();

        delta.setUserId(userId);
        delta.setShopId(shopId);

        delta.setEventType(
                UserDeltaCalculator.EVENT_USER_ORDER
        );

        delta.setEventTs(
                System.currentTimeMillis()
        );

        delta.setOrderCount(1L);
        delta.setOrderAmount(
                new BigDecimal(amount)
        );

        delta.setPaidOrderCount(0L);
        delta.setPaidAmount(
                BigDecimal.ZERO
        );

        return delta;
    }

    /**
     * 构造 USER_PAY。
     */
    private UserDelta createPayDelta(
            Long userId,
            Long shopId,
            String amount) {

        UserDelta delta =
                new UserDelta();

        delta.setUserId(userId);
        delta.setShopId(shopId);

        delta.setEventType(
                UserDeltaCalculator.EVENT_USER_PAY
        );

        delta.setEventTs(
                System.currentTimeMillis()
        );

        delta.setOrderCount(0L);
        delta.setOrderAmount(
                BigDecimal.ZERO
        );

        delta.setPaidOrderCount(1L);
        delta.setPaidAmount(
                new BigDecimal(amount)
        );

        return delta;
    }
}