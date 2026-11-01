// 文件路径：src/test/java/org/example/process/DwdOrderInfoDeserializerTest.java
package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * DwdOrderInfoDeserializer单元测试。
 */
public class DwdOrderInfoDeserializerTest {

    /**
     * 验证正常DWD订单能够完整反序列化。
     */
    @Test
    public void shouldDeserializeValidOrder() {

        String json =
                "{"
                        + "\"orderId\":4,"
                        + "\"shopId\":100055,"
                        + "\"userId\":86,"
                        + "\"realTotalMoney\":511.93,"
                        + "\"createTime\":\"2019-06-22 17:28:15\","
                        + "\"isPay\":0,"
                        + "\"opType\":\"bootstrap\","
                        + "\"processTs\":1789115913485"
                        + "}";

        DwdOrderInfo order =
                DwdOrderInfoDeserializer.deserialize(json);

        Assert.assertNotNull(order);

        Assert.assertEquals(
                Long.valueOf(4L),
                order.getOrderId()
        );

        Assert.assertEquals(
                Long.valueOf(100055L),
                order.getShopId()
        );

        Assert.assertEquals(
                Long.valueOf(86L),
                order.getUserId()
        );

        Assert.assertEquals(
                0,
                new BigDecimal("511.93").compareTo(
                        order.getRealTotalMoney()
                )
        );

        Assert.assertEquals(
                "2019-06-22 17:28:15",
                order.getCreateTime()
        );

        Assert.assertEquals(
                Integer.valueOf(0),
                order.getIsPay()
        );

        Assert.assertNull(
                order.getPayTime()
        );

        Assert.assertEquals(
                "bootstrap",
                order.getOpType()
        );

        Assert.assertEquals(
                Long.valueOf(1789115913485L),
                order.getProcessTs()
        );
    }

    /**
     * 验证已支付订单能够读取payTime。
     */
    @Test
    public void shouldDeserializePaidOrderWithPayTime() {

        String json =
                "{"
                        + "\"orderId\":1001,"
                        + "\"shopId\":100055,"
                        + "\"userId\":100,"
                        + "\"realTotalMoney\":100.50,"
                        + "\"createTime\":\"2019-06-22 17:28:15\","
                        + "\"isPay\":1,"
                        + "\"payTime\":\"2019-06-22 17:30:20\","
                        + "\"opType\":\"UPDATE\","
                        + "\"processTs\":1789115913485"
                        + "}";

        DwdOrderInfo order =
                DwdOrderInfoDeserializer.deserialize(json);

        Assert.assertNotNull(order);

        Assert.assertEquals(
                "2019-06-22 17:28:15",
                order.getCreateTime()
        );

        Assert.assertEquals(
                Integer.valueOf(1),
                order.getIsPay()
        );

        Assert.assertEquals(
                "2019-06-22 17:30:20",
                order.getPayTime()
        );
    }

    /**
     * 非法JSON应该返回null，
     * 避免单条脏数据直接导致DWS Job失败。
     */
    @Test
    public void shouldReturnNullForInvalidJson() {

        String json =
                "{invalid-json}";

        DwdOrderInfo order =
                DwdOrderInfoDeserializer.deserialize(json);

        Assert.assertNull(order);
    }
}