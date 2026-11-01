package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * ShopBootstrapCalculator 历史基线计算测试。
 *
 * 重点验证：
 * 1. orderId 最终快照去重；
 * 2. createTime / payTime 分别进入对应分钟；
 * 3. 同店同分钟用户精确 UV；
 * 4. 不同 shopId 相互隔离；
 * 5. null userId 不影响订单事实指标；
 * 6. 未支付订单不产生支付指标。
 */
public class ShopBootstrapCalculatorTest {

    @Test
    public void testFinalSnapshotShouldRemoveDuplicateOrder() {

        DwdOrderInfo bootstrap = order(
                1001L,
                10L,
                101L,
                "100.00",
                "2026-09-20 10:00:10",
                0,
                null,
                "bootstrap"
        );

        DwdOrderInfo update = order(
                1001L,
                10L,
                101L,
                "100.00",
                "2026-09-20 10:00:10",
                1,
                "2026-09-20 10:01:20",
                "UPDATE"
        );

        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(
                        Arrays.asList(
                                bootstrap,
                                update
                        )
                );

        Assert.assertEquals(
                2L,
                result.getReadCount()
        );

        Assert.assertEquals(
                1L,
                result.getBootstrapCount()
        );

        Assert.assertEquals(
                1L,
                result.getUpdateCount()
        );

        Assert.assertEquals(
                1L,
                result.getUniqueOrderCount()
        );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                1L,
                result.getPaidOrderCount()
        );

        assertMoney(
                "100.00",
                result.getOrderAmount()
        );

