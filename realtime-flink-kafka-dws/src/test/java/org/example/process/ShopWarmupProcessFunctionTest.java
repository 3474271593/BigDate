package org.example.process;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopDelta;
import org.example.bean.ShopSourceRecord;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ShopWarmupProcessFunction 测试。
 *
 * 重点验证：
 *
 * 1. Bootstrap 历史范围只恢复 State，不产生 ShopDelta；
 * 2. offset == boundary 时立即进入实时阶段；
 * 3. 历史预热出来的 previous-order-state
 *    能够被边界后的实时 CDC 正确使用；
 * 4. 历史为空的 Partition 从 offset=0 开始就是实时数据；
 * 5. 未配置 Partition 边界时必须失败。
 */
public class ShopWarmupProcessFunctionTest {

    /**
     * P0 历史边界：
     *
     * [0, 20028)
     *
     * 因此：
     *
     * offset=20027 -> 历史
     *
     * 这里只允许恢复 previous-order-state，
     * 不能产生任何 ShopDelta。
     */
    @Test
    public void shouldNotEmitAtLastHistoricalOffset()
            throws Exception {

        Map<Integer, Long> boundaries =
                new HashMap<>();

        boundaries.put(
                0,
                20028L
        );

        ShopWarmupProcessFunction function =
                new ShopWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                ShopSourceRecord,
                ShopDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                ShopSourceRecord,
                ShopDelta> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        operator,
                        record ->
                                record.getOrder()
                                        .getOrderId(),
                        Types.LONG
                );

