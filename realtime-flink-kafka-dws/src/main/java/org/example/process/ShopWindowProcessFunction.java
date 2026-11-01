// 文件路径：src/main/java/org/example/process/ShopWindowProcessFunction.java
package org.example.process;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.example.bean.ShopAccumulator;
import org.example.bean.ShopSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Shop 主题窗口结果转换函数。
 *
 * 将：
 *
 * ShopAccumulator
 *
 * 转换为：
 *
 * ShopSummary
 *
 * 并补充：
 *
 * shopId
 * windowStart
 * windowEnd
 * orderUserCount
 * paidUserCount
 */
public class ShopWindowProcessFunction
        extends ProcessWindowFunction<
        ShopAccumulator,
        ShopSummary,
        Long,
        TimeWindow> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    @Override
    public void process(
            Long shopId,
            Context context,
            Iterable<ShopAccumulator> elements,
            Collector<ShopSummary> out) {

        ShopAccumulator accumulator =
                elements.iterator().hasNext()
                        ? elements.iterator().next()
                        : null;

        if (accumulator == null) {
            return;
        }

        ShopSummary summary =
                new ShopSummary();

        // ============================================================
        // 1. 店铺维度
        // ============================================================

        summary.setShopId(
                shopId
        );

        // ============================================================
        // 2. 窗口时间
        // ============================================================

        summary.setWindowStart(
                formatTimestamp(
                        context.window().getStart()
                )
        );

        summary.setWindowEnd(
                formatTimestamp(
                        context.window().getEnd()
                )
        );

        // ============================================================
        // 3. 下单指标
        // ============================================================

        summary.setOrderCount(
                accumulator.getOrderCount()
        );

        summary.setOrderUserCount(
                accumulator.getOrderUserIds() == null
                        ? 0L
                        : accumulator
                        .getOrderUserIds()
                        .size()
        );

        summary.setOrderAmount(
                accumulator.getOrderAmount() == null
                        ? BigDecimal.ZERO
                        : accumulator.getOrderAmount()
        );

        // ============================================================
        // 4. 支付指标
        // ============================================================

        summary.setPaidOrderCount(
                accumulator.getPaidOrderCount()
        );

        summary.setPaidUserCount(
                accumulator.getPaidUserIds() == null
                        ? 0L
                        : accumulator
                        .getPaidUserIds()
                        .size()
        );

        summary.setPaidAmount(
                accumulator.getPaidAmount() == null
                        ? BigDecimal.ZERO
                        : accumulator.getPaidAmount()
        );

        out.collect(
                summary
        );
    }

    /**
     * Flink TimeWindow 毫秒时间戳 -> 项目统一时间字符串。
     */
    private String formatTimestamp(
            long timestamp) {

        LocalDateTime dateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestamp),
                        ZoneId.systemDefault()
                );

        return dateTime.format(
                DATE_TIME_FORMATTER
        );
    }
}