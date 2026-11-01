// 文件路径：src/main/java/org/example/process/DwdOrderInfoDeserializer.java
package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.bean.DwdOrderInfo;

import java.math.BigDecimal;

/**
 * dwd_order_info JSON反序列化工具。
 */
public class DwdOrderInfoDeserializer {

    private DwdOrderInfoDeserializer() {
    }

    /**
     * 将Kafka中的DWD订单JSON转换为DwdOrderInfo。
     *
     * @param json Kafka JSON字符串
     * @return DwdOrderInfo，解析失败返回null
     */
    public static DwdOrderInfo deserialize(String json) {

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject jsonObject = JSON.parseObject(json);

            DwdOrderInfo order = new DwdOrderInfo();

            order.setOrderId(
                    jsonObject.getLong("orderId")
            );

            order.setShopId(
                    jsonObject.getLong("shopId")
            );

            order.setUserId(
                    jsonObject.getLong("userId")
            );

            order.setRealTotalMoney(
                    getBigDecimal(
                            jsonObject,
                            "realTotalMoney"
                    )
            );

            /*
             * 新增：
             * 获取DWD中的真实下单业务时间。
             *
             * 后续ORDER_CREATE事件使用createTime作为Event Time。
             */
            order.setCreateTime(
                    jsonObject.getString("createTime")
            );

            /*
             * 支付状态以isPay为准。
             */
            order.setIsPay(
                    jsonObject.getInteger("isPay")
            );

            /*
             * payTime由DWD层保证业务语义。
             *
             * 未支付订单可能不存在该字段，
             * getString()此时返回null。
             */
            order.setPayTime(
                    jsonObject.getString("payTime")
            );

            order.setOpType(
                    jsonObject.getString("opType")
            );

            order.setProcessTs(
                    jsonObject.getLong("processTs")
            );

            return order;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全读取BigDecimal字段。
     */
    private static BigDecimal getBigDecimal(
            JSONObject jsonObject,
            String fieldName
    ) {

        Object value = jsonObject.get(fieldName);

        if (value == null) {
            return null;
        }

        return new BigDecimal(
                value.toString()
        );
    }
}