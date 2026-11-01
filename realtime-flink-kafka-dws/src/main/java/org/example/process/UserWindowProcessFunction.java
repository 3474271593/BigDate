// 文件路径：src/main/java/org/example/process/UserWindowProcessFunction.java
package org.example.process;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import org.example.bean.UserAccumulator;
import org.example.bean.UserSummary;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * User DWS 用户主题窗口结果处理函数。
 *
 * 输入：
 * UserAccumulator
 *
 * 输出：
 * UserSummary
 *
 * 窗口粒度：
 * userId + 1分钟
 *
 * 负责：
 *
 * 1. 获取 userId
 * 2. 获取窗口开始时间
 * 3. 获取窗口结束时间
 * 4. 将 orderShopIds.size() 转成 orderShopCount
 * 5. 将 paidShopIds.size() 转成 paidShopCount
 * 6. 生成最终 UserSummary
 */
public class UserWindowProcessFunction
        extends ProcessWindowFunction<
        UserAccumulator,
        UserSummary,
        Long,
        TimeWindow> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    @Override
    public void process(
            Long userId,
            Context context,
            Iterable<UserAccumulator> elements,
            Collector<UserSummary> out) {

        if (elements == null) {
            return;
        }

        /*
         * 前面使用 AggregateFunction，
         * 因此这里正常情况下只会得到一个聚合后的
         * UserAccumulator。
         */
        UserAccumulator accumulator = null;

        for (UserAccumulator element : elements) {
            accumulator = element;
            break;
        }

        if (accumulator == null) {
            return;
        }

        // ------------------------------------------------------------
        // 1. 获取窗口时间
        // ------------------------------------------------------------

        long windowStartTs =
                context.window().getStart();

        long windowEndTs =
                context.window().getEnd();

        String windowStart =
                formatTimestamp(
                        windowStartTs
                );

        String windowEnd =
                formatTimestamp(
                        windowEndTs
                );

        // ------------------------------------------------------------
        // 2. 生成最终 UserSummary
        // ------------------------------------------------------------

        UserSummary summary =
                new UserSummary();

        /*
         * 优先使用 keyBy 后传入的 userId。
         */
        summary.setUserId(
                userId
        );

        summary.setWindowStart(
                windowStart
        );

        summary.setWindowEnd(
                windowEnd
        );

        // ------------------------------------------------------------
        // 3. 下单指标
        // ------------------------------------------------------------

        summary.setOrderCount(
                accumulator.getOrderCount()
        );

        summary.setOrderShopCount(
                accumulator.getOrderShopCount()
        );

        summary.setOrderAmount(
                accumulator.getOrderAmount()
        );

        // ------------------------------------------------------------
        // 4. 支付指标
        // ------------------------------------------------------------

        summary.setPaidOrderCount(
                accumulator.getPaidOrderCount()
        );

        summary.setPaidShopCount(
                accumulator.getPaidShopCount()
        );

        summary.setPaidAmount(
                accumulator.getPaidAmount()
        );

        // ------------------------------------------------------------
        // 5. 输出
        // ------------------------------------------------------------

        out.collect(
                summary
        );
    }

    /**
     * 毫秒时间戳转换为：
     *
     * yyyy-MM-dd HH:mm:ss
     *
     * 与当前项目其他 DWS 主题保持一致，
     * 使用系统默认时区。
     */
    private static String formatTimestamp(
            long timestamp) {

        LocalDateTime dateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(
                                timestamp
                        ),
                        ZoneId.systemDefault()
                );

        return dateTime.format(
                DATE_TIME_FORMATTER
        );
    }
}