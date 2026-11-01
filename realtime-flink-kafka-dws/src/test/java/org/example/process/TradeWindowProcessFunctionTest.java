// 文件路径：src/test/java/org/example/process/TradeWindowProcessFunctionTest.java
package org.example.process;

import org.example.bean.TradeSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * TradeWindowProcessFunction单元测试。
 *
 * 验证：
 * 1. windowStart写入正确；
 * 2. windowEnd写入正确；
 * 3. 原有交易指标不会被修改。
 *
 * 本测试：
 * 不启动Flink Job；
 * 不连接Kafka；
 * 不调用env.execute()。
 */
public class TradeWindowProcessFunctionTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    @Test
    public void shouldFillWindowStartAndEndCorrectly() {

        // ============================================================
        // 1. 准备窗口聚合结果
        // ============================================================

        TradeSummary summary = new TradeSummary();

        summary.setOrderCount(2L);
        summary.setPaidOrderCount(1L);
        summary.setPaidAmount(
                new BigDecimal("100.50")
        );

        // ============================================================
        // 2. 构造一分钟窗口边界
        //
        // 2019-06-22 17:28:00
        //          ↓
        // 2019-06-22 17:29:00
        // ============================================================

        long windowStart =
                toTimestamp("2019-06-22 17:28:00");

        long windowEnd =
                toTimestamp("2019-06-22 17:29:00");

        // ============================================================
        // 3. 调用窗口时间填充逻辑
        // ============================================================

        TradeWindowProcessFunction function =
                new TradeWindowProcessFunction();

        function.fillWindowTime(
                summary,
                windowStart,
                windowEnd
        );

        // ============================================================
        // 4. 验证窗口开始时间
        // ============================================================

        Assert.assertEquals(
                "2019-06-22 17:28:00",
                summary.getWindowStart()
        );

        // ============================================================
        // 5. 验证窗口结束时间
        // ============================================================

        Assert.assertEquals(
                "2019-06-22 17:29:00",
                summary.getWindowEnd()
        );

        // ============================================================
        // 6. 验证订单数没有发生变化
        // ============================================================

        Assert.assertEquals(
                2L,
                summary.getOrderCount()
        );

        // ============================================================
        // 7. 验证支付订单数没有发生变化
        // ============================================================

        Assert.assertEquals(
                1L,
                summary.getPaidOrderCount()
        );

        // ============================================================
        // 8. 验证支付金额没有发生变化
        // ============================================================

        Assert.assertEquals(
                0,
                new BigDecimal("100.50")
                        .compareTo(summary.getPaidAmount())
        );
    }

    /**
     * 将业务时间转换为毫秒时间戳。
     */
    private long toTimestamp(String time) {

        return LocalDateTime
                .parse(time, DATE_TIME_FORMATTER)
                .atZone(ZONE_ID)
                .toInstant()
                .toEpochMilli();
    }
}