        try {

            harness.open();

            harness.processElement(
                    record(
                            0,
                            20027L,
                            order(
                                    7L,
                                    0,
                                    null
                            )
                    ),
                    1L
            );

            Assert.assertTrue(
                    values(harness).isEmpty()
            );

        } finally {

            harness.close();
        }
    }

    /**
     * 验证历史状态能够无缝交给实时阶段。
     *
     * offset=20027：
     * 未支付订单，只预热 State。
     *
     * offset=20028：
     * 同一个订单变成已支付。
     *
     * 预期：
     *
     * 只产生 SHOP_PAY。
     *
     * 不能再次产生 SHOP_ORDER，
     * 否则就说明历史状态没有正确恢复。
     */
    @Test
    public void shouldWarmStateBeforeBoundaryAndEmitOnlyRealtimePaymentChange()
            throws Exception {

        Map<Integer, Long> boundaries =
                new HashMap<>();

        boundaries.put(
                0,
                20028L
        );

        ShopWarmupProcessFunction function =
                new ShopWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                ShopSourceRecord,
                ShopDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                ShopSourceRecord,
                ShopDelta> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        operator,
                        record ->
                                record.getOrder()
                                        .getOrderId(),
                        Types.LONG
                );

        try {

            harness.open();

            /*
             * 最后一条历史数据。
             *
             * 只预热：
             *
             * orderId=7
             * isPay=0
             */
            harness.processElement(
                    record(
                            0,
                            20027L,
                            order(
                                    7L,
                                    0,
                                    null
                            )
                    ),
                    1L
            );

            Assert.assertTrue(
                    values(harness).isEmpty()
            );

            /*
             * 第一条实时数据。
             *
             * 同一个订单：
             *
             * isPay 0 -> 1
             */
            harness.processElement(
                    record(
                            0,
                            20028L,
                            order(
                                    7L,
                                    1,
                                    "2019-09-05 19:53:20"
                            )
                    ),
                    2L
            );

            List<ShopDelta> output =
                    values(harness);

            Assert.assertEquals(
                    1,
                    output.size()
            );

            Assert.assertEquals(
                    "SHOP_PAY",
                    output.get(0).getEventType()
            );

            Assert.assertEquals(
                    Long.valueOf(7L),
                    output.get(0).getOrderId()
            );

            Assert.assertEquals(
                    Long.valueOf(100054L),
                    output.get(0).getShopId()
            );

            Assert.assertEquals(
                    Long.valueOf(100001L),
                    output.get(0).getUserId()
            );

            Assert.assertEquals(
                    1L,
                    output.get(0)
                            .getPaidOrderCountDelta()
            );

            Assert.assertEquals(
                    0,
                    new BigDecimal("88.80")
                            .compareTo(
                                    output.get(0)
                                            .getPaidAmountDelta()
                            )
            );

        } finally {

            harness.close();
        }
    }

    /**
     * P1 Bootstrap 边界为0。
     *
     * 因此：
     *
     * offset=0
     *
     * 就已经属于实时数据。
     *
     * 第一次看到未支付订单时，
     * 应产生一个 SHOP_ORDER。
     */
    @Test
    public void shouldTreatOffsetAtBoundaryAsRealtimeForEmptyPartition()
            throws Exception {

        Map<Integer, Long> boundaries =
                new HashMap<>();

        boundaries.put(
                1,
                0L
        );

        ShopWarmupProcessFunction function =
                new ShopWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                ShopSourceRecord,
                ShopDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                ShopSourceRecord,
                ShopDelta> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        operator,
                        record ->
                                record.getOrder()
                                        .getOrderId(),
                        Types.LONG
                );

        try {

            harness.open();

            harness.processElement(
                    record(
                            1,
                            0L,
                            order(
                                    8L,
                                    0,
                                    null
                            )
                    ),
                    1L
            );

            List<ShopDelta> output =
                    values(harness);

            Assert.assertEquals(
                    1,
                    output.size()
            );

            Assert.assertEquals(
                    "SHOP_ORDER",
                    output.get(0).getEventType()
            );

            Assert.assertEquals(
                    1L,
                    output.get(0)
                            .getOrderCountDelta()
            );

            Assert.assertEquals(
                    0L,
                    output.get(0)
                            .getPaidOrderCountDelta()
            );

            Assert.assertEquals(
                    0,
                    new BigDecimal("88.80")
                            .compareTo(
                                    output.get(0)
                                            .getOrderAmountDelta()
                            )
            );

        } finally {

            harness.close();
        }
    }

    /**
     * 如果收到一个没有配置 Bootstrap 边界的 Partition，
     * 不能猜测它应该从哪里开始实时计算。
     *
     * 必须直接失败。
     */
    @Test
    public void shouldFailWhenPartitionBoundaryIsMissing()
            throws Exception {

        Map<Integer, Long> boundaries =
                new HashMap<>();

        boundaries.put(
                0,
                20028L
        );

        ShopWarmupProcessFunction function =
                new ShopWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                ShopSourceRecord,
                ShopDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                ShopSourceRecord,
                ShopDelta> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        operator,
                        record ->
                                record.getOrder()
                                        .getOrderId(),
                        Types.LONG
                );

        try {

            harness.open();

            try {

                /*
                 * Partition 2 没有配置边界。
                 */
                harness.processElement(
                        record(
                                2,
                                0L,
                                order(
                                        9L,
                                        0,
                                        null
                                )
                        ),
                        1L
                );

                Assert.fail(
                        "Expected missing partition boundary to fail"
                );

            } catch (Exception e) {

                /*
                 * TestHarness 有可能直接抛出原异常，
                 * 也可能在外层包装一次。
                 *
                 * 因此检查整个 cause 链。
                 */
                Assert.assertTrue(
                        containsMessage(
                                e,
                                "Missing realtime start offset for partition 2"
                        )
                );
            }

        } finally {

            harness.close();
        }
    }

    private List<ShopDelta> values(
            KeyedOneInputStreamOperatorTestHarness<
                    Long,
                    ShopSourceRecord,
                    ShopDelta> harness) {

        return harness
                .extractOutputStreamRecords()
                .stream()
                .map(record ->
                        record.getValue()
                )
                .collect(
                        Collectors.toList()
                );
    }

    private ShopSourceRecord record(
            int partition,
            long offset,
            DwdOrderInfo order) {

        return new ShopSourceRecord(
                order,
                partition,
                offset
        );
    }

    private DwdOrderInfo order(
            Long id,
            int isPay,
            String payTime) {

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(
                id
        );

        order.setShopId(
                100054L
        );

        order.setUserId(
                100001L
        );

        order.setCreateTime(
                "2019-09-05 19:50:00"
        );

        order.setIsPay(
                isPay
        );

        order.setPayTime(
                payTime
        );

        order.setRealTotalMoney(
                new BigDecimal(
                        "88.80"
                )
        );

        return order;
    }

    private boolean containsMessage(
            Throwable throwable,
            String expected) {

        Throwable current =
                throwable;

        while (current != null) {

            if (current.getMessage() != null
                    && current.getMessage()
                    .contains(expected)) {

                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }
}