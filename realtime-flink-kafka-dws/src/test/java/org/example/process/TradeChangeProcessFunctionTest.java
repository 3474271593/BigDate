// 文件路径：src/test/java/org/example/process/TradeChangeProcessFunctionTest.java
package org.example.process;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeDelta;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TradeChangeProcessFunction状态测试。
 *
 * 重点验证：
 *
 * 1. ValueState能够保存订单上一版本；
 * 2. 第一次出现订单产生ORDER_CREATE；
 * 3. 未支付 -> 已支付产生ORDER_PAY；
 * 4. 重复已支付UPDATE不会重复产生支付事件。
 */
public class TradeChangeProcessFunctionTest {

    /**
     * 测试：
     *
     * 第一次收到未支付订单
     *      ↓
     * ORDER_CREATE
     *
     * 同一个订单变成已支付
     *      ↓
     * ORDER_PAY
     */
    @Test
    public void shouldCreateOrderThenPayEvent() throws Exception {

        KeyedOneInputStreamOperatorTestHarness<Long, DwdOrderInfo, TradeDelta> harness =
                createHarness();

        try {

            harness.open();

            // --------------------------------------------------------
            // 第一次：未支付订单
            // --------------------------------------------------------

            DwdOrderInfo unpaid =
                    createOrder(
                            1001L,
                            0,
                            "2019-06-22 17:28:15",
                            null,
                            "100.50"
                    );

            harness.processElement(
                    unpaid,
                    1L
            );

            List<TradeDelta> firstOutput =
                    getOutputValues(harness);

            Assert.assertEquals(
                    1,
                    firstOutput.size()
            );

            Assert.assertEquals(
                    "ORDER_CREATE",
                    firstOutput.get(0).getEventType()
            );

            Assert.assertEquals(
                    1L,
                    firstOutput.get(0).getOrderCountDelta()
            );

            // 清理输出队列，只保留ValueState。
            harness.getOutput().clear();

            // --------------------------------------------------------
            // 第二次：同一订单完成支付
            // --------------------------------------------------------

            DwdOrderInfo paid =
                    createOrder(
                            1001L,
                            1,
                            "2019-06-22 17:28:15",
                            "2019-06-22 17:35:30",
                            "100.50"
                    );

            harness.processElement(
                    paid,
                    2L
            );

            List<TradeDelta> secondOutput =
                    getOutputValues(harness);

            Assert.assertEquals(
                    1,
                    secondOutput.size()
            );

            TradeDelta payDelta =
                    secondOutput.get(0);

            Assert.assertEquals(
                    "ORDER_PAY",
                    payDelta.getEventType()
            );

            Assert.assertEquals(
                    0L,
                    payDelta.getOrderCountDelta()
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

        } finally {

            harness.close();
        }
    }

    /**
     * 测试：
     *
     * 已支付订单再次UPDATE，
     * 不应该重复产生ORDER_PAY。
     */
    @Test
    public void shouldNotDuplicatePaidEvent() throws Exception {

        KeyedOneInputStreamOperatorTestHarness<Long, DwdOrderInfo, TradeDelta> harness =
                createHarness();

        try {

            harness.open();

            DwdOrderInfo first =
                    createOrder(
                            2001L,
                            1,
                            "2019-06-22 17:28:15",
                            "2019-06-22 17:30:20",
                            "200.00"
                    );

            /*
             * 第一次看到已支付历史订单：
             *
             * ORDER_CREATE
             * ORDER_PAY
             */
            harness.processElement(
                    first,
                    1L
            );

            List<TradeDelta> firstOutput =
                    getOutputValues(harness);

            Assert.assertEquals(
                    2,
                    firstOutput.size()
            );

            harness.getOutput().clear();

            /*
             * 第二次仍然是已支付状态。
             *
             * 可以理解成修改地址等普通CDC UPDATE。
             */
            DwdOrderInfo second =
                    createOrder(
                            2001L,
                            1,
                            "2019-06-22 17:28:15",
                            "2019-06-22 17:30:20",
                            "200.00"
                    );

            harness.processElement(
                    second,
                    2L
            );

            List<TradeDelta> secondOutput =
                    getOutputValues(harness);

            /*
             * 不应该再产生任何业务事件。
             */
            Assert.assertTrue(
                    secondOutput.isEmpty()
            );

        } finally {

            harness.close();
        }
    }

    /**
     * 创建Keyed Process测试环境。
     */
    private KeyedOneInputStreamOperatorTestHarness<Long, DwdOrderInfo, TradeDelta> createHarness()
            throws Exception {

        TradeChangeProcessFunction function =
                new TradeChangeProcessFunction();

        KeyedProcessOperator<Long, DwdOrderInfo, TradeDelta> operator =
                new KeyedProcessOperator<>(function);

        return new KeyedOneInputStreamOperatorTestHarness<>(
                operator,
                DwdOrderInfo::getOrderId,
                Types.LONG
        );
    }

    /**
     * 从Harness输出中提取TradeDelta。
     */
    private List<TradeDelta> getOutputValues(
            KeyedOneInputStreamOperatorTestHarness<Long, DwdOrderInfo, TradeDelta> harness
    ) {

        return harness
                .extractOutputStreamRecords()
                .stream()
                .map(record -> record.getValue())
                .collect(Collectors.toList());
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