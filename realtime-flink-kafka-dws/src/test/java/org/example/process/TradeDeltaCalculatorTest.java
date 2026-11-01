// 文件路径：src/test/java/org/example/process/TradeDeltaCalculatorTest.java
package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeDelta;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * TradeDeltaCalculator单元测试。
 */
public class TradeDeltaCalculatorTest {

    /**
     * 新增未支付订单：
     * 只应该产生ORDER_CREATE。
     */
    @Test
    public void shouldCreateOrderCreateEventForNewUnpaidOrder() {

        DwdOrderInfo current =
                createOrder(
                        1001L,
                        0,
                        "2019-06-22 17:28:15",
                        null,
                        "100.00"
                );

        List<TradeDelta> result =
                TradeDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        TradeDelta delta =
                result.get(0);

        Assert.assertEquals(
                "ORDER_CREATE",
                delta.getEventType()
        );

        Assert.assertEquals(
                1L,
                delta.getOrderCountDelta()
        );

        Assert.assertEquals(
                0L,
                delta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                "2019-06-22 17:28:15",
                delta.getEventTime()
        );
    }

    /**
     * 历史bootstrap第一次看到的订单已经支付：
     *
     * 应产生：
     * ORDER_CREATE
     * ORDER_PAY
     */
    @Test
    public void shouldCreateOrderAndPayEventsForFirstPaidOrder() {

        DwdOrderInfo current =
                createOrder(
                        1001L,
                        1,
                        "2019-06-22 17:28:15",
                        "2019-06-22 17:30:20",
                        "100.50"
                );

        List<TradeDelta> result =
                TradeDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                2,
                result.size()
        );

        TradeDelta createDelta =
                result.get(0);

        TradeDelta payDelta =
                result.get(1);

        Assert.assertEquals(
                "ORDER_CREATE",
                createDelta.getEventType()
        );

        Assert.assertEquals(
                "ORDER_PAY",
                payDelta.getEventType()
        );

        Assert.assertEquals(
                1L,
                createDelta.getOrderCountDelta()
        );

        Assert.assertEquals(
                1L,
                payDelta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("100.50").compareTo(
                        payDelta.getPaidAmountDelta()
                )
        );

        Assert.assertEquals(
                "2019-06-22 17:30:20",
                payDelta.getEventTime()
        );
    }

    /**
     * 未支付 -> 已支付：
     * 只产生ORDER_PAY。
     */
    @Test
    public void shouldCreatePayEventWhenOrderBecomesPaid() {

        DwdOrderInfo previous =
                createOrder(
                        1001L,
                        0,
                        "2019-06-22 17:28:15",
                        null,
                        "100.50"
                );

        DwdOrderInfo current =
                createOrder(
                        1001L,
                        1,
                        "2019-06-22 17:28:15",
                        "2019-06-22 17:35:30",
                        "100.50"
                );

        List<TradeDelta> result =
                TradeDeltaCalculator.calculate(
                        previous,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        TradeDelta delta =
                result.get(0);

        Assert.assertEquals(
                "ORDER_PAY",
                delta.getEventType()
        );

        Assert.assertEquals(
                0L,
                delta.getOrderCountDelta()
        );

        Assert.assertEquals(
                1L,
                delta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("100.50").compareTo(
                        delta.getPaidAmountDelta()
                )
        );
    }

    /**
     * 已支付订单普通UPDATE：
     * 不应该重复产生交易事件。
     */
    @Test
    public void shouldNotCreateEventForRepeatedPaidUpdate() {

        DwdOrderInfo previous =
                createOrder(
                        1001L,
                        1,
                        "2019-06-22 17:28:15",
                        "2019-06-22 17:30:20",
                        "100.50"
                );

        DwdOrderInfo current =
                createOrder(
                        1001L,
                        1,
                        "2019-06-22 17:28:15",
                        "2019-06-22 17:30:20",
                        "100.50"
                );

        List<TradeDelta> result =
                TradeDeltaCalculator.calculate(
                        previous,
                        current
                );

        Assert.assertTrue(
                result.isEmpty()
        );
    }

    /**
     * 创建测试订单。
     */
    private DwdOrderInfo createOrder(
            Long orderId,
            Integer isPay,
            String createTime,
            String payTime,
            String amount
    ) {

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(orderId);
        order.setShopId(100055L);
        order.setUserId(86L);
        order.setIsPay(isPay);
        order.setCreateTime(createTime);
        order.setPayTime(payTime);

        order.setRealTotalMoney(
                new BigDecimal(amount)
        );

        return order;
    }
}