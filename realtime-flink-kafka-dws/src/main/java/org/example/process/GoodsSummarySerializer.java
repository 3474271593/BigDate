package org.example.process;

import com.alibaba.fastjson.JSON;
import org.example.bean.GoodsSummary;

/**
 * GoodsSummary JSON序列化器。
 *
 * DWS商品主题最终链路：
 *
 * GoodsSummary
 *      ↓
 * JSON
 *      ↓
 * Kafka
 *      ↓
 * dws_goods_summary
 */
public final class GoodsSummarySerializer {

    private GoodsSummarySerializer() {
        // 工具类禁止实例化
    }

    /**
     * GoodsSummary -> JSON String
     *
     * @param summary 商品主题一分钟汇总结果
     * @return JSON字符串；输入为空时返回null
     */
    public static String serialize(
            GoodsSummary summary) {

        if (summary == null) {
            return null;
        }

        try {

            return JSON.toJSONString(
                    summary
            );

        } catch (Exception e) {

            System.err.println(
                    "GoodsSummary序列化失败：" +
                            e.getMessage()
            );

            return null;
        }
    }
}