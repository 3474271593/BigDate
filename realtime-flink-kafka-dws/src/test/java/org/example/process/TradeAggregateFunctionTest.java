// 文件路径：src/test/java/org/example/process/TradeAggregateFunctionTest.java
package org.example.process;

import org.example.bean.TradeDelta;
import org.example.bean.TradeSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * TradeAggregateFunction单元测试。
 *
 * 验证TradeDelta能够正确聚合成TradeSummary。
 */
public class TradeAggregateFunctionTest {

    @Test
    public void shouldAggregateTradeDeltaCorrectly() {

        TradeAggregateFunction function =
                new TradeAggregateFunction();

        TradeSummary accumulator =
                function.createAccumulator();

        /*
         * 第一条：
         *
         * 用户创建订单。
         */
        TradeDelta orderCreate1 =
                createDelta(
                        1L,
                        0L,
                        "0"
                );

        accumulator =
                function.add(
                        orderCreate1,
                        accumulator
                );

        /*
         * 第二条：
         *
         * 另外一个用户创建订单。
         */
        TradeDelta orderCreate2 =
                createDelta(
                        1L,
                        0L,
                        "0"
                );

        accumulator =
                function.add(
                        orderCreate2,
                        accumulator
                );

        /*
         * 第三条：
         *
         * 第一张订单完成支付。
         */
        TradeDelta orderPay1 =
                createDelta(
                        0L,
                        1L,
                        "100.50"
                );

        accumulator =
                function.add(
                        orderPay1,
                        accumulator
                );

        /*
         * 第四条：
         *
         * 第二张订单完成支付。
         */
        TradeDelta orderPay2 =
                createDelta(
                        0L,
                        1L,
                        "200.30"
                );

        accumulator =
                function.add(
                        orderPay2,
                        accumulator
                );

        TradeSummary result =
                function.getResult(
                        accumulator
                );

        /*
         * 两张订单。
         */
        Assert.assertEquals(
                2L,
                result.getOrderCount()
        );

        /*
         * 两张订单完成支付。
         */
        Assert.assertEquals(
                2L,
                result.getPaidOrderCount()
        );

        /*
         * 100.50 + 200.30 = 300.80
         */
        Assert.assertEquals(
                0,
                new BigDecimal("300.80").compareTo(
                        result.getPaidAmount()
                )
        );
    }

    /**
     * 创建测试TradeDelta。
     */
    private TradeDelta createDelta(
            long orderCountDelta,
            long paidOrderCountDelta,
            String paidAmountDelta
    ) {

        TradeDelta delta =
                new TradeDelta();

        delta.setOrderCountDelta(
                orderCountDelta
        );

        delta.setPaidOrderCountDelta(
                paidOrderCountDelta
        );

        delta.setPaidAmountDelta(
                new BigDecimal(paidAmountDelta)
        );

        return delta;
    }
}