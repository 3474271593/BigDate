// 文件路径：src/main/java/org/example/process/DwdOrderGoodsDeserializer.java
package org.example.process;

import com.alibaba.fastjson.JSON;
import org.example.bean.DwdOrderGoods;

/**
 * dwd_order_goods Kafka JSON反序列化工具。
 */
public final class DwdOrderGoodsDeserializer {

    private DwdOrderGoodsDeserializer() {
    }

    /**
     * 将dwd_order_goods中的JSON转换为DwdOrderGoods。
     *
     * @param json Kafka JSON
     * @return DwdOrderGoods；解析失败返回null
     */
    public static DwdOrderGoods deserialize(String json) {

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return JSON.parseObject(
                    json,
                    DwdOrderGoods.class
            );
        } catch (Exception e) {

            System.err.println(
                    "DWS解析dwd_order_goods失败，json="
                            + json
                            + ", error="
                            + e.getMessage()
            );

            return null;
        }
    }
}