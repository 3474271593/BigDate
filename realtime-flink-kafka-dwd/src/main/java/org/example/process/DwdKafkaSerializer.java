package org.example.process;

import com.alibaba.fastjson.JSON;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderInfo;
import org.example.bean.DwdOrderGoods;

/**
 * DWD Kafka JSON 序列化工具。
 *
 * 作用：
 *
 * 1. DwdOrderInfo -> JSON String
 * 2. DirtyData    -> JSON String
 *
 * 后续 Flink Kafka Sink 会直接把这里生成的 JSON
 * 写入对应的 Kafka Topic。
 */
public class DwdKafkaSerializer {

    /**
     * 工具类不允许创建实例。
     */
    private DwdKafkaSerializer() {
    }

    /**
     * 将 DWD 订单对象转换为 JSON。
     *
     * 最终用于写入：
     *
     * dwd_order_info
     *
     * @param order DWD订单
     * @return JSON字符串
     */
    public static String serializeOrder(DwdOrderInfo order) {

        /*
         * 防止错误调用。
         *
         * 当前正常业务情况下，
         * OrderDwdProcessFunction 不会输出 null。
         *
         * 这里仍然增加保护，
         * 防止后续代码误传 null。
         */
        if (order == null) {
            throw new IllegalArgumentException(
                    "DwdOrderInfo不能为空"
            );
        }

        return JSON.toJSONString(order);
    }

    /**
     * 将脏数据对象转换为 JSON。
     *
     * 最终用于写入：
     *
     * dwd_dirty_data
     *
     * @param dirtyData 脏数据
     * @return JSON字符串
     */
    public static String serializeDirtyData(DirtyData dirtyData) {

        /*
         * DirtyData 也不能为 null。
         */
        if (dirtyData == null) {
            throw new IllegalArgumentException(
                    "DirtyData不能为空"
            );
        }

        return JSON.toJSONString(dirtyData);
    }
    /**
     * 将订单商品明细DWD对象序列化为JSON。
     *
     * 后续写入Kafka Topic：
     * dwd_order_goods
     *
     * @param orderGoods 订单商品明细DWD对象
     * @return JSON字符串
     */
    public static String serializeOrderGoods(DwdOrderGoods orderGoods) {
        if (orderGoods == null) {
            return null;
        }

        return JSON.toJSONString(orderGoods);
    }
}