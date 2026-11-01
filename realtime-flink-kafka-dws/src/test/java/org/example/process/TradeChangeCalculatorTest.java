package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class TradeChangeCalculatorTest {

    @Test
    public void shouldCountNewUnpaidOrderOnce() {
        DwdOrderInfo current = createOrder(1001L, 101L, 0, "100.00");

        TradeSummary delta = TradeChangeCalculator.calculate(null, current);

        Assert.assertEquals(1L, delta.getOrderCount());
        Assert.assertEquals(0L, delta.getPaidOrderCount());
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(delta.getPaidAmount()));
    }

    @Test
    public void shouldOnlyAddPaymentMetricsWhenOrderBecomesPaid() {
        DwdOrderInfo previous = createOrder(1001L, 101L, 0, "100.00");
        DwdOrderInfo current = createOrder(1001L, 101L, 1, "100.00");

        TradeSummary delta = TradeChangeCalculator.calculate(previous, current);

        Assert.assertEquals(0L, delta.getOrderCount());
        Assert.assertEquals(1L, delta.getPaidOrderCount());
        Assert.assertEquals(0, new BigDecimal("100.00").compareTo(delta.getPaidAmount()));
    }

    @Test
    public void shouldNotCountRepeatedPaidUpdateAgain() {
        DwdOrderInfo previous = createOrder(1001L, 101L, 1, "100.00");
        DwdOrderInfo current = createOrder(1001L, 101L, 1, "100.00");

        TradeSummary delta = TradeChangeCalculator.calculate(previous, current);

        Assert.assertEquals(0L, delta.getOrderCount());
        Assert.assertEquals(0L, delta.getPaidOrderCount());
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(delta.getPaidAmount()));
    }

    private DwdOrderInfo createOrder(Long orderId, Long userId, Integer isPay, String realTotalMoney) {
        DwdOrderInfo order = new DwdOrderInfo();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setIsPay(isPay);
        order.setRealTotalMoney(new BigDecimal(realTotalMoney));
        return order;
    }
}