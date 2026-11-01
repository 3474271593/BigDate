// 文件路径：src/test/java/org/example/process/GoodsWindowProcessFunctionTest.java
package org.example.process;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.api.java.functions.KeySelector;
import org.example.bean.GoodsDelta;
import org.example.bean.GoodsSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品主题 Event Time + 1分钟滚动窗口测试。
 *
 * 验证：
 *
 * GoodsDelta
 *     ↓
 * keyBy(goodsId)
 *     ↓
 * 1分钟 Event Time Window
 *     ↓
 * GoodsAggregateFunction
 *     ↓
 * GoodsWindowProcessFunction
 *     ↓
 * GoodsSummary
 *
 * 重点检查：
 * 1. goodsId
 * 2. windowStart
 * 3. windowEnd
 * 4. 聚合指标
 */
public class GoodsWindowProcessFunctionTest {

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    /**
     * 验证：
     *
     * 2019-06-22 17:28:15
     *
     * 应进入：
     *
     * [17:28:00, 17:29:00)
     */
    @Test
    public void testOneMinuteGoodsWindow()
            throws Exception {

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment
                        .getExecutionEnvironment();

        env.setParallelism(1);

        /*
         * 第一条：
         *
         * GOODS_ORDER
         *
         * +1订单
         * +1商品
         * +11.60
         */
        GoodsDelta orderDelta =
                createDelta(
                        100125L,
                        "三只松鼠 无核白葡萄干",
                        1L,
                        1L,
                        "11.60",
                        0L,
                        0L,
                        "0",
                        "2019-06-22 17:28:15"
                );

        /*
         * 第二条：
         *
         * GOODS_UPDATE_CORRECTION
         *
         * +0订单
         * +2商品
         * +23.20
         */
        GoodsDelta correctionDelta =
                createDelta(
                        100125L,
                        "三只松鼠 无核白葡萄干",
                        0L,
                        2L,
                        "23.20",
                        0L,
                        0L,
                        "0",
                        "2019-06-22 17:28:30"
                );

        DataStream<GoodsDelta> source =
                env.fromElements(
                        orderDelta,
                        correctionDelta
                );

        /*
         * 测试环境中直接给事件分配时间戳和Watermark。
         *
         * 这里使用较小的乱序容忍，
         * 目的只是验证窗口逻辑。
         */
        SingleOutputStreamOperator<GoodsDelta> withWatermark =
                source.assignTimestampsAndWatermarks(
                        org.apache.flink.api.common.eventtime
                                .WatermarkStrategy
                                .<GoodsDelta>forMonotonousTimestamps()
                                .withTimestampAssigner(
                                        (element, recordTimestamp) ->
                                                element.getEventTs()
                                )
                );

        SingleOutputStreamOperator<GoodsSummary> result =
                withWatermark
                        .keyBy(
                                GoodsDelta::getGoodsId
                        )
                        .window(
                                TumblingEventTimeWindows.of(
                                        Time.minutes(1)
                                )
                        )
                        .aggregate(
                                new GoodsAggregateFunction(),
                                new GoodsWindowProcessFunction()
                        );

        /*
         * 使用executeAndCollect直接取得测试结果。
         */
        List<GoodsSummary> results =
                new ArrayList<>();

        try (org.apache.flink.util.CloseableIterator<GoodsSummary> iterator =
                     result.executeAndCollect()) {

            while (iterator.hasNext()) {

                results.add(
                        iterator.next()
                );
            }
        }

        Assert.assertEquals(
                1,
                results.size()
        );

        GoodsSummary summary =
                results.get(0);

        Assert.assertEquals(
                Long.valueOf(100125L),
                summary.getGoodsId()
        );

        Assert.assertEquals(
                "三只松鼠 无核白葡萄干",
                summary.getGoodsName()
        );

        /*
         * 17:28:15 和 17:28:30
         * 都属于：
         *
         * [17:28:00, 17:29:00)
         */
        Assert.assertEquals(
                "2019-06-22 17:28:00",
                summary.getWindowStart()
        );

        Assert.assertEquals(
                "2019-06-22 17:29:00",
                summary.getWindowEnd()
        );

        /*
         * 1 + Correction
         *
         * 商品数量：
         *
         * 1 + 2 = 3
         */
        Assert.assertEquals(
                1L,
                summary.getOrderCount()
        );

        Assert.assertEquals(
                3L,
                summary.getOrderGoodsNum()
        );

        assertDecimalEquals(
                "34.80",
                summary.getOrderAmount()
        );

        Assert.assertEquals(
                0L,
                summary.getPaidOrderCount()
        );

        Assert.assertEquals(
                0L,
                summary.getPaidGoodsNum()
        );

        assertDecimalEquals(
                "0",
                summary.getPaidAmount()
        );
    }

    /**
     * 创建GoodsDelta。
     */
    private GoodsDelta createDelta(
            Long goodsId,
            String goodsName,
            long orderCountDelta,
            long orderGoodsNumDelta,
            String orderAmountDelta,
            long paidOrderCountDelta,
            long paidGoodsNumDelta,
            String paidAmountDelta,
            String eventTime) {

        GoodsDelta delta =
                new GoodsDelta();

        delta.setGoodsId(
                goodsId
        );

        delta.setGoodsName(
                goodsName
        );

        delta.setOrderCountDelta(
                orderCountDelta
        );

        delta.setOrderGoodsNumDelta(
                orderGoodsNumDelta
        );

        delta.setOrderAmountDelta(
                new BigDecimal(
                        orderAmountDelta
                )
        );

        delta.setPaidOrderCountDelta(
                paidOrderCountDelta
        );

        delta.setPaidGoodsNumDelta(
                paidGoodsNumDelta
        );

        delta.setPaidAmountDelta(
                new BigDecimal(
                        paidAmountDelta
                )
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