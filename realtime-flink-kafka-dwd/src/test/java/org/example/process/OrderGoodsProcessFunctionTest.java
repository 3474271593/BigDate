package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.example.bean.DwdOrderGoods;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * order_goods DWD 清洗规则测试。
 *
 * 测试编号：
 *
 * 20. ogId 主键校验
 * 21. orderId 关联订单校验
 * 22. goodsId 商品主键校验
 * 23. goodsNum 商品数量校验
 * 24. goodsPrice 商品价格校验
 * 25. goodsName 缺失时保留交易事实
 * 26. goodsAmount 商品金额计算
 * 27. 相同 orderId + goodsId 不应被误判为重复数据
 *
 * 当前测试基于真实 shop.order_goods 表结构设计。
 */
public class OrderGoodsProcessFunctionTest {

    /**
     * 第20条测试：
     * 校验订单商品明细主键 ogId。
     *
     * ogId 是 order_goods 的真实主键。
     * ogId <= 0 时属于无效事实数据。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidOgId() {
        JSONObject odsJson = buildValidOrderGoods();

        odsJson.getJSONObject("data").put("ogId", "0");

        OrderGoodsProcessFunction.cleanOrderGoods(odsJson);
    }

    /**
     * 第21条测试：
     * 校验 orderId。
     *
     * order_goods 必须能够关联到具体订单，
     * 因此 orderId <= 0 时进入脏数据。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidOrderId() {
        JSONObject odsJson = buildValidOrderGoods();

        odsJson.getJSONObject("data").put("orderId", "0");

        OrderGoodsProcessFunction.cleanOrderGoods(odsJson);
    }

    /**
     * 第22条测试：
     * 校验 goodsId。
     *
     * 商品明细必须能够关联有效商品。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidGoodsId() {
        JSONObject odsJson = buildValidOrderGoods();

        odsJson.getJSONObject("data").put("goodsId", "0");

        OrderGoodsProcessFunction.cleanOrderGoods(odsJson);
    }

    /**
     * 第23条测试：
     * 校验商品购买数量。
     *
     * goodsNum 必须大于 0。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidGoodsNum() {
        JSONObject odsJson = buildValidOrderGoods();

        odsJson.getJSONObject("data").put("goodsNum", "0");

        OrderGoodsProcessFunction.cleanOrderGoods(odsJson);
    }

    /**
     * 第24条测试：
     * 校验商品价格。
     *
     * 商品价格不能小于 0。
     *
     * 注意：
     * goodsPrice = 0 暂时允许，
     * 因为不能排除赠品、活动商品等合法业务情况。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeGoodsPrice() {
        JSONObject odsJson = buildValidOrderGoods();

        odsJson.getJSONObject("data").put("goodsPrice", "-20.90");

        OrderGoodsProcessFunction.cleanOrderGoods(odsJson);
    }

    /**
     * 第25条测试：
     * goodsName 缺失时不应丢弃交易事实。
     *
     * goodsName 属于商品描述字段，不是判断交易事实是否成立的核心字段。
     *
     * 只要：
     * ogId、orderId、goodsId、goodsNum、goodsPrice
     * 等核心事实字段合法，就应该保留该 order_goods 数据。
     *
     * goodsName 为空时，后续通过：
     *
     * goodsId -> Redis DIM -> dim:goods:<goodsId> -> goodsName
     *
     * 尝试补全商品名称。
     *
     * 因此这里不能因为 goodsName 为空而抛出异常。
     */
    @Test
    public void shouldKeepOrderGoodsWhenGoodsNameIsBlank() {
        JSONObject odsJson = buildValidOrderGoods();

        JSONObject data = odsJson.getJSONObject("data");

        data.put("ogId", "150790");
        data.put("orderId", "65400535");
        data.put("goodsId", "100125");
        data.put("goodsNum", "1");
        data.put("goodsPrice", "11.60");
        data.put("goodsName", "");

        DwdOrderGoods result = OrderGoodsProcessFunction.cleanOrderGoods(odsJson);

        Assert.assertNotNull(result);
        Assert.assertEquals(Long.valueOf(150790L), result.getOgId());
        Assert.assertEquals(Long.valueOf(65400535L), result.getOrderId());
        Assert.assertEquals(Long.valueOf(100125L), result.getGoodsId());
        Assert.assertEquals(Long.valueOf(1L), result.getGoodsNum());
        Assert.assertEquals(0, new BigDecimal("11.60").compareTo(result.getGoodsPrice()));
        Assert.assertEquals(0, new BigDecimal("11.60").compareTo(result.getGoodsAmount()));
        Assert.assertEquals("", result.getGoodsName());
    }

