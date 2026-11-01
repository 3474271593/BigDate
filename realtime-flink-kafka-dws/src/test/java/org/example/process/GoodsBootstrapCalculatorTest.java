package org.example.process;

import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsSummary;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

public class GoodsBootstrapCalculatorTest {
    @Test
    public void buildsOrderAndPaymentBaselineFromFinalSnapshots() {
        DwdOrderInfo unpaid = order(1L, 0, null);
        DwdOrderInfo paid = order(1L, 1, "2019-09-05 19:57:10");
        DwdOrderGoods goods = goods(10L, 1L, 100161L, 2, "93.00");

        GoodsBootstrapResult result = GoodsBootstrapCalculator.calculate(
                Arrays.asList(unpaid, paid),
                Collections.singletonList(goods)
        );

        Assert.assertEquals(2L, result.getOrderReadCount());
        Assert.assertEquals(1L, result.getDetailReadCount());
        Assert.assertEquals(2, result.getSummaries().size());

        GoodsSummary orderSummary = result.getSummaries().get(0);
        Assert.assertEquals("2019-09-05 19:56:00", orderSummary.getWindowStart());
        Assert.assertEquals(1L, orderSummary.getOrderCount());
        Assert.assertEquals(2L, orderSummary.getOrderGoodsNum());
        Assert.assertEquals(0L, orderSummary.getPaidOrderCount());
        Assert.assertEquals(0, new BigDecimal("93.00").compareTo(orderSummary.getOrderAmount()));

        GoodsSummary paySummary = result.getSummaries().get(1);
        Assert.assertEquals("2019-09-05 19:57:00", paySummary.getWindowStart());
        Assert.assertEquals(1L, paySummary.getPaidOrderCount());
        Assert.assertEquals(2L, paySummary.getPaidGoodsNum());
        Assert.assertEquals(0, new BigDecimal("93.00").compareTo(paySummary.getPaidAmount()));
    }

    @Test
    public void usesLatestDetailSnapshotWithoutCorrectionReplay() {
        DwdOrderInfo order = order(2L, 0, null);
        DwdOrderGoods first = goods(20L, 2L, 100162L, 1, "10.00");
        DwdOrderGoods latest = goods(20L, 2L, 100162L, 3, "30.00");

        GoodsBootstrapResult result = GoodsBootstrapCalculator.calculate(
                Collections.singletonList(order),
                Arrays.asList(first, latest)
        );

        Assert.assertEquals(1, result.getSummaries().size());
        GoodsSummary summary = result.getSummaries().get(0);
        Assert.assertEquals(3L, summary.getOrderGoodsNum());
        Assert.assertEquals(0, new BigDecimal("30.00").compareTo(summary.getOrderAmount()));
    }

    private DwdOrderInfo order(long id, int isPay, String payTime) {
        DwdOrderInfo value = new DwdOrderInfo();
        value.setOrderId(id);
        value.setShopId(100052L);
        value.setUserId(6L);
        value.setCreateTime("2019-09-05 19:56:20");
        value.setIsPay(isPay);
        value.setPayTime(payTime);
        return value;
    }

    private DwdOrderGoods goods(long ogId, long orderId, long goodsId,
                                int number, String amount) {
        DwdOrderGoods value = new DwdOrderGoods();
        value.setOgId(ogId);
        value.setOrderId(orderId);
        value.setGoodsId(goodsId);
        value.setGoodsName("goods-" + goodsId);
        value.setGoodsNum(number);
        value.setGoodsAmount(new BigDecimal(amount));
        return value;
    }
}
