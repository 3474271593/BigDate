package org.example.process;

import com.alibaba.fastjson.JSON;
import org.example.bean.ShopSummary;

/**
 * ShopSummary JSON 序列化器。
 *
 * DWS 店铺主题输出链路：
 *
 * ShopSummary
 *      ↓
 * JSON
 *      ↓
 * Kafka
 *      ↓
 * dws_shop_summary
 */
public final class ShopSummarySerializer {

    private ShopSummarySerializer() {
        // 工具类禁止实例化
    }

    /**
     * ShopSummary -> JSON String
     *
     * @param summary 店铺主题一分钟汇总结果
     * @return JSON 字符串；输入为空时返回 null
     */
    public static String serialize(
            ShopSummary summary) {

        if (summary == null) {
            return null;
        }

        try {

            return JSON.toJSONString(
                    summary
            );

        } catch (Exception e) {

            System.err.println(
                    "ShopSummary序列化失败："
                            + e.getMessage()
            );

            return null;
        }
    }
}