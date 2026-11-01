package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopDelta;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ShopDeltaCalculator 单元测试。
 *
 * 验证订单 CDC 数据转换成 Shop 业务事件时：
 *
 * 1. 首次订单只产生一次 SHOP_ORDER
 * 2. 首次即支付产生 SHOP_ORDER + SHOP_PAY
 * 3. 未支付 -> 已支付只产生 SHOP_PAY
 * 4. 普通 UPDATE 不重复统计
 * 5. createTime/payTime 作为业务 Event Time
 * 6. processTs 不参与业务 Event Time
 * 7. 金额使用 BigDecimal
 * 8. 异常数据不会产生错误指标
 */
public class ShopDeltaCalculatorTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 首次出现的未支付订单：
     *
     * 应只产生：
     * SHOP_ORDER
     */
    @Test
    public void testFirstUnpaidOrderProducesOrderOnly() {

        DwdOrderInfo current = order(
                1001L,
                2001L,
                3001L,
                "88.50",
                "2026-09-20 10:00:10",
                0,
                null,
                9999999999999L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        Assert.assertEquals(1, result.size());

        ShopDelta delta = result.get(0);

        Assert.assertEquals(
                ShopDeltaCalculator.EVENT_SHOP_ORDER,
                delta.getEventType()
        );

        Assert.assertEquals(1L, delta.getOrderCountDelta());
        Assert.assertEquals(0L, delta.getPaidOrderCountDelta());

        assertMoney("88.50", delta.getOrderAmountDelta());
        assertMoney("0", delta.getPaidAmountDelta());

        Assert.assertEquals(
                "2026-09-20 10:00:10",
                delta.getEventTime()
        );
    }

    /**
     * 第一次看到订单时已经支付：
     *
     * 应同时产生：
     *
     * SHOP_ORDER
     * SHOP_PAY
     */
    @Test
    public void testFirstPaidOrderProducesOrderAndPay() {

        DwdOrderInfo current = order(
                1002L,
                2001L,
                3002L,
                "120.80",
                "2026-09-20 10:01:10",
                1,
                "2026-09-20 10:02:20",
                9999999999999L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        Assert.assertEquals(2, result.size());

        ShopDelta orderDelta = result.get(0);
        ShopDelta payDelta = result.get(1);

        Assert.assertEquals(
                ShopDeltaCalculator.EVENT_SHOP_ORDER,
                orderDelta.getEventType()
        );

        Assert.assertEquals(
                ShopDeltaCalculator.EVENT_SHOP_PAY,
                payDelta.getEventType()
        );

        Assert.assertEquals(1L, orderDelta.getOrderCountDelta());
        Assert.assertEquals(0L, orderDelta.getPaidOrderCountDelta());

        Assert.assertEquals(0L, payDelta.getOrderCountDelta());
        Assert.assertEquals(1L, payDelta.getPaidOrderCountDelta());

        assertMoney("120.80", orderDelta.getOrderAmountDelta());
        assertMoney("120.80", payDelta.getPaidAmountDelta());
    }

    /**
     * 未支付 -> 已支付：
     *
     * 订单之前已经统计过 SHOP_ORDER，
     * 所以这次只能产生 SHOP_PAY。
     */
    @Test
    public void testUnpaidToPaidProducesPayOnly() {

        DwdOrderInfo previous = order(
                1003L,
                2001L,
                3003L,
                "66.00",
                "2026-09-20 10:03:00",
                0,
                null,
                1000L
        );

        DwdOrderInfo current = order(
                1003L,
                2001L,
                3003L,
                "66.00",
                "2026-09-20 10:03:00",
                1,
                "2026-09-20 10:05:30",
                2000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(previous, current);

        Assert.assertEquals(1, result.size());

        ShopDelta delta = result.get(0);

        Assert.assertEquals(
                ShopDeltaCalculator.EVENT_SHOP_PAY,
                delta.getEventType()
        );

        Assert.assertEquals(0L, delta.getOrderCountDelta());
        Assert.assertEquals(1L, delta.getPaidOrderCountDelta());

        assertMoney("0", delta.getOrderAmountDelta());
        assertMoney("66.00", delta.getPaidAmountDelta());
    }

    /**
     * 已支付 -> 已支付的普通 UPDATE：
     *
     * 不能重复统计支付。
     */
    @Test
    public void testPaidToPaidUpdateProducesNothing() {

        DwdOrderInfo previous = order(
                1004L,
                2001L,
                3004L,
                "50.00",
                "2026-09-20 10:06:00",
                1,
                "2026-09-20 10:07:00",
                1000L
        );

        DwdOrderInfo current = order(
                1004L,
                2001L,
                3004L,
                "50.00",
                "2026-09-20 10:06:00",
                1,
                "2026-09-20 10:07:00",
                2000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(previous, current);

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * 未支付 -> 未支付普通 UPDATE：
     *
     * 不应该重复产生 SHOP_ORDER。
     */
    @Test
    public void testUnpaidToUnpaidUpdateProducesNothing() {

        DwdOrderInfo previous = order(
                1005L,
                2001L,
                3005L,
                "99.00",
                "2026-09-20 10:08:00",
                0,
                null,
                1000L
        );

        DwdOrderInfo current = order(
                1005L,
                2001L,
                3005L,
                "99.00",
                "2026-09-20 10:08:00",
                0,
                null,
                2000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(previous, current);

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * SHOP_ORDER 必须使用 createTime。
     */
    @Test
    public void testOrderUsesCreateTimeAsEventTime() {

        String createTime =
                "2026-09-20 11:10:15";

        DwdOrderInfo current = order(
                1006L,
                2001L,
                3006L,
                "10.00",
                createTime,
                0,
                null,
                123L
        );

        ShopDelta delta =
                ShopDeltaCalculator
                        .calculate(null, current)
                        .get(0);

        Assert.assertEquals(
                createTime,
                delta.getEventTime()
        );

        Assert.assertEquals(
                timestamp(createTime),
                delta.getEventTs()
        );
    }

    /**
     * SHOP_PAY 必须使用 payTime。
     */
    @Test
    public void testPayUsesPayTimeAsEventTime() {

        String payTime =
                "2026-09-20 11:12:30";

        DwdOrderInfo current = order(
                1007L,
                2001L,
                3007L,
                "20.00",
                "2026-09-20 11:11:00",
                1,
                payTime,
                456L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        ShopDelta payDelta = result.get(1);

        Assert.assertEquals(
                payTime,
                payDelta.getEventTime()
        );

        Assert.assertEquals(
                timestamp(payTime),
                payDelta.getEventTs()
        );
    }

    /**
     * processTs 即使是完全不同的时间，
     * 也不能成为 Shop 业务 Event Time。
     */
    @Test
    public void testProcessTsIsNotBusinessEventTime() {

        String createTime =
                "2026-09-20 12:00:00";

        DwdOrderInfo current = order(
                1008L,
                2001L,
                3008L,
                "30.00",
                createTime,
                0,
                null,
                9999999999999L
        );

        ShopDelta delta =
                ShopDeltaCalculator
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
     * null 金额按 ZERO 处理。
     */
    @Test
    public void testNullMoneyBecomesZero() {

        DwdOrderInfo current = order(
                1009L,
                2001L,
                3009L,
                null,
                "2026-09-20 12:01:00",
                1,
                "2026-09-20 12:02:00",
                1000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        Assert.assertEquals(2, result.size());

        assertMoney(
                "0",
                result.get(0).getOrderAmountDelta()
        );

        assertMoney(
                "0",
                result.get(1).getPaidAmountDelta()
        );
    }

    /**
     * current == null。
     */
    @Test
    public void testNullCurrentProducesNothing() {

        Assert.assertTrue(
                ShopDeltaCalculator
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
                3010L,
                "10.00",
                "2026-09-20 12:03:00",
                0,
                null,
                1000L
        );

        Assert.assertTrue(
                ShopDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * shopId == null。
     *
     * 没有店铺 ID 就无法进入 Shop 粒度。
     */
    @Test
    public void testNullShopIdProducesNothing() {

        DwdOrderInfo current = order(
                1011L,
                null,
                3011L,
                "10.00",
                "2026-09-20 12:04:00",
                0,
                null,
                1000L
        );

        Assert.assertTrue(
                ShopDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * userId == null：
     *
     * 不能因此丢掉订单事实。
     *
     * 后续 AggregateFunction 会负责：
     * 不把 null userId 加入 UV Set。
     */
    @Test
    public void testNullUserStillProducesOrderDelta() {

        DwdOrderInfo current = order(
                1012L,
                2001L,
                null,
                "10.00",
                "2026-09-20 12:05:00",
                0,
                null,
                1000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        Assert.assertEquals(1, result.size());

        Assert.assertNull(
                result.get(0).getUserId()
        );

        Assert.assertEquals(
                1L,
                result.get(0).getOrderCountDelta()
        );
    }

    /**
     * isPay=1 但是 payTime=null：
     *
     * SHOP_ORDER 可以产生，
     * SHOP_PAY 不可以产生。
     */
    @Test
    public void testPaidWithoutPayTimeDoesNotProducePay() {

        DwdOrderInfo current = order(
                1013L,
                2001L,
                3013L,
                "55.00",
                "2026-09-20 12:06:00",
                1,
                null,
                1000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        Assert.assertEquals(1, result.size());

        Assert.assertEquals(
                ShopDeltaCalculator.EVENT_SHOP_ORDER,
                result.get(0).getEventType()
        );
    }

    /**
     * createTime=null：
     *
     * 无法建立正确的下单 Event Time，
     * 因此不能产生 SHOP_ORDER。
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
                ShopDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * createTime 无法解析：
     *
     * 不产生 SHOP_ORDER。
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
                ShopDeltaCalculator
                        .calculate(null, current)
                        .isEmpty()
        );
    }

    /**
     * createTime 和 payTime 跨分钟。
     *
     * 两个事件必须保留各自独立的 Event Timestamp。
     */
    @Test
    public void testOrderAndPayCanBelongToDifferentMinutes() {

        String createTime =
                "2026-09-20 13:00:59";

        String payTime =
                "2026-09-20 13:01:01";

        DwdOrderInfo current = order(
                1016L,
                2001L,
                3016L,
                "100.00",
                createTime,
                1,
                payTime,
                1000L
        );

        List<ShopDelta> result =
                ShopDeltaCalculator.calculate(null, current);

        Assert.assertEquals(2, result.size());

        ShopDelta orderDelta = result.get(0);
        ShopDelta payDelta = result.get(1);

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
                .parse(value, FORMATTER)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}