package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderGoods;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * order_goods DWD分流测试。
 *
 * 第28条：
 * 正常商品明细进入DWD主流。
 *
 * 第29条：
 * 异常商品明细进入DirtyData侧输出。
 */
public class OrderGoodsDwdProcessFunctionTest {

    /**
     * 第28条测试：
     * 正常order_goods数据应进入DWD主流。
     */
    @Test
    public void shouldOutputValidOrderGoodsToMainStream() throws Exception {
        OrderGoodsDwdProcessFunction function = new OrderGoodsDwdProcessFunction();

        OneInputStreamOperatorTestHarness<JSONObject, DwdOrderGoods> harness =
                ProcessFunctionTestHarnesses.forProcessFunction(function);

        harness.open();

        JSONObject odsJson = buildValidOrderGoods();

        harness.processElement(odsJson, 1000L);

        ConcurrentLinkedQueue<Object> output = harness.getOutput();

        Assert.assertNotNull(output);
        Assert.assertFalse(output.isEmpty());

        DwdOrderGoods result = (DwdOrderGoods)
                ((org.apache.flink.streaming.runtime.streamrecord.StreamRecord<?>) output.poll()).getValue();

        Assert.assertEquals(Long.valueOf(1L), result.getOgId());
        Assert.assertEquals(Long.valueOf(520L), result.getOrderId());
        Assert.assertEquals(Long.valueOf(10L), result.getGoodsId());

        harness.close();
    }

    /**
     * 第29条测试：
     * 非法order_goods数据不能进入主流，
     * 应进入DirtyData侧输出。
     */
    @Test
    public void shouldOutputInvalidOrderGoodsToDirtySideOutput() throws Exception {
        OrderGoodsDwdProcessFunction function = new OrderGoodsDwdProcessFunction();

        OneInputStreamOperatorTestHarness<JSONObject, DwdOrderGoods> harness =
                ProcessFunctionTestHarnesses.forProcessFunction(function);

        harness.open();

        JSONObject odsJson = buildValidOrderGoods();

        odsJson.getJSONObject("data").put("orderId", "0");

        harness.processElement(odsJson, 1000L);

        ConcurrentLinkedQueue<Object> mainOutput = harness.getOutput();

        Assert.assertTrue(
                mainOutput == null || mainOutput.isEmpty()
        );

        ConcurrentLinkedQueue<org.apache.flink.streaming.runtime.streamrecord.StreamRecord<DirtyData>> dirtyOutput =
                harness.getSideOutput(OrderGoodsDwdProcessFunction.DIRTY_TAG);

        Assert.assertNotNull(dirtyOutput);
        Assert.assertFalse(dirtyOutput.isEmpty());

        DirtyData dirtyData = dirtyOutput.poll().getValue();

        Assert.assertEquals("order_goods", dirtyData.getTableName());
        Assert.assertEquals("bootstrap", dirtyData.getOpType());
        Assert.assertNotNull(dirtyData.getErrorType());
        Assert.assertNotNull(dirtyData.getErrorMessage());
        Assert.assertNotNull(dirtyData.getRawData());

        harness.close();
    }

    /**
     * 构造合法order_goods数据。
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