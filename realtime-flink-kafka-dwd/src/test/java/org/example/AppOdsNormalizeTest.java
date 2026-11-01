package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * ODS 数据结构统一测试。
 *
 * 需要同时兼容：
 *
 * 1. 历史 bootstrap：
 *    data = JSONObject
 *
 * 2. Canal CDC：
 *    data = JSONArray
 *    old  = JSONArray
 */
public class AppOdsNormalizeTest {

    /**
     * 历史 bootstrap 数据本身已经是单条 JSONObject，
     * 统一后仍然应该只有一条数据。
     */
    @Test
    public void shouldNormalizeBootstrapData() {
        String json = "{\"table\":\"orders\",\"type\":\"bootstrap\",\"data\":{\"orderId\":\"4\",\"orderStatus\":\"0\"}}";

        JSONObject odsJson = JSON.parseObject(json);
        List<JSONObject> result = App.normalizeOdsEvent(odsJson);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("orders", result.get(0).getString("table"));
        Assert.assertEquals("bootstrap", result.get(0).getString("type"));
        Assert.assertEquals("4", result.get(0).getJSONObject("data").getString("orderId"));
        Assert.assertEquals("0", result.get(0).getJSONObject("data").getString("orderStatus"));
    }

    /**
     * Canal UPDATE 的 data 是数组。
     *
     * 统一以后：
     * data 必须变成单条 JSONObject，
     * old 也必须变成对应的 JSONObject。
     */
    @Test
    public void shouldNormalizeCanalUpdateData() {
        String json = "{\"database\":\"shop\",\"table\":\"orders\",\"type\":\"UPDATE\",\"data\":[{\"orderId\":\"4\",\"orderStatus\":\"0\"}],\"old\":[{\"orderStatus\":\"1\"}]}";

        JSONObject odsJson = JSON.parseObject(json);
        List<JSONObject> result = App.normalizeOdsEvent(odsJson);

        Assert.assertEquals(1, result.size());

        JSONObject normalized = result.get(0);

        Assert.assertEquals("shop", normalized.getString("database"));
        Assert.assertEquals("orders", normalized.getString("table"));
        Assert.assertEquals("UPDATE", normalized.getString("type"));
        Assert.assertEquals("4", normalized.getJSONObject("data").getString("orderId"));
        Assert.assertEquals("0", normalized.getJSONObject("data").getString("orderStatus"));
        Assert.assertEquals("1", normalized.getJSONObject("old").getString("orderStatus"));
    }

    /**
     * Canal 一条消息可能包含多行。
     *
     * 不能只取 data[0]，
     * 否则会造成实时数据丢失。
     */
    @Test
    public void shouldSplitMultipleCanalRows() {
        String json = "{\"database\":\"shop\",\"table\":\"orders\",\"type\":\"UPDATE\",\"data\":[{\"orderId\":\"4\"},{\"orderId\":\"6\"}],\"old\":[{\"orderStatus\":\"1\"},{\"orderStatus\":\"0\"}]}";

        JSONObject odsJson = JSON.parseObject(json);
        List<JSONObject> result = App.normalizeOdsEvent(odsJson);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("4", result.get(0).getJSONObject("data").getString("orderId"));
        Assert.assertEquals("6", result.get(1).getJSONObject("data").getString("orderId"));
        Assert.assertEquals("1", result.get(0).getJSONObject("old").getString("orderStatus"));
        Assert.assertEquals("0", result.get(1).getJSONObject("old").getString("orderStatus"));
    }
}