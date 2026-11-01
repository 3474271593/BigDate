package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * order_goods 历史回灌过滤规则测试。
 *
 * 回灌程序只能处理：
 *
 * table = order_goods
 * type  = bootstrap
 *
 * 不能处理实时 INSERT / UPDATE / DELETE，
 * 也不能处理其他业务表。
 */
public class OrderGoodsBackfillFilterTest {

    /**
     * 第31条测试：
     *
     * order_goods + bootstrap
     *
     * 应该进入历史回灌流程。
     */
    @Test
    public void shouldAcceptOrderGoodsBootstrap() {

        JSONObject json = new JSONObject();

        json.put("table", "order_goods");
        json.put("type", "bootstrap");

        boolean result = OrderGoodsBackfillFilter.shouldBackfill(json);

        Assert.assertTrue(result);
    }

    /**
     * 第32条测试：
     *
     * order_goods + UPDATE
     *
     * 属于实时CDC数据，
     * 历史回灌程序不能再次处理。
     */
    @Test
    public void shouldRejectOrderGoodsUpdate() {

        JSONObject json = new JSONObject();

        json.put("table", "order_goods");
        json.put("type", "UPDATE");

        boolean result = OrderGoodsBackfillFilter.shouldBackfill(json);

        Assert.assertFalse(result);
    }

    /**
     * 第33条测试：
     *
     * orders + bootstrap
     *
     * 虽然也是历史数据，
     * 但是本次只回灌 order_goods，
     * 所以必须过滤掉。
     */
    @Test
    public void shouldRejectOrdersBootstrap() {

        JSONObject json = new JSONObject();

        json.put("table", "orders");
        json.put("type", "bootstrap");

        boolean result = OrderGoodsBackfillFilter.shouldBackfill(json);

        Assert.assertFalse(result);
    }
}