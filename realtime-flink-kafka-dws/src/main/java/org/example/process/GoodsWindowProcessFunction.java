// 文件路径：src/main/java/org/example/process/GoodsWindowProcessFunction.java
package org.example.process;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.example.bean.GoodsSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 商品主题窗口结果处理函数。
 *
 * GoodsAggregateFunction负责：
 * - orderCount
 * - orderGoodsNum
 * - orderAmount
 * - paidOrderCount
 * - paidGoodsNum
 * - paidAmount
 *
 * 本类负责补充：
 * - goodsId
 * - windowStart
 * - windowEnd
 */
public class GoodsWindowProcessFunction
        extends ProcessWindowFunction<
        GoodsSummary,
        GoodsSummary,
        Long,
        TimeWindow> {

    /**
     * 与现有业务时间统一使用 Asia/Shanghai。
     */
    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    @Override
    public void process(
            Long goodsId,
            Context context,
            Iterable<GoodsSummary> elements,
            Collector<GoodsSummary> out) {

        if (elements == null) {
            return;
        }

        /*
         * AggregateFunction已经把窗口中的所有GoodsDelta
         * 聚合成一个GoodsSummary。
         *
         * 因此这里正常只有一个聚合结果。
         */
        GoodsSummary summary = null;

        for (GoodsSummary element : elements) {
            summary = element;
            break;
        }

        if (summary == null) {
            return;
        }

        /*
         * keyBy(goodsId)之后，
         * 当前窗口的Key就是goodsId。
         */
        summary.setGoodsId(
                goodsId
        );

        /*
         * Flink TimeWindow：
         *
         * getStart()：
         * 窗口开始时间
         *
         * getEnd()：
         * 窗口结束时间
         *
         * 例如：
         *
         * 17:28:00
         *     ↓
         * 17:29:00
         */
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

        out.collect(
                summary
        );
    }

    /**
     * 毫秒时间戳转换为：
     *
     * yyyy-MM-dd HH:mm:ss
     *
     * 时区：
     * Asia/Shanghai
     */
    private String formatTimestamp(
            long timestamp) {

        return Instant
                .ofEpochMilli(timestamp)
                .atZone(ZONE_ID)
                .format(FORMATTER);
    }
}