// 文件路径：src/main/java/org/example/process/TradeWindowProcessFunction.java
package org.example.process;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.example.bean.TradeSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * DWS交易主题窗口结果处理函数。
 *
 * TradeAggregateFunction负责指标聚合，
 * 本函数负责给聚合结果补充窗口开始时间和结束时间。
 */
public class TradeWindowProcessFunction
        extends ProcessWindowFunction<TradeSummary, TradeSummary, String, TimeWindow> {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE_ID);

    @Override
    public void process(
            String key,
            Context context,
            Iterable<TradeSummary> elements,
            Collector<TradeSummary> out) {

        if (elements == null || !elements.iterator().hasNext()) {
            return;
        }

        TradeSummary summary = elements.iterator().next();

        /*
         * 获取真实Flink窗口边界。
         */
        fillWindowTime(
                summary,
                context.window().getStart(),
                context.window().getEnd()
        );

        out.collect(summary);
    }

    /**
     * 给TradeSummary写入窗口开始时间和结束时间。
     *
     * 单独提取该方法是为了让窗口时间转换逻辑能够进行普通JUnit测试，
     * 不需要人为构造Flink内部Context。
     */
    void fillWindowTime(
            TradeSummary summary,
            long windowStart,
            long windowEnd) {

        if (summary == null) {
            return;
        }

        summary.setWindowStart(
                formatTimestamp(windowStart)
        );

        summary.setWindowEnd(
                formatTimestamp(windowEnd)
        );
    }

    /**
     * 毫秒时间戳转换为业务时间。
     */
    private String formatTimestamp(long timestamp) {

        return DATE_TIME_FORMATTER.format(
                Instant.ofEpochMilli(timestamp)
        );
    }
}