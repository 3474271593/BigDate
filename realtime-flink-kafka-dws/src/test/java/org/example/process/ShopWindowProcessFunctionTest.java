// 文件路径：src/test/java/org/example/process/ShopWindowProcessFunctionTest.java
package org.example.process;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.CloseableIterator;
import org.example.bean.ShopDelta;
import org.example.bean.ShopSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Shop主题 Event Time + 1分钟滚动窗口测试。
 *
 * 验证完整链路：
 *
 * ShopDelta
 *      ↓
 * keyBy(shopId)
 *      ↓
 * 1分钟 Event Time Window
 *      ↓
 * ShopAggregateFunction
 *      ↓
 * ShopWindowProcessFunction
 *      ↓
 * ShopSummary
 *
 * 重点验证：
 * 1. shopId
 * 2. windowStart / windowEnd
 * 3. orderCount
 * 4. orderUserCount 精确UV
 * 5. orderAmount
 * 6. paidOrderCount
 * 7. paidUserCount 精确UV
 * 8. paidAmount
 */
public class ShopWindowProcessFunctionTest {

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    @Test
    public void testOneMinuteShopWindow()
            throws Exception {

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment
                        .getExecutionEnvironment();

        env.setParallelism(1);

        /*
         * 同一个店铺，同一分钟。
         *
         * 用户10001：
         *   下单一次 + 支付一次
         *
         * 用户10002：
         *   下单两次
         *
         * 最终应得到：
         *
         * orderCount     = 3
         * orderUserCount = 2
         * orderAmount    = 60
         *
         * paidOrderCount = 1
         * paidUserCount  = 1
         * paidAmount     = 10
         */

        ShopDelta order1 =
                createDelta(
                        1L,
                        100001L,
                        10001L,
                        1L,
                        "10.00",
                        0L,
                        "0",
                        ShopDeltaCalculator.EVENT_SHOP_ORDER,
                        "2019-06-22 17:28:10"
                );

        ShopDelta pay1 =
                createDelta(
                        1L,
                        100001L,
                        10001L,
                        0L,
                        "0",
                        1L,
                        "10.00",
                        ShopDeltaCalculator.EVENT_SHOP_PAY,
                        "2019-06-22 17:28:20"
                );

        ShopDelta order2 =
                createDelta(
                        2L,
                        100001L,
                        10002L,
                        1L,
                        "20.00",
                        0L,
                        "0",
                        ShopDeltaCalculator.EVENT_SHOP_ORDER,
                        "2019-06-22 17:28:30"
                );

        /*
         * 同一个用户10002再次下单。
         *
         * orderCount 应继续 +1，
         * 但是 orderUserCount 不能 +1。
         */
        ShopDelta order3 =
                createDelta(
                        3L,
                        100001L,
                        10002L,
                        1L,
                        "30.00",
                        0L,
                        "0",
                        ShopDeltaCalculator.EVENT_SHOP_ORDER,
                        "2019-06-22 17:28:40"
                );

        DataStream<ShopDelta> source =
                env.fromElements(
                        order1,
                        pay1,
                        order2,
                        order3
                );

        /*
         * 测试环境直接根据 ShopDelta.eventTs
         * 分配 Event Time。
         */
        SingleOutputStreamOperator<ShopDelta> withWatermark =
                source.assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<ShopDelta>forMonotonousTimestamps()
                                .withTimestampAssigner(
                                        (element, recordTimestamp) ->
                                                element.getEventTs()
                                )
                );

        /*
         * Shop主题：
         *
         * keyBy(shopId)
         *      ↓
         * 1分钟滚动窗口
         *      ↓
         * ShopAggregateFunction
         *      ↓
         * ShopWindowProcessFunction
         */
        SingleOutputStreamOperator<ShopSummary> result =
                withWatermark
                        .keyBy(
                                ShopDelta::getShopId
                        )
                        .window(
                                TumblingEventTimeWindows.of(
                                        Time.minutes(1)
                                )
                        )
                        .aggregate(
                                new ShopAggregateFunction(),
                                new ShopWindowProcessFunction()
                        );

        List<ShopSummary> results =
                new ArrayList<>();

        try (CloseableIterator<ShopSummary> iterator =
                     result.executeAndCollect()) {

            while (iterator.hasNext()) {
                results.add(
                        iterator.next()
                );
            }
        }

        /*
         * 所有事件属于：
         *
         * [2019-06-22 17:28:00,
         *  2019-06-22 17:29:00)
         *
         * 所以只能产生一个窗口结果。
         */
        Assert.assertEquals(
                1,
                results.size()
        );

        ShopSummary summary =
                results.get(0);

        // 店铺ID
        Assert.assertEquals(
                Long.valueOf(100001L),
                summary.getShopId()
        );

        // 窗口开始时间
        Assert.assertEquals(
                "2019-06-22 17:28:00",
                summary.getWindowStart()
        );

        // 窗口结束时间
        Assert.assertEquals(
                "2019-06-22 17:29:00",
                summary.getWindowEnd()
        );

        // ------------------------------------------------------------
        // 下单指标
        // ------------------------------------------------------------

        // 3笔订单
        Assert.assertEquals(
                3L,
                summary.getOrderCount()
        );

        /*
         * 用户：
         *
         * 10001
         * 10002
         * 10002
         *
         * 精确UV = 2
         */
        Assert.assertEquals(
                2L,
                summary.getOrderUserCount()
        );

        // 10 + 20 + 30 = 60
        assertDecimalEquals(
                "60.00",
                summary.getOrderAmount()
        );

        // ------------------------------------------------------------
        // 支付指标
        // ------------------------------------------------------------

        // 只有 order1 支付
        Assert.assertEquals(
                1L,
                summary.getPaidOrderCount()
        );

        // 支付用户只有10001
        Assert.assertEquals(
                1L,
                summary.getPaidUserCount()
        );

        assertDecimalEquals(
                "10.00",
                summary.getPaidAmount()
        );
    }

    /**
     * 创建测试 ShopDelta。
     */
    private ShopDelta createDelta(
            Long orderId,
            Long shopId,
            Long userId,
            long orderCountDelta,
            String orderAmountDelta,
            long paidOrderCountDelta,
            String paidAmountDelta,
            String eventType,
            String eventTime) {

        ShopDelta delta =
                new ShopDelta();

        delta.setOrderId(
                orderId
        );

        delta.setShopId(
                shopId
        );

        delta.setUserId(
                userId
        );

        delta.setOrderCountDelta(
                orderCountDelta
        );

        delta.setOrderAmountDelta(
                new BigDecimal(
                        orderAmountDelta
                )
        );

        delta.setPaidOrderCountDelta(
                paidOrderCountDelta
        );

        delta.setPaidAmountDelta(
                new BigDecimal(
                        paidAmountDelta
                )
        );

        delta.setEventType(
                eventType
        );

        delta.setEventTime(
                eventTime
        );

        delta.setEventTs(
                parseTimestamp(
                        eventTime
                )
        );

        return delta;
    }

    /**
     * yyyy-MM-dd HH:mm:ss
     * 转毫秒时间戳。
     */
    private long parseTimestamp(
            String time) {

        return LocalDateTime
                .parse(
                        time,
                        FORMATTER
                )
                .atZone(
                        ZONE_ID
                )
                .toInstant()
                .toEpochMilli();
    }

    private void assertDecimalEquals(
            String expected,
            BigDecimal actual) {

        Assert.assertNotNull(
                actual
        );

        Assert.assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}