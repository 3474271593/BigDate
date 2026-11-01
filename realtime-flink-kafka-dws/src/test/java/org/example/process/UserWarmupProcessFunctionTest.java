// 文件路径：src/test/java/org/example/process/UserWarmupProcessFunctionTest.java
package org.example.process;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.example.bean.DwdOrderInfo;
import org.example.bean.UserDelta;
import org.example.bean.UserSourceRecord;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserWarmupProcessFunction 测试。
 *
 * 重点验证：
 *
 * 1. Bootstrap 历史范围只恢复 State，不产生 UserDelta；
 * 2. offset == boundary 时立即进入实时阶段；
 * 3. 历史预热得到的 previous-order-state
 *    能被边界后的实时 CDC 正确使用；
 * 4. 历史为空的 Partition 从 offset=0 开始就是实时数据；
 * 5. 未配置 Partition 边界时必须失败。
 */
public class UserWarmupProcessFunctionTest {

    /**
     * P0 User 历史边界：
     *
     * [0, 20030)
     *
     * 所以：
     *
     * offset=20029 是最后一条历史数据。
     *
     * 只能恢复 State，不能产生 UserDelta。
     */
    @Test
    public void shouldNotEmitAtLastHistoricalOffset()
            throws Exception {

        Map<Integer, Long> boundaries =
                new HashMap<>();

        boundaries.put(
                0,
                20030L
        );

        UserWarmupProcessFunction function =
                new UserWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                UserSourceRecord,
                UserDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                UserSourceRecord,
                UserDelta> harness =
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
                            20029L,
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
     * 验证历史状态可以无缝交给实时阶段。
     *
     * offset=20029：
     * 未支付订单，只恢复 State。
     *
     * offset=20030：
     * 同一个订单变成已支付。
     *
     * 预期：
     *
     * 只产生 USER_PAY，
     * 不能重新产生 USER_ORDER。
     */
    @Test
    public void shouldWarmStateBeforeBoundaryAndEmitOnlyRealtimePaymentChange()
            throws Exception {

        Map<Integer, Long> boundaries =
                new HashMap<>();

        boundaries.put(
                0,
                20030L
        );

        UserWarmupProcessFunction function =
                new UserWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                UserSourceRecord,
                UserDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                UserSourceRecord,
                UserDelta> harness =
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
             * orderId=7
             * isPay=0
             *
             * 只用于预热状态。
             */
            harness.processElement(
                    record(
                            0,
                            20029L,
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
             * 同一订单：
             *
             * isPay 0 -> 1
             */
            harness.processElement(
                    record(
                            0,
                            20030L,
                            order(
                                    7L,
                                    1,
                                    "2019-09-05 19:53:20"
                            )
                    ),
                    2L
            );

            List<UserDelta> output =
                    values(harness);

            Assert.assertEquals(
                    1,
                    output.size()
            );

            UserDelta delta =
                    output.get(0);

            Assert.assertEquals(
                    "USER_PAY",
                    delta.getEventType()
            );

            Assert.assertEquals(
                    Long.valueOf(100001L),
                    delta.getUserId()
            );

            Assert.assertEquals(
                    Long.valueOf(100054L),
                    delta.getShopId()
            );

            Assert.assertEquals(
                    0L,
                    delta.getOrderCount()
            );

            Assert.assertEquals(
                    1L,
                    delta.getPaidOrderCount()
            );

            Assert.assertEquals(
                    0,
                    BigDecimal.ZERO.compareTo(
                            delta.getOrderAmount()
                    )
            );

            Assert.assertEquals(
                    0,
                    new BigDecimal("88.80")
                            .compareTo(
                                    delta.getPaidAmount()
                            )
            );

        } finally {

            harness.close();
        }
    }

    /**
     * P1 Bootstrap 边界为 0。
     *
     * 因此 offset=0
     * 就已经属于实时数据。
     *
     * 第一次看到未支付新订单时，
     * 应产生 USER_ORDER。
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

        UserWarmupProcessFunction function =
                new UserWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                UserSourceRecord,
                UserDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                UserSourceRecord,
                UserDelta> harness =
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

            List<UserDelta> output =
                    values(harness);

            Assert.assertEquals(
                    1,
                    output.size()
            );

            UserDelta delta =
                    output.get(0);

            Assert.assertEquals(
                    "USER_ORDER",
                    delta.getEventType()
            );

            Assert.assertEquals(
                    Long.valueOf(100001L),
                    delta.getUserId()
            );

            Assert.assertEquals(
                    Long.valueOf(100054L),
                    delta.getShopId()
            );

            Assert.assertEquals(
                    1L,
                    delta.getOrderCount()
            );

            Assert.assertEquals(
                    0L,
                    delta.getPaidOrderCount()
            );

            Assert.assertEquals(
                    0,
                    new BigDecimal("88.80")
                            .compareTo(
                                    delta.getOrderAmount()
                            )
            );

            Assert.assertEquals(
                    0,
                    BigDecimal.ZERO.compareTo(
                            delta.getPaidAmount()
                    )
            );

        } finally {

            harness.close();
        }
    }

    /**
     * 如果收到没有配置 Bootstrap 边界的 Partition，
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
                20030L
        );

        UserWarmupProcessFunction function =
                new UserWarmupProcessFunction(
                        boundaries
                );

        KeyedProcessOperator<
                Long,
                UserSourceRecord,
                UserDelta> operator =
                new KeyedProcessOperator<>(
                        function
                );

        KeyedOneInputStreamOperatorTestHarness<
                Long,
                UserSourceRecord,
                UserDelta> harness =
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
                 * TestHarness 可能直接抛原始异常，
                 * 也可能包装异常，因此检查整个 cause 链。
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

    /**
     * 提取 TestHarness 中真正输出的 UserDelta。
     */
    private List<UserDelta> values(
            KeyedOneInputStreamOperatorTestHarness<
                    Long,
                    UserSourceRecord,
                    UserDelta> harness) {

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

    /**
     * 构造带 Kafka partition / offset 的输入记录。
     */
    private UserSourceRecord record(
            int partition,
            long offset,
            DwdOrderInfo order) {

        return new UserSourceRecord(
                order,
                partition,
                offset
        );
    }

    /**
     * 构造测试订单。
     */
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

    /**
     * 检查异常及其 cause 链。
     */
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