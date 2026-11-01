package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.UserSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * UserBootstrapCalculator 单元测试。
 *
 * 重点验证：
 *
 * 1. 同一个 orderId 的 UPDATE 不会重复计算订单；
 * 2. 最终未支付 -> 已支付状态能够正确生成支付事实；
 * 3. userId + minute 粒度正确；
 * 4. 同一用户同一分钟同一店铺精确去重；
 * 5. 同一用户同一分钟不同店铺正确累计；
 * 6. 下单与支付可以落入不同分钟；
 * 7. null userId 不进入 User DWS；
 * 8. null shopId 不影响订单事实，只不贡献店铺数。
 */
public class UserBootstrapCalculatorTest {

    @Test
    public void testBootstrapUsesFinalOrderSnapshot() {

        List<DwdOrderInfo> records = new ArrayList<>();

        // 订单1001：最初未支付
        records.add(order(
                1001L,
                2001L,
                3001L,
                "100.00",
                "2026-09-21 10:00:10",
                0,
                null,
                "bootstrap"
        ));

        // 同一订单后续支付
        records.add(order(
                1001L,
                2001L,
                3001L,
                "100.00",
                "2026-09-21 10:00:10",
                1,
                "2026-09-21 10:01:20",
                "UPDATE"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        Assert.assertEquals(2L, result.getReadCount());
        Assert.assertEquals(1L, result.getBootstrapCount());
        Assert.assertEquals(1L, result.getUpdateCount());

        // 两条CDC记录实际上只有一个订单
        Assert.assertEquals(1L, result.getUniqueOrderCount());

        // ORDER只能统计一次
        Assert.assertEquals(1L, result.getOrderCount());

        // 最终状态已经支付
        Assert.assertEquals(1L, result.getPaidOrderCount());

        assertMoney("100.00", result.getOrderAmount());
        assertMoney("100.00", result.getPaidAmount());

        // createTime 与 payTime 跨分钟，因此产生两个summary
        Assert.assertEquals(2, result.getSummaries().size());

        UserSummary orderSummary =
                findSummary(
                        result,
                        3001L,
                        "2026-09-21 10:00:00"
                );

        UserSummary paySummary =
                findSummary(
                        result,
                        3001L,
                        "2026-09-21 10:01:00"
                );

        Assert.assertNotNull(orderSummary);
        Assert.assertNotNull(paySummary);

        Assert.assertEquals(1L, orderSummary.getOrderCount());
        Assert.assertEquals(1L, orderSummary.getOrderShopCount());
        assertMoney("100.00", orderSummary.getOrderAmount());

        Assert.assertEquals(0L, orderSummary.getPaidOrderCount());
        Assert.assertEquals(0L, orderSummary.getPaidShopCount());

        Assert.assertEquals(0L, paySummary.getOrderCount());
        Assert.assertEquals(1L, paySummary.getPaidOrderCount());
        Assert.assertEquals(1L, paySummary.getPaidShopCount());
        assertMoney("100.00", paySummary.getPaidAmount());
    }

    @Test
    public void testSameShopIsDeduplicatedWithinUserMinute() {

        List<DwdOrderInfo> records = new ArrayList<>();

        records.add(order(
                1001L,
                2001L,
                3001L,
                "100.00",
                "2026-09-21 11:00:10",
                0,
                null,
                "bootstrap"
        ));

        records.add(order(
                1002L,
                2001L,
                3001L,
                "200.00",
                "2026-09-21 11:00:20",
                0,
                null,
                "bootstrap"
        ));

        records.add(order(
                1003L,
                2001L,
                3001L,
                "300.00",
                "2026-09-21 11:00:30",
                0,
                null,
                "bootstrap"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        Assert.assertEquals(3L, result.getUniqueOrderCount());
        Assert.assertEquals(3L, result.getOrderCount());

        Assert.assertEquals(1, result.getSummaries().size());

        UserSummary summary =
                result.getSummaries().get(0);

        Assert.assertEquals(
                Long.valueOf(3001L),
                summary.getUserId()
        );

        Assert.assertEquals(3L, summary.getOrderCount());

        // 三个订单都是同一家店
        Assert.assertEquals(1L, summary.getOrderShopCount());

        assertMoney("600.00", summary.getOrderAmount());
    }

    @Test
    public void testDifferentShopsAreCountedExactly() {

        List<DwdOrderInfo> records = new ArrayList<>();

        records.add(order(
                1001L,
                2001L,
                3001L,
                "100.00",
                "2026-09-21 12:00:10",
                0,
                null,
                "bootstrap"
        ));

        records.add(order(
                1002L,
                2002L,
                3001L,
                "200.00",
                "2026-09-21 12:00:20",
                0,
                null,
                "bootstrap"
        ));

        records.add(order(
                1003L,
                2002L,
                3001L,
                "300.00",
                "2026-09-21 12:00:30",
                0,
                null,
                "bootstrap"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        UserSummary summary =
                result.getSummaries().get(0);

        Assert.assertEquals(3L, summary.getOrderCount());

        // 2001、2002 两家店
        Assert.assertEquals(2L, summary.getOrderShopCount());

        assertMoney("600.00", summary.getOrderAmount());
    }

    @Test
    public void testPaidShopDeduplication() {

        List<DwdOrderInfo> records = new ArrayList<>();

        records.add(order(
                1001L,
                2001L,
                3001L,
                "100.00",
                "2026-09-21 13:00:01",
                1,
                "2026-09-21 13:01:10",
                "bootstrap"
        ));

        records.add(order(
                1002L,
                2001L,
                3001L,
                "200.00",
                "2026-09-21 13:00:02",
                1,
                "2026-09-21 13:01:20",
                "bootstrap"
        ));

        records.add(order(
                1003L,
                2002L,
                3001L,
                "300.00",
                "2026-09-21 13:00:03",
                1,
                "2026-09-21 13:01:30",
                "bootstrap"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        UserSummary paySummary =
                findSummary(
                        result,
                        3001L,
                        "2026-09-21 13:01:00"
                );

        Assert.assertNotNull(paySummary);

        Assert.assertEquals(3L, paySummary.getPaidOrderCount());

        // 支付涉及2001、2002两家店
        Assert.assertEquals(2L, paySummary.getPaidShopCount());

        assertMoney("600.00", paySummary.getPaidAmount());
    }

    @Test
    public void testDifferentUsersAreSeparated() {

        List<DwdOrderInfo> records = new ArrayList<>();

        records.add(order(
                1001L,
                2001L,
                3001L,
                "100.00",
                "2026-09-21 14:00:10",
                0,
                null,
                "bootstrap"
        ));

        records.add(order(
                1002L,
                2001L,
                3002L,
                "200.00",
                "2026-09-21 14:00:20",
                0,
                null,
                "bootstrap"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        Assert.assertEquals(2, result.getSummaries().size());

        Assert.assertNotNull(
                findSummary(
                        result,
                        3001L,
                        "2026-09-21 14:00:00"
                )
        );

        Assert.assertNotNull(
                findSummary(
                        result,
                        3002L,
                        "2026-09-21 14:00:00"
                )
        );
    }

    @Test
    public void testNullUserIsIgnored() {

        List<DwdOrderInfo> records = new ArrayList<>();

        records.add(order(
                1001L,
                2001L,
                null,
                "100.00",
                "2026-09-21 15:00:10",
                0,
                null,
                "bootstrap"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        /*
         * orderId本身仍然参与CDC最终快照统计，
         * 所以 uniqueOrderCount = 1。
         *
         * 但无法进入User主题指标。
         */
        Assert.assertEquals(1L, result.getUniqueOrderCount());

        Assert.assertEquals(0L, result.getOrderCount());
        Assert.assertEquals(0L, result.getPaidOrderCount());

        Assert.assertTrue(result.getSummaries().isEmpty());
    }

    @Test
    public void testNullShopStillCountsUserOrder() {

        List<DwdOrderInfo> records = new ArrayList<>();

        records.add(order(
                1001L,
                null,
                3001L,
                "100.00",
                "2026-09-21 16:00:10",
                0,
                null,
                "bootstrap"
        ));

        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(records);

        Assert.assertEquals(1L, result.getOrderCount());
        Assert.assertEquals(1, result.getSummaries().size());

        UserSummary summary =
                result.getSummaries().get(0);

        Assert.assertEquals(1L, summary.getOrderCount());

        // null shopId不能贡献店铺数
        Assert.assertEquals(0L, summary.getOrderShopCount());

        assertMoney("100.00", summary.getOrderAmount());
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
            String opType) {

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

        order.setOpType(opType);
        order.setProcessTs(System.currentTimeMillis());

        return order;
    }

    private UserSummary findSummary(
            UserBootstrapResult result,
            Long userId,
            String windowStart) {

        for (UserSummary summary : result.getSummaries()) {

            if (userId.equals(summary.getUserId())
                    && windowStart.equals(summary.getWindowStart())) {

                return summary;
            }
        }

        return null;
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
}