package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.example.bean.DwdOrderGoods;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * DwdOrderGoods Kafka序列化测试。
 *
 * 第30条测试：
 * 验证订单商品明细能够正确序列化成Kafka JSON。
 */
public class DwdOrderGoodsKafkaSerializerTest {

    @Test
    public void shouldSerializeDwdOrderGoods() {
        DwdOrderGoods orderGoods = new DwdOrderGoods();

        orderGoods.setOgId(2L);
        orderGoods.setOrderId(521L);
        orderGoods.setGoodsId(16L);
        orderGoods.setGoodsNum(5L);
        orderGoods.setGoodsPrice(new BigDecimal("20.90"));
        orderGoods.setGoodsAmount(new BigDecimal("104.50"));
        orderGoods.setGoodsSpecId(11);
        orderGoods.setGoodsSpecNames("dddd");
        orderGoods.setGoodsName("花果山 农家新鲜大蒜 2500g装 蒜头");
        orderGoods.setGoodsImg("b.img");
        orderGoods.setExtraJson(null);
        orderGoods.setGoodsType(0);
        orderGoods.setCommissionRate(new BigDecimal("0.01"));
        orderGoods.setGoodsCode(null);
        orderGoods.setPromotionJson(null);
        orderGoods.setOpType("UPDATE");
        orderGoods.setProcessTs(1789118401892L);

        String jsonString = DwdKafkaSerializer.serializeOrderGoods(orderGoods);

        Assert.assertNotNull(jsonString);

        JSONObject json = JSONObject.parseObject(jsonString);

        Assert.assertEquals(Long.valueOf(2L), json.getLong("ogId"));
        Assert.assertEquals(Long.valueOf(521L), json.getLong("orderId"));
        Assert.assertEquals(Long.valueOf(16L), json.getLong("goodsId"));
        Assert.assertEquals(Long.valueOf(5L), json.getLong("goodsNum"));

        Assert.assertEquals(
                0,
                new BigDecimal("20.90").compareTo(json.getBigDecimal("goodsPrice"))
        );

        Assert.assertEquals(
                0,
                new BigDecimal("104.50").compareTo(json.getBigDecimal("goodsAmount"))
        );

        Assert.assertEquals(
                "花果山 农家新鲜大蒜 2500g装 蒜头",
                json.getString("goodsName")
        );

        Assert.assertEquals(
                "UPDATE",
                json.getString("opType")
        );

        Assert.assertEquals(
                Long.valueOf(1789118401892L),
                json.getLong("processTs")
        );
    }
}