        assertMoney(
                "100.00",
                result.getPaidAmount()
        );
    }

    @Test
    public void testCreateAndPayShouldEnterDifferentMinutes() {

        DwdOrderInfo order = order(
                2001L,
                20L,
                201L,
                "88.50",
                "2026-09-20 11:25:30",
                1,
                "2026-09-20 11:28:15",
                "bootstrap"
        );

        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(
                        Arrays.asList(order)
                );

        Assert.assertEquals(
                2,
                result.getSummaries().size()
        );

        ShopSummary orderWindow =
                find(
                        result.getSummaries(),
                        20L,
                        "2026-09-20 11:25:00"
                );

        ShopSummary payWindow =
                find(
                        result.getSummaries(),
                        20L,
                        "2026-09-20 11:28:00"
                );

        Assert.assertEquals(
                1L,
                orderWindow.getOrderCount()
        );

        Assert.assertEquals(
                1L,
                orderWindow.getOrderUserCount()
        );

        assertMoney(
                "88.50",
                orderWindow.getOrderAmount()
        );

        Assert.assertEquals(
                0L,
                orderWindow.getPaidOrderCount()
        );

        Assert.assertEquals(
                0L,
                orderWindow.getPaidUserCount()
        );

        assertMoney(
                "0",
                orderWindow.getPaidAmount()
        );

        Assert.assertEquals(
                0L,
                payWindow.getOrderCount()
        );

        Assert.assertEquals(
                0L,
                payWindow.getOrderUserCount()
        );

        assertMoney(
                "0",
                payWindow.getOrderAmount()
        );

        Assert.assertEquals(
                1L,
                payWindow.getPaidOrderCount()
        );

        Assert.assertEquals(
                1L,
                payWindow.getPaidUserCount()
        );

        assertMoney(
                "88.50",
                payWindow.getPaidAmount()
        );
    }

    @Test
    public void testExactUvForSameUserInSameMinute() {

        DwdOrderInfo order1 = order(
                3001L,
                30L,
                301L,
                "10.00",
                "2026-09-20 12:00:10",
                1,
                "2026-09-20 12:01:10",
                "bootstrap"
        );

        DwdOrderInfo order2 = order(
                3002L,
                30L,
                301L,
                "20.00",
                "2026-09-20 12:00:30",
                1,
                "2026-09-20 12:01:30",
                "bootstrap"
        );

        DwdOrderInfo order3 = order(
                3003L,
                30L,
                302L,
                "30.00",
                "2026-09-20 12:00:50",
                1,
                "2026-09-20 12:01:50",
                "bootstrap"
        );

        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(
                        Arrays.asList(
                                order1,
                                order2,
                                order3
                        )
                );

        ShopSummary orderWindow =
                find(
                        result.getSummaries(),
                        30L,
                        "2026-09-20 12:00:00"
                );

        Assert.assertEquals(
                3L,
                orderWindow.getOrderCount()
        );

        Assert.assertEquals(
                2L,
                orderWindow.getOrderUserCount()
        );

        assertMoney(
                "60.00",
                orderWindow.getOrderAmount()
        );

        ShopSummary payWindow =
                find(
                        result.getSummaries(),
                        30L,
                        "2026-09-20 12:01:00"
                );

        Assert.assertEquals(
                3L,
                payWindow.getPaidOrderCount()
        );

        Assert.assertEquals(
                2L,
                payWindow.getPaidUserCount()
        );

        assertMoney(
                "60.00",
                payWindow.getPaidAmount()
        );
    }

    @Test
    public void testDifferentShopsShouldBeSeparated() {

        DwdOrderInfo shop10 = order(
                4001L,
                10L,
                401L,
                "50.00",
                "2026-09-20 13:00:10",
                0,
                null,
                "bootstrap"
        );

        DwdOrderInfo shop20 = order(
                4002L,
                20L,
                401L,
                "70.00",
                "2026-09-20 13:00:20",
                0,
                null,
                "bootstrap"
        );

        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(
                        Arrays.asList(
                                shop10,
                                shop20
                        )
                );

        Assert.assertEquals(
                2,
                result.getSummaries().size()
        );

        ShopSummary summary10 =
                find(
                        result.getSummaries(),
                        10L,
                        "2026-09-20 13:00:00"
                );

        ShopSummary summary20 =
                find(
                        result.getSummaries(),
                        20L,
                        "2026-09-20 13:00:00"
                );

        Assert.assertEquals(
                1L,
                summary10.getOrderCount()
        );

        assertMoney(
                "50.00",
                summary10.getOrderAmount()
        );

        Assert.assertEquals(
                1L,
                summary20.getOrderCount()
        );

        assertMoney(
                "70.00",
                summary20.getOrderAmount()
        );
    }

    @Test
    public void testNullUserShouldCountOrderButNotUv() {

        DwdOrderInfo order = order(
                5001L,
                50L,
                null,
                "99.00",
                "2026-09-20 14:00:10",
                1,
                "2026-09-20 14:00:30",
                "bootstrap"
        );

        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(
                        Arrays.asList(order)
                );

        ShopSummary summary =
                find(
                        result.getSummaries(),
                        50L,
                        "2026-09-20 14:00:00"
                );

        Assert.assertEquals(
                1L,
                summary.getOrderCount()
        );

        Assert.assertEquals(
                0L,
                summary.getOrderUserCount()
        );

        Assert.assertEquals(
                1L,
                summary.getPaidOrderCount()
        );

        Assert.assertEquals(
                0L,
                summary.getPaidUserCount()
        );

        assertMoney(
                "99.00",
                summary.getOrderAmount()
        );

        assertMoney(
                "99.00",
                summary.getPaidAmount()
        );
    }

    @Test
    public void testUnpaidOrderShouldNotCreatePayMetric() {

        DwdOrderInfo order = order(
                6001L,
                60L,
                601L,
                "66.00",
                "2026-09-20 15:10:10",
                0,
                null,
                "bootstrap"
        );

        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(
                        Arrays.asList(order)
                );

        Assert.assertEquals(
                1L,
                result.getOrderCount()
        );

        Assert.assertEquals(
                0L,
                result.getPaidOrderCount()
        );

        assertMoney(
                "66.00",
                result.getOrderAmount()
        );

        assertMoney(
                "0",
                result.getPaidAmount()
        );

        Assert.assertEquals(
                1,
                result.getSummaries().size()
        );
    }

    private static DwdOrderInfo order(
            Long orderId,
            Long shopId,
            Long userId,
            String amount,
            String createTime,
            Integer isPay,
            String payTime,
            String opType) {

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(orderId);
        order.setShopId(shopId);
        order.setUserId(userId);

        order.setRealTotalMoney(
                amount == null
                        ? null
                        : new BigDecimal(amount)
        );

        order.setCreateTime(createTime);
        order.setIsPay(isPay);
        order.setPayTime(payTime);
        order.setOpType(opType);

        return order;
    }

    private static ShopSummary find(
            List<ShopSummary> summaries,
            Long shopId,
            String windowStart) {

        for (ShopSummary summary : summaries) {

            if (shopId.equals(summary.getShopId())
                    && windowStart.equals(
                    summary.getWindowStart())) {

                return summary;
            }
        }

        Assert.fail(
                "ShopSummary not found: shopId="
                        + shopId
                        + ", windowStart="
                        + windowStart
        );

        return null;
    }

    private static void assertMoney(
            String expected,
            BigDecimal actual) {

        Assert.assertNotNull(actual);

        Assert.assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}