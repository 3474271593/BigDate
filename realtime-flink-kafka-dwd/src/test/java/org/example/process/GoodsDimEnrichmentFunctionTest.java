package org.example.process;

import org.example.bean.DwdOrderGoods;
import org.junit.Assert;
import org.junit.Test;

/**
 * 商品DIM维度补全规则测试。
 *
 * 测试目标：
 *
 * 1. order_goods自身有goodsName时，不覆盖原始订单商品快照。
 * 2. order_goods的goodsName为空时，使用DIM商品名称补全。
 * 3. DIM也没有商品名称时，仍然保留交易事实。
 */
public class GoodsDimEnrichmentFunctionTest {

    /**
     * 第34条测试：
     * 原始order_goods已经存在goodsName时，
     * 应优先保留订单商品快照名称。
     *
     * 不能使用当前DIM名称覆盖历史订单快照。
     */
    @Test
    public void shouldKeepOriginalGoodsNameWhenPresent() {
        DwdOrderGoods orderGoods = buildOrderGoods("订单快照商品名称");

        GoodsDimEnrichmentFunction.enrichGoodsName(
                orderGoods,
                "当前DIM商品名称"
        );

        Assert.assertEquals(
                "订单快照商品名称",
                orderGoods.getGoodsName()
        );
    }

    /**
     * 第35条测试：
     * order_goods的goodsName为空时，
     * 使用Redis DIM查询得到的商品名称进行补全。
     */
    @Test
    public void shouldEnrichGoodsNameWhenMissing() {
        DwdOrderGoods orderGoods = buildOrderGoods("");

        GoodsDimEnrichmentFunction.enrichGoodsName(
                orderGoods,
                "三只松鼠 无核白葡萄干280gx2袋"
        );

        Assert.assertEquals(
                "三只松鼠 无核白葡萄干280gx2袋",
                orderGoods.getGoodsName()
        );
    }

    /**
     * 第36条测试：
     * order_goods没有goodsName，
     * DIM也没有找到goodsName时，
     * 不能因为维度缺失而丢弃交易事实。
     */
    @Test
    public void shouldKeepOrderGoodsWhenDimGoodsNameMissing() {
        DwdOrderGoods orderGoods = buildOrderGoods("");

        GoodsDimEnrichmentFunction.enrichGoodsName(
                orderGoods,
                null
        );

        Assert.assertNotNull(orderGoods);
        Assert.assertEquals(Long.valueOf(100125L), orderGoods.getGoodsId());
        Assert.assertEquals("", orderGoods.getGoodsName());
    }

    /**
     * 构造测试用DWD订单商品事实。
     */
    private DwdOrderGoods buildOrderGoods(String goodsName) {
        DwdOrderGoods orderGoods = new DwdOrderGoods();

        orderGoods.setOgId(150790L);
        orderGoods.setOrderId(65400535L);
        orderGoods.setGoodsId(100125L);
        orderGoods.setGoodsNum(1L);
        orderGoods.setGoodsName(goodsName);

        return orderGoods;
    }
}