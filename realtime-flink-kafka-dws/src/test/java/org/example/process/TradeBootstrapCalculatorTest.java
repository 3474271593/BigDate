package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class TradeBootstrapCalculatorTest {

    @Test
    public void shouldAggregateFinalOrderSnapshotsWithoutCountingRepeatedUpdates() {
        List<DwdOrderInfo> records = Arrays.asList(
                order(1L, "bootstrap", 0, "2019-06-22 17:28:15", null, "10.10"),
                order(2L, "bootstrap", 1, "2019-06-22 17:28:45", "2019-06-22 17:29:00", "20.20"),
                order(1L, "UPDATE", 1, "2019-06-22 17:28:15", "2019-06-22 17:30:05", "10.10"),
                order(1L, "UPDATE", 1, "2019-06-22 17:28:15", "2019-06-22 17:30:05", "10.10")
        );

        TradeBootstrapResult result = TradeBootstrapCalculator.calculate(records);

        Assert.assertEquals(4L, result.getReadCount());
        Assert.assertEquals(2L, result.getBootstrapCount());
        Assert.assertEquals(2L, result.getUpdateCount());
        Assert.assertEquals(2, result.getUniqueOrderCount());
        Assert.assertEquals(2L, result.getOrderCount());
        Assert.assertEquals(2L, result.getPaidOrderCount());
        Assert.assertEquals(0, new BigDecimal("30.30").compareTo(result.getPaidAmount()));
        Assert.assertEquals("2019-06-22 17:28:15", result.getEarliestCreateTime());
        Assert.assertEquals("2019-06-22 17:28:45", result.getLatestCreateTime());

        List<TradeSummary> summaries = result.getSummaries();
        Assert.assertEquals(3, summaries.size());
        assertSummary(summaries.get(0), "2019-06-22 17:28:00", "2019-06-22 17:29:00", 2L, 0L, "0");
        assertSummary(summaries.get(1), "2019-06-22 17:29:00", "2019-06-22 17:30:00", 0L, 1L, "20.20");
        assertSummary(summaries.get(2), "2019-06-22 17:30:00", "2019-06-22 17:31:00", 0L, 1L, "10.10");
    }

    @Test
    public void shouldIgnoreNullPayTimeAndUnpaidOrdersAndAccumulateBigDecimalExactly() {
        List<DwdOrderInfo> records = Arrays.asList(
                order(1L, "bootstrap", 1, "2019-06-22 17:28:01", null, "0.10"),
                order(2L, "bootstrap", 0, "2019-06-22 17:28:02", "2019-06-22 17:28:40", "0.20"),
                order(3L, "bootstrap", 1, "2019-06-22 17:28:03", "2019-06-22 17:28:50", "0.10"),
                order(4L, "bootstrap", 1, "2019-06-22 17:28:04", "2019-06-22 17:28:55", "0.20")
        );

        TradeBootstrapResult result = TradeBootstrapCalculator.calculate(records);

        Assert.assertEquals(4L, result.getOrderCount());
        Assert.assertEquals(2L, result.getPaidOrderCount());
        Assert.assertEquals(0, new BigDecimal("0.30").compareTo(result.getPaidAmount()));
        Assert.assertEquals(1, result.getSummaries().size());
        assertSummary(result.getSummaries().get(0), "2019-06-22 17:28:00", "2019-06-22 17:29:00", 4L, 2L, "0.30");
    }

    private void assertSummary(TradeSummary summary, String start, String end,
                               long orders, long paid, String amount) {
        Assert.assertEquals(start, summary.getWindowStart());
        Assert.assertEquals(end, summary.getWindowEnd());
        Assert.assertEquals(orders, summary.getOrderCount());
        Assert.assertEquals(paid, summary.getPaidOrderCount());
        Assert.assertEquals(0, new BigDecimal(amount).compareTo(summary.getPaidAmount()));
    }

    private DwdOrderInfo order(Long id, String opType, int isPay,
                               String createTime, String payTime, String amount) {
        DwdOrderInfo order = new DwdOrderInfo();
        order.setOrderId(id);
        order.setOpType(opType);
        order.setIsPay(isPay);
        order.setCreateTime(createTime);
        order.setPayTime(payTime);
        order.setRealTotalMoney(amount == null ? null : new BigDecimal(amount));
        return order;
    }
}
