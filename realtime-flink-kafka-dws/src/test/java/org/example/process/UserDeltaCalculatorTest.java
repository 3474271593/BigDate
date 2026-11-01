// 文件路径：src/test/java/org/example/process/UserDeltaCalculatorTest.java
package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.UserDelta;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UserDeltaCalculator 单元测试。
 *
 * 验证：
 *
 * 1. 首次未支付订单 -> USER_ORDER
 * 2. 首次已支付订单 -> USER_ORDER + USER_PAY
 * 3. 未支付 -> 已支付 -> USER_PAY
 * 4. 普通 UPDATE 不重复统计
 * 5. createTime / payTime 作为业务 Event Time
 * 6. processTs 不参与业务 Event Time
 * 7. null 金额按 ZERO 处理
 * 8. 非法业务数据不产生错误指标
 * 9. User 主题必须存在 userId
 */
public class UserDeltaCalculatorTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 首次看到未支付订单。
     *
     * 只产生 USER_ORDER。
     */
    @Test
    public void testFirstUnpaidOrderProducesOrderOnly() {

        DwdOrderInfo current = order(
                1001L,
                2001L,
                3001L,
                "88.50",
                "2026-09-21 10:00:10",
                0,
                null,
                9999999999999L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(null, current);

        Assert.assertEquals(1, result.size());

        UserDelta delta = result.get(0);

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_ORDER,
                delta.getEventType()
        );

        Assert.assertEquals(
                Long.valueOf(3001L),
                delta.getUserId()
        );

        Assert.assertEquals(
                Long.valueOf(2001L),
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

        assertMoney(
                "88.50",
                delta.getOrderAmount()
        );

        assertMoney(
                "0",
                delta.getPaidAmount()
        );
    }

    /**
     * 第一次看到订单时已经支付。
     *
     * 产生：
     *
     * USER_ORDER
     * USER_PAY
     */
    @Test
    public void testFirstPaidOrderProducesOrderAndPay() {

        DwdOrderInfo current = order(
                1002L,
                2001L,
                3002L,
                "120.80",
                "2026-09-21 10:01:10",
                1,
                "2026-09-21 10:02:20",
                9999999999999L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(null, current);

        Assert.assertEquals(2, result.size());

        UserDelta orderDelta =
                result.get(0);

        UserDelta payDelta =
                result.get(1);

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_ORDER,
                orderDelta.getEventType()
        );

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_PAY,
                payDelta.getEventType()
        );

        Assert.assertEquals(
                1L,
                orderDelta.getOrderCount()
        );

        Assert.assertEquals(
                0L,
                orderDelta.getPaidOrderCount()
        );

        Assert.assertEquals(
                0L,
                payDelta.getOrderCount()
        );

        Assert.assertEquals(
                1L,
                payDelta.getPaidOrderCount()
        );

        assertMoney(
                "120.80",
                orderDelta.getOrderAmount()
        );

        assertMoney(
                "120.80",
                payDelta.getPaidAmount()
        );
    }

    /**
     * 未支付 -> 已支付。
     *
     * 之前 USER_ORDER 已经统计，
     * 因此这里只产生 USER_PAY。
     */
    @Test
    public void testUnpaidToPaidProducesPayOnly() {

        DwdOrderInfo previous = order(
                1003L,
                2001L,
                3003L,
                "66.00",
                "2026-09-21 10:03:00",
                0,
                null,
                1000L
        );

        DwdOrderInfo current = order(
                1003L,
                2001L,
                3003L,
                "66.00",
                "2026-09-21 10:03:00",
                1,
                "2026-09-21 10:05:30",
                2000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        previous,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        UserDelta delta =
                result.get(0);

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_PAY,
                delta.getEventType()
        );

        Assert.assertEquals(
                0L,
                delta.getOrderCount()
        );

        Assert.assertEquals(
                1L,
                delta.getPaidOrderCount()
        );

        assertMoney(
                "0",
                delta.getOrderAmount()
        );

        assertMoney(
                "66.00",
                delta.getPaidAmount()
        );
    }

    /**
     * 已支付 -> 已支付普通 UPDATE。
     *
     * 不重复产生 USER_PAY。
     */
    @Test
    public void testPaidToPaidUpdateProducesNothing() {

        DwdOrderInfo previous = order(
                1004L,
                2001L,
                3004L,
                "50.00",
                "2026-09-21 10:06:00",
                1,
                "2026-09-21 10:07:00",
                1000L
        );

        DwdOrderInfo current = order(
                1004L,
                2001L,
                3004L,
                "50.00",
                "2026-09-21 10:06:00",
                1,
                "2026-09-21 10:07:00",
                2000L
        );

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(previous, current)
                        .isEmpty()
        );
    }

    /**
     * 未支付 -> 未支付普通 UPDATE。
     *
     * 不重复产生 USER_ORDER。
     */
    @Test
    public void testUnpaidToUnpaidUpdateProducesNothing() {

        DwdOrderInfo previous = order(
                1005L,
                2001L,
                3005L,
                "99.00",
                "2026-09-21 10:08:00",
                0,
                null,
                1000L
        );

        DwdOrderInfo current = order(
                1005L,
                2001L,
                3005L,
                "99.00",
                "2026-09-21 10:08:00",
                0,
                null,
                2000L
        );

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(previous, current)
                        .isEmpty()
        );
    }

    /**
     * USER_ORDER 必须使用 createTime。
     */
    @Test
    public void testOrderUsesCreateTimeAsEventTime() {

        String createTime =
                "2026-09-21 11:10:15";

        DwdOrderInfo current = order(
                1006L,
                2001L,
                3006L,
                "10.00",
                createTime,
                0,
                null,
                9999999999999L
        );

        UserDelta delta =
                UserDeltaCalculator
                        .calculate(null, current)
                        .get(0);

        Assert.assertEquals(
                timestamp(createTime),
                delta.getEventTs()
        );

        Assert.assertNotEquals(
                current.getProcessTs().longValue(),
                delta.getEventTs()
        );
    }

    /**
     * USER_PAY 必须使用 payTime。
     */
    @Test
    public void testPayUsesPayTimeAsEventTime() {

        String payTime =
                "2026-09-21 11:12:30";

        DwdOrderInfo current = order(
                1007L,
                2001L,
                3007L,
                "20.00",
                "2026-09-21 11:11:00",
                1,
                payTime,
                456L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        UserDelta payDelta =
                result.get(1);

        Assert.assertEquals(
                timestamp(payTime),
                payDelta.getEventTs()
        );

        Assert.assertNotEquals(
                current.getProcessTs().longValue(),
                payDelta.getEventTs()
        );
    }

    /**
     * null 金额按 ZERO 处理。
     */
    @Test
    public void testNullMoneyBecomesZero() {

        DwdOrderInfo current = order(
                1008L,
                2001L,
                3008L,
                null,
                "2026-09-21 12:01:00",
                1,
                "2026-09-21 12:02:00",
                1000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                2,
                result.size()
        );

        assertMoney(
                "0",
                result.get(0).getOrderAmount()
        );

        assertMoney(
                "0",
                result.get(1).getPaidAmount()
        );
    }

    /**
     * current == null。
     */
    @Test
    public void testNullCurrentProducesNothing() {

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(null, null)
                        .isEmpty()
        );
    }

    /**
     * orderId == null。
     */
    @Test
    public void testNullOrderIdProducesNothing() {

        DwdOrderInfo current = order(
                null,
                2001L,
                3009L,
                "10.00",
                "2026-09-21 12:03:00",
                0,
                null,
                1000L
        );

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * userId == null。
     *
     * 与 Shop 主题不同：
     *
     * User DWS 的统计 Key 就是 userId，
     * 因此没有 userId 的订单无法进入 User 主题。
     */
    @Test
    public void testNullUserIdProducesNothing() {

        DwdOrderInfo current = order(
                1010L,
                2001L,
                null,
                "10.00",
                "2026-09-21 12:04:00",
                0,
                null,
                1000L
        );

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * shopId == null。
     *
     * User 主指标仍然可以统计订单，
     * 只是该订单不能贡献 orderShopCount / paidShopCount。
     */
    @Test
    public void testNullShopIdStillProducesUserOrder() {

        DwdOrderInfo current = order(
                1011L,
                null,
                3011L,
                "10.00",
                "2026-09-21 12:05:00",
                0,
                null,
                1000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        Assert.assertNull(
                result.get(0).getShopId()
        );

        Assert.assertEquals(
                1L,
                result.get(0).getOrderCount()
        );
    }

    /**
     * isPay=1，但是 payTime=null。
     *
     * USER_ORDER 可以产生，
     * USER_PAY 不可以产生。
     */
    @Test
    public void testPaidWithoutPayTimeDoesNotProducePay() {

        DwdOrderInfo current = order(
                1012L,
                2001L,
                3012L,
                "55.00",
                "2026-09-21 12:06:00",
                1,
                null,
                1000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_ORDER,
                result.get(0).getEventType()
        );
    }

    /**
     * isPay=0，但是 payTime 有值。
     *
     * 支付状态必须以 isPay 为准，
     * 因此只能产生 USER_ORDER。
     */
    @Test
    public void testUnpaidWithPayTimeDoesNotProducePay() {

        DwdOrderInfo current = order(
                1013L,
                2001L,
                3013L,
                "55.00",
                "2026-09-21 12:07:00",
                0,
                "2026-09-21 12:08:00",
                1000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_ORDER,
                result.get(0).getEventType()
        );
    }

    /**
     * createTime == null。
     *
     * 无法产生 USER_ORDER。
     */
    @Test
    public void testMissingCreateTimeDoesNotProduceOrder() {

        DwdOrderInfo current = order(
                1014L,
                2001L,
                3014L,
                "55.00",
                null,
                0,
                null,
                1000L
        );

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * createTime 非法。
     */
    @Test
    public void testInvalidCreateTimeDoesNotProduceOrder() {

        DwdOrderInfo current = order(
                1015L,
                2001L,
                3015L,
                "55.00",
                "not-a-time",
                0,
                null,
                1000L
        );

        Assert.assertTrue(
                UserDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * isPay=1，但 payTime 非法。
     *
     * 合法 createTime 仍然产生 USER_ORDER，
     * 非法 payTime 不产生 USER_PAY。
     */
    @Test
    public void testInvalidPayTimeDoesNotProducePay() {

        DwdOrderInfo current = order(
                1016L,
                2001L,
                3016L,
                "55.00",
                "2026-09-21 12:09:00",
                1,
                "not-a-time",
                1000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                1,
                result.size()
        );

        Assert.assertEquals(
                UserDeltaCalculator.EVENT_USER_ORDER,
                result.get(0).getEventType()
        );
    }

    /**
     * 下单和支付跨分钟。
     *
     * 两个 UserDelta 必须保留各自独立 Event Timestamp。
     */
    @Test
    public void testOrderAndPayCanBelongToDifferentMinutes() {

        String createTime =
                "2026-09-21 13:00:59";

        String payTime =
                "2026-09-21 13:01:01";

        DwdOrderInfo current = order(
                1017L,
                2001L,
                3017L,
                "100.00",
                createTime,
                1,
                payTime,
                1000L
        );

        List<UserDelta> result =
                UserDeltaCalculator.calculate(
                        null,
                        current
                );

        Assert.assertEquals(
                2,
                result.size()
        );

        UserDelta orderDelta =
                result.get(0);

        UserDelta payDelta =
                result.get(1);

        Assert.assertEquals(
                timestamp(createTime),
                orderDelta.getEventTs()
        );

        Assert.assertEquals(
                timestamp(payTime),
                payDelta.getEventTs()
        );

        Assert.assertNotEquals(
                orderDelta.getEventTs(),
                payDelta.getEventTs()
        );
    }

    // ================================================================
    // 测试辅助方法
    // ================================================================

    private DwdOrderInfo order(
            Long orderId,
            Long shopId,
            Long userId,
            String money,
            String createTime,
            Integer isPay,
            String payTime,
            Long processTs) {

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(orderId);
        order.setShopId(shopId);
        order.setUserId(userId);

        order.setRealTotalMoney(
                money == null
                        ? null
                        : new BigDecimal(money)
        );

        order.setCreateTime(createTime);
        order.setIsPay(isPay);
        order.setPayTime(payTime);

        order.setOpType("TEST");
        order.setProcessTs(processTs);

        return order;
    }

    private void assertMoney(
            String expected,
            BigDecimal actual) {

        Assert.assertNotNull(actual);

        Assert.assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }

    private long timestamp(
            String value) {

        return LocalDateTime
                .parse(
                        value,
                        FORMATTER
                )
                .atZone(
                        ZoneId.systemDefault()
                )
                .toInstant()
                .toEpochMilli();
    }
}