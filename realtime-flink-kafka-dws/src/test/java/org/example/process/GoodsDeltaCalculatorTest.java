// 文件路径：src/test/java/org/example/process/GoodsDeltaCalculatorTest.java
package org.example.process;

import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsDelta;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * GoodsDeltaCalculator 单元测试。
 */
public class GoodsDeltaCalculatorTest {

    private GoodsDeltaCalculator calculator;

    @Before
    public void setUp() {

        calculator =
                new GoodsDeltaCalculator();
    }

    /**
     * 正常商品下单事件。
     */
    @Test
    public void testCreateOrderDelta() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods goods =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                );

        GoodsDelta delta =
                calculator.createOrderDelta(
                        order,
                        goods
                );

        Assert.assertNotNull(delta);

        Assert.assertEquals(
                "GOODS_ORDER",
                delta.getEventType()
        );

        Assert.assertEquals(
                1L,
                delta.getOrderCountDelta()
        );

        Assert.assertEquals(
                2L,
                delta.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "23.20",
                delta.getOrderAmountDelta()
        );

        Assert.assertEquals(
                0L,
                delta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                0L,
                delta.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "0",
                delta.getPaidAmountDelta()
        );
    }

    /**
     * 正常支付事件。
     */
    @Test
    public void testCreatePayDelta() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        1,
                        "2019-06-22 17:29:30"
                );

        DwdOrderGoods goods =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                );

        GoodsDelta delta =
                calculator.createPayDelta(
                        order,
                        goods
                );

        Assert.assertNotNull(delta);

        Assert.assertEquals(
                "GOODS_PAY",
                delta.getEventType()
        );

        Assert.assertEquals(
                0L,
                delta.getOrderCountDelta()
        );

        Assert.assertEquals(
                1L,
                delta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                2L,
                delta.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "23.20",
                delta.getPaidAmountDelta()
        );
    }

    /**
     * 未支付订单不能产生支付Delta。
     */
    @Test
    public void testUnpaidOrderDoesNotCreatePayDelta() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods goods =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        GoodsDelta delta =
                calculator.createPayDelta(
                        order,
                        goods
                );

        Assert.assertNull(delta);
    }

    /**
     * isPay=1但payTime为空，
     * 不能伪造支付时间。
     */
    @Test
    public void testPaidWithoutPayTimeDoesNotCreatePayDelta() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        1,
                        null
                );

        DwdOrderGoods goods =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        GoodsDelta delta =
                calculator.createPayDelta(
                        order,
                        goods
                );

        Assert.assertNull(delta);
    }

    /**
     * goodsName为空不能丢事实数据。
     */
    @Test
    public void testEmptyGoodsNameStillCreatesOrderDelta() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods goods =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        goods.setGoodsName("");

        GoodsDelta delta =
                calculator.createOrderDelta(
                        order,
                        goods
                );

        Assert.assertNotNull(delta);

        Assert.assertEquals(
                Long.valueOf(100125L),
                delta.getGoodsId()
        );
    }

    /**
     * orderId不一致不能关联。
     */
    @Test
    public void testMismatchedOrderIdDoesNotJoin() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods goods =
                createGoods(
                        120228L,
                        999L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        Assert.assertNull(
                calculator.createOrderDelta(
                        order,
                        goods
                )
        );

        Assert.assertNull(
                calculator.createPayDelta(
                        order,
                        goods
                )
        );
    }

    /**
     * 商品数量：
     *
     * 1 -> 3
     *
     * 应产生：
     *
     * goodsNumDelta = +2
     * amountDelta = +23.20
     */
    @Test
    public void testCorrectionIncrease() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        3,
                        "34.80",
                        2000L
                );

        GoodsDelta delta =
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                );

        Assert.assertNotNull(delta);

        Assert.assertEquals(
                "GOODS_UPDATE_CORRECTION",
                delta.getEventType()
        );

        Assert.assertEquals(
                0L,
                delta.getOrderCountDelta()
        );

        Assert.assertEquals(
                2L,
                delta.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "23.20",
                delta.getOrderAmountDelta()
        );

        Assert.assertEquals(
                0L,
                delta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                0L,
                delta.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "0",
                delta.getPaidAmountDelta()
        );

        Assert.assertEquals(
                2000L,
                delta.getEventTs()
        );
    }

    /**
     * 商品数量：
     *
     * 3 -> 1
     *
     * 必须支持负Delta。
     */
    @Test
    public void testCorrectionDecrease() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        3,
                        "34.80",
                        1000L
                );

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        2000L
                );

        GoodsDelta delta =
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                );

        Assert.assertNotNull(delta);

        Assert.assertEquals(
                -2L,
                delta.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "-23.20",
                delta.getOrderAmountDelta()
        );

        Assert.assertEquals(
                0L,
                delta.getOrderCountDelta()
        );
    }

    /**
     * 已支付订单发生商品UPDATE。
     *
     * 下单指标和支付指标都要同步修正。
     */
    @Test
    public void testPaidOrderCorrection() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        1,
                        "2019-06-22 17:29:30"
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        4,
                        "46.40",
                        2000L
                );

        GoodsDelta delta =
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                );

        Assert.assertNotNull(delta);

        /*
         * 订单数不能增加。
         */
        Assert.assertEquals(
                0L,
                delta.getOrderCountDelta()
        );

        /*
         * 商品数量：
         * 4 - 1 = +3
         */
        Assert.assertEquals(
                3L,
                delta.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "34.80",
                delta.getOrderAmountDelta()
        );

        /*
         * 支付订单数同样不能增加。
         */
        Assert.assertEquals(
                0L,
                delta.getPaidOrderCountDelta()
        );

        /*
         * 因订单已经支付，
         * 支付商品数量和金额同步修正。
         */
        Assert.assertEquals(
                3L,
                delta.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "34.80",
                delta.getPaidAmountDelta()
        );
    }

    /**
     * 完全相同的UPDATE不产生Correction。
     *
     * 例如只是goodsName发生维度补全，
     * 数量和金额没有变化。
     */
    @Test
    public void testSameMetricsDoesNotCreateCorrection() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                );

        previous.setGoodsName("");

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        2000L
                );

        current.setGoodsName(
                "三只松鼠测试商品"
        );

        GoodsDelta delta =
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                );

        Assert.assertNull(delta);
    }

    /**
     * 不同ogId不能做Correction。
     */
    @Test
    public void testDifferentOgIdDoesNotCreateCorrection() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        DwdOrderGoods current =
                createGoods(
                        120229L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        2000L
                );

        Assert.assertNull(
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                )
        );
    }

    /**
     * 同一个ogId如果goodsId发生变化，
     * 第一版不允许简单Correction。
     */
    @Test
    public void testGoodsIdChangedDoesNotCreateCorrection() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100171L,
                        2,
                        "23.20",
                        2000L
                );

        Assert.assertNull(
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                )
        );
    }

    /**
     * Correction必须有合法processTs。
     */
    @Test
    public void testCorrectionWithoutProcessTsReturnsNull() {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        null
                );

        Assert.assertNull(
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                )
        );
    }

    /**
     * Correction eventTime必须由processTs
     * 按Asia/Shanghai转换。
     */
    @Test
    public void testCorrectionEventTimeUsesProcessTs()
            throws Exception {

        DwdOrderInfo order =
                createOrder(
                        521L,
                        0,
                        null
                );

        DwdOrderGoods previous =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                );

        long processTs =
                parseShanghaiTime(
                        "2026-09-18 10:30:00"
                );

        DwdOrderGoods current =
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        processTs
                );

        GoodsDelta delta =
                calculator.createCorrectionDelta(
                        order,
                        previous,
                        current
                );

        Assert.assertNotNull(delta);

        Assert.assertEquals(
                processTs,
                delta.getEventTs()
        );

        Assert.assertEquals(
                "2026-09-18 10:30:00",
                delta.getEventTime()
        );
    }

    /**
     * 创建测试订单。
     */
    private DwdOrderInfo createOrder(
            Long orderId,
            Integer isPay,
            String payTime) {

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(orderId);
        order.setShopId(100055L);
        order.setUserId(86L);

        order.setCreateTime(
                "2019-06-22 17:28:15"
        );

        order.setIsPay(isPay);
        order.setPayTime(payTime);

        return order;
    }

    /**
     * 创建测试商品明细。
     */
    private DwdOrderGoods createGoods(
            Long ogId,
            Long orderId,
            Long goodsId,
            Integer goodsNum,
            String goodsAmount,
            Long processTs) {

        DwdOrderGoods goods =
                new DwdOrderGoods();

        goods.setOgId(ogId);
        goods.setOrderId(orderId);
        goods.setGoodsId(goodsId);

        goods.setGoodsName(
                "测试商品-" + goodsId
        );

        goods.setGoodsNum(goodsNum);

        goods.setGoodsPrice(
                new BigDecimal("11.60")
        );

        goods.setGoodsAmount(
                new BigDecimal(goodsAmount)
        );

        goods.setProcessTs(processTs);

        return goods;
    }

    /**
     * BigDecimal比较。
     *
     * 使用compareTo避免：
     *
     * 23.20
     * 23.2
     *
     * 因scale不同导致equals失败。
     */
    private void assertDecimalEquals(
            String expected,
            BigDecimal actual) {

        Assert.assertNotNull(actual);

        Assert.assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }

    /**
     * 将Asia/Shanghai业务时间转为毫秒时间戳。
     */
    private long parseShanghaiTime(
            String time)
            throws Exception {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss"
                );

        formatter.setTimeZone(
                TimeZone.getTimeZone(
                        "Asia/Shanghai"
                )
        );

        return formatter.parse(
                time
        ).getTime();
    }
}