// 文件路径：src/test/java/org/example/process/GoodsJoinProcessFunctionTest.java
package org.example.process;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.co.KeyedCoProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedTwoInputStreamOperatorTestHarness;
import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsDelta;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GoodsJoinProcessFunction State 单元测试。
 *
 * 原有场景：
 * 1. order先到，goods后到
 * 2. goods先到，order后到
 * 3. 一个订单多个商品
 * 4. 未支付 -> 已支付
 * 5. 第一次看到订单时已经支付
 * 6. 重复已支付订单UPDATE不重复
 * 7. 多商品后支付
 * 8. 重复goods消息不重复
 *
 * Correction场景：
 * 9. 商品数量/金额增加
 * 10. 商品数量/金额减少
 * 11. 已支付后商品UPDATE
 * 12. 完全重复UPDATE
 * 13. 仅goodsName变化
 */
public class GoodsJoinProcessFunctionTest {

    private KeyedTwoInputStreamOperatorTestHarness<
            Long,
            DwdOrderInfo,
            DwdOrderGoods,
            GoodsDelta> harness;

    private GoodsRuntimeMode runtimeMode = GoodsRuntimeMode.REALTIME;

    @Before
    public void setUp() throws Exception {

        harness = createHarness(runtimeMode);
        harness.open();
    }

    private KeyedTwoInputStreamOperatorTestHarness<
            Long,
            DwdOrderInfo,
            DwdOrderGoods,
            GoodsDelta> createHarness(
            GoodsRuntimeMode mode)
            throws Exception {

        GoodsJoinProcessFunction function =
                new GoodsJoinProcessFunction(mode);

        KeyedCoProcessOperator<
                Long,
                DwdOrderInfo,
                DwdOrderGoods,
                GoodsDelta> operator =
                new KeyedCoProcessOperator<>(
                        function
                );

        return
                new KeyedTwoInputStreamOperatorTestHarness<
                        Long,
                        DwdOrderInfo,
                        DwdOrderGoods,
                        GoodsDelta>(
                        operator,
                        DwdOrderInfo::getOrderId,
                        DwdOrderGoods::getOrderId,
                        Types.LONG
                );
    }

    @Test
    public void testWarmupRestoresUnpaidStateWithoutOutput() throws Exception {
        reopenHarness(GoodsRuntimeMode.WARMUP);
        harness.processElement1(createOrder(56265L, 0, null), 0L);
        harness.processElement2(createGoods(121037L, 56265L, 100125L, 2, "23.20", 1000L), 0L);
        Assert.assertTrue(getOutputValues().isEmpty());
    }

    @Test
    public void testWarmupSnapshotProducesPayAfterRealtimeRestore() throws Exception {
        reopenHarness(GoodsRuntimeMode.WARMUP);
        harness.processElement1(createOrder(56265L, 0, null), 0L);
        harness.processElement2(createGoods(121037L, 56265L, 100125L, 2, "23.20", 1000L), 0L);

        org.apache.flink.runtime.checkpoint.OperatorSubtaskState snapshot = harness.snapshot(1L, 1L);
        harness.close();
        harness = null;

        runtimeMode = GoodsRuntimeMode.REALTIME;
        harness = createHarness(runtimeMode);
        harness.initializeState(snapshot);
        harness.open();
        harness.processElement1(createOrder(56265L, 1, "2019-06-22 17:29:30"), 0L);

        List<GoodsDelta> output = getOutputValues();
        Assert.assertEquals(1, output.size());
        Assert.assertEquals("GOODS_PAY", output.get(0).getEventType());
    }

    private void reopenHarness(GoodsRuntimeMode mode) throws Exception {
        if (harness != null) {
            harness.close();
        }
        harness = null;
        runtimeMode = mode;
        setUp();
    }

    @After
    public void tearDown() throws Exception {

        if (harness != null) {
            harness.close();
        }
    }

    /**
     * 1. order先到，goods后到。
     */
    @Test
    public void testOrderFirstGoodsLater()
            throws Exception {

        harness.processElement1(
                createOrder(
                        56265L,
                        0,
                        null
                ),
                0L
        );

        Assert.assertEquals(
                0,
                getOutputValues().size()
        );

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                1,
                output.size()
        );