    /**
     * 第26条测试：
     * 校验商品金额计算。
     *
     * goodsAmount = goodsNum * goodsPrice
     *
     * 当前测试使用真实样例：
     *
     * goodsNum = 5
     * goodsPrice = 20.90
     *
     * 期望：
     *
     * goodsAmount = 104.50
     */
    @Test
    public void shouldCalculateGoodsAmount() {
        JSONObject odsJson = buildValidOrderGoods();

        JSONObject data = odsJson.getJSONObject("data");
        data.put("goodsNum", "5");
        data.put("goodsPrice", "20.90");

        DwdOrderGoods result = OrderGoodsProcessFunction.cleanOrderGoods(odsJson);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getGoodsAmount());

        Assert.assertEquals(
                0,
                new BigDecimal("104.50").compareTo(result.getGoodsAmount())
        );
    }

    /**
     * 第27条测试：
     * 验证明细事实粒度。
     *
     * 真实源数据中存在：
     *
     * ogId=2 orderId=521 goodsId=16
     * ogId=3 orderId=521 goodsId=16
     * ogId=4 orderId=521 goodsId=16
     *
     * 因此不能使用：
     *
     * orderId + goodsId
     *
     * 判断两条数据重复。
     *
     * 不同 ogId 就应该保留成不同商品明细事实。
     */
    @Test
    public void shouldKeepDifferentOgIdWithSameOrderAndGoods() {
        JSONObject firstJson = buildValidOrderGoods();
        JSONObject secondJson = buildValidOrderGoods();

        JSONObject firstData = firstJson.getJSONObject("data");
        JSONObject secondData = secondJson.getJSONObject("data");

        firstData.put("ogId", "2");
        firstData.put("orderId", "521");
        firstData.put("goodsId", "16");

        secondData.put("ogId", "3");
        secondData.put("orderId", "521");
        secondData.put("goodsId", "16");

        DwdOrderGoods first = OrderGoodsProcessFunction.cleanOrderGoods(firstJson);
        DwdOrderGoods second = OrderGoodsProcessFunction.cleanOrderGoods(secondJson);

        Assert.assertNotNull(first);
        Assert.assertNotNull(second);

        Assert.assertEquals(Long.valueOf(2L), first.getOgId());
        Assert.assertEquals(Long.valueOf(3L), second.getOgId());

        Assert.assertEquals(first.getOrderId(), second.getOrderId());
        Assert.assertEquals(first.getGoodsId(), second.getGoodsId());

        Assert.assertNotEquals(first.getOgId(), second.getOgId());
    }

    /**
     * 构造一条合法的 order_goods ODS 数据。
     *
     * 结构保持和经过 ODS Normalize 后的数据一致：
     *
     * {
     *   "table": "order_goods",
     *   "type": "bootstrap",
     *   "data": {
     *       ...
     *   }
     * }
     *
     * 使用真实 order_goods 样例数据作为基础。
     */
    private JSONObject buildValidOrderGoods() {
        JSONObject data = new JSONObject();

        data.put("ogId", "1");
        data.put("orderId", "520");
        data.put("goodsId", "10");
        data.put("goodsNum", "20");
        data.put("goodsPrice", "11.60");
        data.put("goodsSpecId", "33");
        data.put("goodsSpecNames", "aaa");
        data.put("goodsName", "红颜奶油草莓 约重500g/15-20颗 新鲜水果");
        data.put("goodsImg", "a.img");
        data.put("extraJson", null);
        data.put("goodsType", "0");
        data.put("commissionRate", "0.02");
        data.put("goodsCode", null);
        data.put("promotionJson", null);

        JSONObject odsJson = new JSONObject();

        odsJson.put("table", "order_goods");
        odsJson.put("type", "bootstrap");
        odsJson.put("data", data);

        return odsJson;
    }
}