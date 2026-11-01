package org.example.process;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class GoodsBootstrapConfigTest {
    @After
    public void clearProperties() {
        System.clearProperty("goods.bootstrap.order.p0.end");
        System.clearProperty("goods.bootstrap.detail.p2.end");
    }

    @Test
    public void exposesSeparateOrderAndDetailBoundaries() {
        Assert.assertEquals(Long.valueOf(20015L), GoodsBootstrapConfig.orderEndOffsets().get(0));
        Assert.assertEquals(Long.valueOf(0L), GoodsBootstrapConfig.orderEndOffsets().get(1));
        Assert.assertEquals(Long.valueOf(0L), GoodsBootstrapConfig.orderEndOffsets().get(2));
        Assert.assertEquals(Long.valueOf(30638L), GoodsBootstrapConfig.detailEndOffsets().get(0));
        Assert.assertEquals(Long.valueOf(0L), GoodsBootstrapConfig.detailEndOffsets().get(1));
        Assert.assertEquals(Long.valueOf(0L), GoodsBootstrapConfig.detailEndOffsets().get(2));
    }

    @Test
    public void exposesSharedOrderInfoBoundaryForAllThemes() {
        Assert.assertEquals(Long.valueOf(20015L), TradeBootstrapConfig.historyEndOffsets().get(0));
        Assert.assertEquals(Long.valueOf(20015L), ShopBootstrapConfig.getEndOffsets().get(0));
        Assert.assertEquals(Long.valueOf(20015L), UserBootstrapConfig.getEndOffsets().get(0));
    }

    @Test
    public void supportsExplicitOverrides() {
        System.setProperty("goods.bootstrap.order.p0.end", "21");
        System.setProperty("goods.bootstrap.detail.p2.end", "34");
        Assert.assertEquals(Long.valueOf(21L), GoodsBootstrapConfig.orderEndOffsets().get(0));
        Assert.assertEquals(Long.valueOf(34L), GoodsBootstrapConfig.detailEndOffsets().get(2));
    }
}