        GoodsDelta delta =
                output.get(0);

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
    }

    /**
     * 2. goods先到，order后到。
     */
    @Test
    public void testGoodsFirstOrderLater()
            throws Exception {

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        Assert.assertEquals(
                0,
                getOutputValues().size()
        );

        harness.processElement1(
                createOrder(
                        56265L,
                        0,
                        null
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                1,
                output.size()
        );

        Assert.assertEquals(
                "GOODS_ORDER",
                output.get(0).getEventType()
        );
    }

    /**
     * 3. 一个订单包含多个商品。
     */
    @Test
    public void testMultipleGoodsInOneOrder()
            throws Exception {

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        121038L,
                        56265L,
                        100171L,
                        1,
                        "11.60",
                        1001L
                ),
                0L
        );

        harness.processElement1(
                createOrder(
                        56265L,
                        0,
                        null
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                2,
                output.size()
        );

        Assert.assertEquals(
                2,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );
    }

    /**
     * 4. 未支付 -> 已支付。
     *
     * 应产生：
     * GOODS_ORDER
     * GOODS_PAY
     */
    @Test
    public void testUnpaidToPaid()
            throws Exception {

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        harness.processElement1(
                createOrder(
                        56265L,
                        0,
                        null
                ),
                0L
        );

        harness.processElement1(
                createOrder(
                        56265L,
                        1,
                        "2019-06-22 17:29:30"
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                2,
                output.size()
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_PAY"
                )
        );

        GoodsDelta payDelta =
                findFirst(
                        output,
                        "GOODS_PAY"
                );

        Assert.assertNotNull(
                payDelta
        );

        Assert.assertEquals(
                1L,
                payDelta.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                2L,
                payDelta.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "23.20",
                payDelta.getPaidAmountDelta()
        );
    }

    /**
     * 5. 第一次看到订单时已经支付。
     *
     * 商品到达后应该同时产生：
     * GOODS_ORDER
     * GOODS_PAY
     */
    @Test
    public void testFirstSeenAlreadyPaid()
            throws Exception {

        harness.processElement1(
                createOrder(
                        56265L,
                        1,
                        "2019-06-22 17:29:30"
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                2,
                output.size()
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_PAY"
                )
        );
    }

    /**
     * 6. 已支付订单重复UPDATE不能重复统计。
     */
    @Test
    public void testDuplicatePaidUpdateDoesNotDuplicate()
            throws Exception {

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        DwdOrderInfo paidOrder =
                createOrder(
                        56265L,
                        1,
                        "2019-06-22 17:29:30"
                );

        harness.processElement1(
                paidOrder,
                0L
        );

        /*
         * 模拟相同订单再次UPDATE。
         */
        harness.processElement1(
                createOrder(
                        56265L,
                        1,
                        "2019-06-22 17:29:30"
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                2,
                output.size()
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_PAY"
                )
        );
    }

    /**
     * 7. 一个订单多个商品后完成支付。
     *
     * 两个GOODS_ORDER
     * 两个GOODS_PAY
     */
    @Test
    public void testMultipleGoodsThenPay()
            throws Exception {

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        121038L,
                        56265L,
                        100171L,
                        1,
                        "11.60",
                        1001L
                ),
                0L
        );

        harness.processElement1(
                createOrder(
                        56265L,
                        0,
                        null
                ),
                0L
        );

        harness.processElement1(
                createOrder(
                        56265L,
                        1,
                        "2019-06-22 17:29:30"
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                4,
                output.size()
        );

        Assert.assertEquals(
                2,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );

        Assert.assertEquals(
                2,
                countEventType(
                        output,
                        "GOODS_PAY"
                )
        );
    }

    /**
     * 8. 完全相同的商品消息重复进入。
     *
     * 不应该产生重复下单，
     * 也不应该产生Correction。
     */
    @Test
    public void testDuplicateGoodsDoesNotDuplicate()
            throws Exception {

        harness.processElement1(
                createOrder(
                        56265L,
                        0,
                        null
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        121037L,
                        56265L,
                        100125L,
                        2,
                        "23.20",
                        2000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                1,
                output.size()
        );

        Assert.assertEquals(
                "GOODS_ORDER",
                output.get(0).getEventType()
        );
    }

    /**
     * 9. 商品数量/金额增加。
     *
     * 真实业务模型：
     *
     * 1 -> 3
     * 11.60 -> 34.80
     *
     * Correction：
     *
     * goodsNumDelta = +2
     * amountDelta = +23.20
     */
    @Test
    public void testGoodsCorrectionIncrease()
            throws Exception {

        harness.processElement1(
                createOrder(
                        521L,
                        0,
                        null
                ),
                0L
        );

        /*
         * 初始商品：
         * 1件 / 11.60
         */
        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                ),
                0L
        );

        /*
         * UPDATE：
         * 3件 / 34.80
         */
        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        3,
                        "34.80",
                        2000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        /*
         * 一条初始下单 +
         * 一条Correction
         */
        Assert.assertEquals(
                2,
                output.size()
        );

        GoodsDelta correction =
                findFirst(
                        output,
                        "GOODS_UPDATE_CORRECTION"
                );

        Assert.assertNotNull(
                correction
        );

        Assert.assertEquals(
                0L,
                correction.getOrderCountDelta()
        );

        Assert.assertEquals(
                2L,
                correction.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "23.20",
                correction.getOrderAmountDelta()
        );

        Assert.assertEquals(
                0L,
                correction.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                0L,
                correction.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "0",
                correction.getPaidAmountDelta()
        );

        Assert.assertEquals(
                2000L,
                correction.getEventTs()
        );
    }

    /**
     * 10. 商品数量/金额减少。
     *
     * 3 -> 1
     * 34.80 -> 11.60
     *
     * Correction必须支持负Delta。
     */
    @Test
    public void testGoodsCorrectionDecrease()
            throws Exception {

        harness.processElement1(
                createOrder(
                        521L,
                        0,
                        null
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        3,
                        "34.80",
                        1000L
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        2000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                2,
                output.size()
        );

        GoodsDelta correction =
                findFirst(
                        output,
                        "GOODS_UPDATE_CORRECTION"
                );

        Assert.assertNotNull(
                correction
        );

        Assert.assertEquals(
                -2L,
                correction.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "-23.20",
                correction.getOrderAmountDelta()
        );

        Assert.assertEquals(
                0L,
                correction.getOrderCountDelta()
        );
    }

    /**
     * 11. 已支付以后商品发生UPDATE。
     *
     * 初始：
     * 1 / 11.60
     *
     * UPDATE：
     * 4 / 46.40
     *
     * 应同时修正：
     *
     * orderGoodsNum +3
     * orderAmount +34.80
     *
     * paidGoodsNum +3
     * paidAmount +34.80
     *
     * 但订单数和支付订单数都不能增加。
     */
    @Test
    public void testPaidGoodsCorrection()
            throws Exception {

        harness.processElement1(
                createOrder(
                        521L,
                        1,
                        "2019-06-22 17:29:30"
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        1,
                        "11.60",
                        1000L
                ),
                0L
        );

        /*
         * 此时已经产生：
         * GOODS_ORDER
         * GOODS_PAY
         */

        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        4,
                        "46.40",
                        2000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                3,
                output.size()
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_PAY"
                )
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_UPDATE_CORRECTION"
                )
        );

        GoodsDelta correction =
                findFirst(
                        output,
                        "GOODS_UPDATE_CORRECTION"
                );

        Assert.assertNotNull(
                correction
        );

        Assert.assertEquals(
                0L,
                correction.getOrderCountDelta()
        );

        Assert.assertEquals(
                3L,
                correction.getOrderGoodsNumDelta()
        );

        assertDecimalEquals(
                "34.80",
                correction.getOrderAmountDelta()
        );

        Assert.assertEquals(
                0L,
                correction.getPaidOrderCountDelta()
        );

        Assert.assertEquals(
                3L,
                correction.getPaidGoodsNumDelta()
        );

        assertDecimalEquals(
                "34.80",
                correction.getPaidAmountDelta()
        );
    }

    /**
     * 12. 完全相同的UPDATE。
     *
     * processTs可以变化，
     * 但是goodsNum和goodsAmount没变化。
     *
     * 不应该产生Correction。
     */
    @Test
    public void testSameMetricsUpdateDoesNotCreateCorrection()
            throws Exception {

        harness.processElement1(
                createOrder(
                        521L,
                        0,
                        null
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        1000L
                ),
                0L
        );

        harness.processElement2(
                createGoods(
                        120228L,
                        521L,
                        100125L,
                        2,
                        "23.20",
                        2000L
                ),
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                1,
                output.size()
        );

        Assert.assertEquals(
                0,
                countEventType(
                        output,
                        "GOODS_UPDATE_CORRECTION"
                )
        );
    }

    /**
     * 13. 只有goodsName变化。
     *
     * 例如：
     *
     * 第一次维度查询失败：
     * goodsName = ""
     *
     * 后续维度补全：
     * goodsName = "三只松鼠..."
     *
     * 只要数量和金额没变，
     * 就不能修改事实指标。
     */
    @Test
    public void testGoodsNameOnlyUpdateDoesNotCreateCorrection()
            throws Exception {

        harness.processElement1(
                createOrder(
                        521L,
                        0,
                        null
                ),
                0L
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

        harness.processElement2(
                previous,
                0L
        );

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
                "三只松鼠 无核白葡萄干"
        );

        harness.processElement2(
                current,
                0L
        );

        List<GoodsDelta> output =
                getOutputValues();

        Assert.assertEquals(
                1,
                output.size()
        );

        Assert.assertEquals(
                1,
                countEventType(
                        output,
                        "GOODS_ORDER"
                )
        );

        Assert.assertEquals(
                0,
                countEventType(
                        output,
                        "GOODS_UPDATE_CORRECTION"
                )
        );
    }

    /**
     * 创建订单测试数据。
     */
    private DwdOrderInfo createOrder(
            Long orderId,
            Integer isPay,
            String payTime) {

        DwdOrderInfo order =
                new DwdOrderInfo();

        order.setOrderId(
                orderId
        );

        order.setShopId(
                100055L
        );

        order.setUserId(
                86L
        );

        order.setCreateTime(
                "2019-06-22 17:28:15"
        );

        order.setIsPay(
                isPay
        );

        order.setPayTime(
                payTime
        );

        return order;
    }

    /**
     * 创建商品测试数据。
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

        goods.setOgId(
                ogId
        );

        goods.setOrderId(
                orderId
        );

        goods.setGoodsId(
                goodsId
        );

        goods.setGoodsName(
                "测试商品-" + goodsId
        );

        goods.setGoodsNum(
                goodsNum
        );

        goods.setGoodsPrice(
                new BigDecimal("11.60")
        );

        goods.setGoodsAmount(
                new BigDecimal(goodsAmount)
        );

        goods.setProcessTs(
                processTs
        );

        return goods;
    }

    /**
     * 获取Harness输出中的GoodsDelta。
     */
    private List<GoodsDelta> getOutputValues() {

        List<GoodsDelta> result =
                new ArrayList<>();

        ConcurrentLinkedQueue<Object> output =
                harness.getOutput();

        for (Object item : output) {

            if (!(item instanceof StreamRecord)) {
                continue;
            }

            StreamRecord<?> record =
                    (StreamRecord<?>) item;

            Object value =
                    record.getValue();

            if (value instanceof GoodsDelta) {

                result.add(
                        (GoodsDelta) value
                );
            }
        }

        return result;
    }

    /**
     * 统计某种事件类型出现次数。
     */
    private int countEventType(
            List<GoodsDelta> output,
            String eventType) {

        int count = 0;

        for (GoodsDelta delta : output) {

            if (eventType.equals(
                    delta.getEventType()
            )) {

                count++;
            }
        }

        return count;
    }

    /**
     * 查找某种事件类型的第一条记录。
     */
    private GoodsDelta findFirst(
            List<GoodsDelta> output,
            String eventType) {

        for (GoodsDelta delta : output) {

            if (eventType.equals(
                    delta.getEventType()
            )) {

                return delta;
            }
        }

        return null;
    }

    /**
     * BigDecimal比较。
     */
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
